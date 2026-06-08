package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_sources")
data class ProxySourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val category: String,
    val isActive: Boolean = true,
    val lastCrawledAt: Long = 0L,
    val configsFound: Int = 0
)

@Entity(tableName = "v2ray_configs")
data class V2RayConfigEntity(
    @PrimaryKey val configHash: String, // String MD5 or hash
    val rawConfig: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val name: String,
    val pingMs: Long = -1L, // -1 means unchecked / unreachable / timeout
    val lastChecked: Long = 0L,
    val isFavorite: Boolean = false,
    val sourceName: String = "Unknown Source"
)
