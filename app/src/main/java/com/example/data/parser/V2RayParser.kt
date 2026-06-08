package com.example.data.parser

import android.util.Base64
import android.util.Log
import com.example.data.model.V2RayConfigEntity
import org.json.JSONObject
import java.net.URLDecoder
import java.security.MessageDigest

object V2RayParser {
    private const val TAG = "V2RayParser"

    fun String.md5(): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(this.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            this.hashCode().toString()
        }
    }

    /**
     * Extracts all V2Ray configurations from a webpage content or a subscription response.
     */
    fun parseConfigs(rawContent: String, sourceName: String): List<V2RayConfigEntity> {
        if (rawContent.isBlank()) return emptyList()

        val trimmed = rawContent.trim()
        val decodedList = ArrayList<String>()

        // 1. Try decoding as full subscription (often a single Base64 block)
        try {
            val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8)
            if (decodedStr.contains("://")) {
                decodedList.addAll(decodedStr.split("\n", "\r").map { it.trim() }.filter { it.isNotBlank() })
            }
        } catch (e: Exception) {
            // Not a direct base64 sub, we will process the raw text
        }

        // If not a single base64 chunk or empty, add raw lines as well (or the raw text itself)
        if (decodedList.isEmpty()) {
            decodedList.addAll(rawContent.split("\n", "\r").map { it.trim() }.filter { it.isNotBlank() })
        }

        val results = mutableListOf<V2RayConfigEntity>()

        // Find all v2ray links in the lines (could be inline in HTML)
        val regex = "(vmess|vless|ss|trojan)://[^\\s\"'<>]+".toRegex()

        for (item in decodedList) {
            val matches = regex.findAll(item)
            for (match in matches) {
                val fullUrl = match.value
                val parsed = parseSingleConfig(fullUrl, sourceName)
                if (parsed != null) {
                    results.add(parsed)
                }
            }
        }

        return results
    }

    /**
     * Parses a single v2ray config URL.
     */
    fun parseSingleConfig(url: String, sourceName: String): V2RayConfigEntity? {
        val cleanUrl = url.trim()
        val hash = cleanUrl.md5()

        return try {
            if (cleanUrl.startsWith("vmess://")) {
                parseVMess(cleanUrl, hash, sourceName)
            } else if (cleanUrl.startsWith("vless://") || cleanUrl.startsWith("trojan://") || cleanUrl.startsWith("ss://")) {
                parseGeneric(cleanUrl, hash, sourceName)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config: $url", e)
            null
        }
    }

    private fun parseVMess(url: String, hash: String, sourceName: String): V2RayConfigEntity? {
        val base64Part = url.substringAfter("vmess://")
        val decodedStr = try {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // Try URL safe dec
            try {
                val bytes = Base64.decode(base64Part, Base64.URL_SAFE)
                String(bytes, Charsets.UTF_8)
            } catch (e2: Exception) {
                return null
            }
        }

        return try {
            val json = JSONObject(decodedStr)
            val host = json.optString("add", "").trim()
            val port = when {
                json.has("port") -> {
                    val p = json.get("port")
                    if (p is Number) p.toInt() else p.toString().toIntOrNull() ?: 443
                }
                else -> 443
            }
            val name = json.optString("ps", "VMess Server").trim().let {
                try { URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
            }

            if (host.isEmpty()) return null

            V2RayConfigEntity(
                configHash = hash,
                rawConfig = url,
                protocol = "VMess",
                host = host,
                port = port,
                name = name.ifEmpty { "VMess $host" },
                sourceName = sourceName
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSONObject parse failed for VMess string: $decodedStr", e)
            null
        }
    }

    private fun parseGeneric(url: String, hash: String, sourceName: String): V2RayConfigEntity? {
        val protocol = when {
            url.startsWith("vless://") -> "VLess"
            url.startsWith("trojan://") -> "Trojan"
            url.startsWith("ss://") -> "Shadowsocks"
            else -> "Unknown"
        }

        val noProto = url.substringAfter("://")
        
        // Extract Name/Remark (after '#')
        val remarkPart = noProto.substringAfter("#", "")
        val rawName = if (remarkPart.isNotEmpty()) {
            try {
                URLDecoder.decode(remarkPart, "UTF-8")
            } catch (e: Exception) {
                remarkPart
            }
        } else {
            ""
        }

        // Clean part before '#' and check '@'
        val corePart = noProto.substringBefore("#")
        val credentialsAndHostPort = corePart.substringBefore("?") // strip params like ?type=ws

        var host = ""
        var port = 443

        if (credentialsAndHostPort.contains("@")) {
            val hostPortPart = credentialsAndHostPort.substringAfterLast("@")
            if (hostPortPart.contains(":")) {
                host = hostPortPart.substringBeforeLast(":").trim()
                port = hostPortPart.substringAfterLast(":").toIntOrNull() ?: 443
            } else {
                host = hostPortPart.trim()
            }
        } else {
            // For older shadowsocks format ss://[base64_string] without @, let's try to base64 decode it
            if (protocol == "Shadowsocks") {
                try {
                    val decodedBytes = Base64.decode(credentialsAndHostPort, Base64.DEFAULT)
                    val decodedStr = String(decodedBytes, Charsets.UTF_8)
                    if (decodedStr.contains("@")) {
                        val hp = decodedStr.substringAfterLast("@")
                        if (hp.contains(":")) {
                            host = hp.substringBeforeLast(":").trim()
                            port = hp.substringAfterLast(":").toIntOrNull() ?: 443
                        } else {
                            host = hp.trim()
                        }
                    } else if (decodedStr.contains(":")) {
                        // method:password@host:port, or maybe plain host:port?
                        // Let's see if we have credentials
                        val parts = decodedStr.split(":")
                        if (parts.size >= 2) {
                            // Let's assume the last part is port and second last is host
                            port = parts.last().toIntOrNull() ?: 443
                            host = parts[parts.size - 2].substringAfterLast("@").trim()
                        }
                    }
                } catch (e: Exception) {
                    // ignore older parsing errors, continue
                }
            }
        }

        if (host.isEmpty()) {
            // fallback: check if we can just find any host:port
            val parts = credentialsAndHostPort.split(":")
            if (parts.size >= 2) {
                port = parts.last().toIntOrNull() ?: 443
                host = parts[parts.size - 2].substringAfterLast("@").trim()
            }
        }

        if (host.isEmpty()) return null

        val displayName = rawName.trim().ifEmpty { "$protocol $host" }

        return V2RayConfigEntity(
            configHash = hash,
            rawConfig = url,
            protocol = protocol,
            host = host,
            port = port,
            name = displayName,
            sourceName = sourceName
        )
    }
}
