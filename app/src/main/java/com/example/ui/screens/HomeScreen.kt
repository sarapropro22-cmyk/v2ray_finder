package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProxySourceEntity
import com.example.data.model.V2RayConfigEntity
import com.example.ui.viewmodel.V2RayViewModel

// Clean Minimalism Light Palette
val DarkBg = Color(0xFFFCF8FF)
val CardBg = Color(0xFFFFFFFF)
val BorderColor = Color(0xFFCAC4D0)
val AccentTeal = Color(0xFF6750A4)
val MetricGreen = Color(0xFF2E7D32)
val MetricOrange = Color(0xFFE65100)
val MetricRed = Color(0xFFC62828)
val MetricGray = Color(0xFF546E7A)

val LightPurpBg = Color(0xFFF3EDF7)
val StatusPurpBg = Color(0xFFEADDFF)
val StatusPurpText = Color(0xFF21005D)
val MiniTextPrimary = Color(0xFF1C1B1F)
val MiniTextSecondary = Color(0xFF49454F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: V2RayViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val configs by viewModel.configs.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val sources by viewModel.sources.collectAsState()

    val isCrawling by viewModel.isCrawling.collectAsState()
    val crawlProgressText by viewModel.crawlProgressText.collectAsState()

    val isPinging by viewModel.isPinging.collectAsState()
    val pingCurrent by viewModel.pingProgressCurrent.collectAsState()
    val pingTotal by viewModel.pingProgressTotal.collectAsState()

    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Filtering states
    var searchKeyword by remember { mutableStateOf("") }
    var selectedProtocolFilter by remember { mutableStateOf("All") }
    var onlyFavorites by remember { mutableStateOf(false) }

    val filteredConfigs = remember(configs, favorites, searchKeyword, selectedProtocolFilter, onlyFavorites) {
        val baseList = if (onlyFavorites) favorites else configs
        baseList.filter { config ->
            val matchesKeyword = config.name.contains(searchKeyword, ignoreCase = true) ||
                    config.host.contains(searchKeyword, ignoreCase = true) ||
                    config.protocol.contains(searchKeyword, ignoreCase = true)
            val matchesProtocol = selectedProtocolFilter == "All" || config.protocol.equals(selectedProtocolFilter, ignoreCase = true)
            matchesKeyword && matchesProtocol
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentTeal)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "V2Ray Pulse",
                            fontWeight = FontWeight.Bold,
                            color = MiniTextPrimary,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startCrawling() }, enabled = !isCrawling && !isPinging) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "بروزرسانی کل منابع",
                            tint = if (isCrawling) MetricGray else AccentTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = MiniTextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = LightPurpBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "کانفیگ‌ها") },
                    label = { Text("کانفیگ‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        unselectedIconColor = MiniTextSecondary,
                        unselectedTextColor = MiniTextSecondary,
                        indicatorColor = AccentTeal
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "منابع سرچ (${sources.size})") },
                    label = { Text("منابع سرچ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        unselectedIconColor = MiniTextSecondary,
                        unselectedTextColor = MiniTextSecondary,
                        indicatorColor = AccentTeal
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "آزمایشگاه هوش مصنوعی") },
                    label = { Text("آزمایشگاه هوش مصنوعی", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentTeal,
                        unselectedIconColor = MiniTextSecondary,
                        unselectedTextColor = MiniTextSecondary,
                        indicatorColor = AccentTeal
                    )
                )
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Crawl & Ping general active banners
            AnimatedVisibility(
                visible = isCrawling || isPinging,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusPurpBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isCrawling) "در حال جستجو و استخراج کانفیگ‌های جدید..." else "در حال پینگ و سنجش تاخیر اتصالات...",
                                color = StatusPurpText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AccentTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isCrawling) {
                            Text(
                                text = crawlProgressText,
                                fontSize = 12.sp,
                                color = StatusPurpText.copy(alpha = 0.8f)
                            )
                        } else if (isPinging) {
                            LinearProgressIndicator(
                                progress = { if (pingTotal > 0) pingCurrent.toFloat() / pingTotal else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AccentTeal,
                                trackColor = LightPurpBg,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تست موفق: $pingCurrent از $pingTotal سرور",
                                fontSize = 12.sp,
                                color = StatusPurpText
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> ConfigsTab(
                    configs = filteredConfigs,
                    totalConfigs = configs.size,
                    searchKeyword = searchKeyword,
                    onSearchKeywordChange = { searchKeyword = it },
                    selectedProtocolFilter = selectedProtocolFilter,
                    onProtocolFilterChange = { selectedProtocolFilter = it },
                    onlyFavorites = onlyFavorites,
                    onOnlyFavoritesChange = { onlyFavorites = it },
                    isCrawling = isCrawling,
                    isPinging = isPinging,
                    onStartCrawling = { viewModel.startCrawling() },
                    onStartPinging = { viewModel.startPinging() },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteConfig = { viewModel.deleteConfig(it) },
                    onClearConfigs = { viewModel.clearAllConfigs() },
                    onClearNonFavorites = { viewModel.clearNonFavorites() },
                    onAskGemini = { viewModel.askGeminiForOptimization(it); selectedTab = 2 },
                    context = context
                )
                1 -> SourcesTab(
                    sources = sources,
                    onToggleSource = { viewModel.toggleSourceActive(it) },
                    onAddSource = { name, url, cat -> viewModel.addCustomSource(name, url, cat) },
                    onDeleteSource = { viewModel.deleteSource(it) }
                )
                2 -> AiTab(
                    response = aiResponse,
                    isLoading = isAiLoading,
                    onFindSources = { viewModel.searchFreshProxiesWithGemini() },
                    onClear = { viewModel.clearAiOutput() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConfigsTab(
    configs: List<V2RayConfigEntity>,
    totalConfigs: Int,
    searchKeyword: String,
    onSearchKeywordChange: (String) -> Unit,
    selectedProtocolFilter: String,
    onProtocolFilterChange: (String) -> Unit,
    onlyFavorites: Boolean,
    onOnlyFavoritesChange: (Boolean) -> Unit,
    isCrawling: Boolean,
    isPinging: Boolean,
    onStartCrawling: () -> Unit,
    onStartPinging: () -> Unit,
    onToggleFavorite: (V2RayConfigEntity) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onClearConfigs: () -> Unit,
    onClearNonFavorites: () -> Unit,
    onAskGemini: (V2RayConfigEntity) -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Control buttons bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartCrawling,
                enabled = !isCrawling && !isPinging,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("شکار کانفیگ جدید", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onStartPinging,
                enabled = !isCrawling && !isPinging && totalConfigs > 0,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MetricGreen, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تست پینگ همگانی", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Summary Statistics Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = StatusPurpBg),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("کل به دست آمده", fontSize = 11.sp, color = StatusPurpText.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                    Text("$totalConfigs کانفیگ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StatusPurpText)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StatusPurpText.copy(alpha = 0.2f))
                )
                Column {
                    val workingCount = configs.count { it.pingMs in 1..2999 }
                    Text("فعال و سریع", fontSize = 11.sp, color = MetricGreen)
                    Text("$workingCount سرور", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MetricGreen)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StatusPurpText.copy(alpha = 0.2f))
                )
                Text(
                    text = "پاکسازی",
                    color = MetricRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            onClearNonFavorites()
                            Toast.makeText(context, "سرورهای غیر ستاره‌دار پاک شدند.", Toast.LENGTH_SHORT).show()
                        }
                        .padding(8.dp)
                )
            }
        }

        // Search Bar with Persian styling
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onSearchKeywordChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جستجو در بین آدرس‌ها یا نام سرور...", fontSize = 13.sp, color = MiniTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentTeal) },
            trailingIcon = if (searchKeyword.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchKeywordChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = MiniTextSecondary)
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MiniTextPrimary,
                unfocusedTextColor = MiniTextPrimary,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categories / Protocols Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val protocols = listOf("All", "VMess", "VLess", "Trojan", "Shadowsocks")
            protocols.forEach { proto ->
                val isSelected = selectedProtocolFilter == proto
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProtocolFilterChange(proto) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AccentTeal else LightPurpBg,
                    border = BorderStroke(1.dp, if (isSelected) AccentTeal else BorderColor)
                ) {
                    Text(
                        text = if (proto == "All") "همه" else proto,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (isSelected) Color.White else MiniTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = onlyFavorites,
                    onCheckedChange = onOnlyFavoritesChange,
                    colors = CheckboxDefaults.colors(checkedColor = AccentTeal)
                )
                Text("نمایش علاقه‌مندی‌ها (ستاره‌دار)", color = MiniTextPrimary, fontSize = 12.sp)
            }
            Text(
                text = "${configs.size} یافت شد",
                fontSize = 11.sp,
                color = MiniTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Configs Cards LazyList
        if (configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AccentTeal.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "هیچ کانفیگی یافت نشد.\n\nروی دکمه 'شکار کانفیگ جدید' در بالا بزنید تا بیش از ۵۰ منبع معتبر اینترنت سرچ شوند.",
                        color = MiniTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(configs, key = { it.configHash }) { config ->
                    V2RayConfigCard(
                        config = config,
                        onToggleFavorite = { onToggleFavorite(config) },
                        onDelete = { onDeleteConfig(config.configHash) },
                        onAskGemini = { onAskGemini(config) },
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun V2RayConfigCard(
    config: V2RayConfigEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onAskGemini: () -> Unit,
    context: Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Protocol Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (config.protocol.uppercase()) {
                                    "VLESS" -> Color(0xFFE3F2FD)
                                    "VMESS" -> Color(0xFFF3E5F5)
                                    "TROJAN" -> Color(0xFFFFEBEE)
                                    else -> Color(0xFFE8F5E9)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = config.protocol.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (config.protocol.uppercase()) {
                                "VLESS" -> Color(0xFF1E88E5)
                                "VMESS" -> Color(0xFF8E24AA)
                                "TROJAN" -> Color(0xFFD32F2F)
                                else -> Color(0xFF388E3C)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = config.name,
                        color = MiniTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite & Delete buttons
                Row {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (config.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "علاقه‌مندی",
                            tint = if (config.isFavorite) MetricRed else MiniTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MiniTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Server connection details & Ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${config.host}:${config.port}",
                        color = MiniTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "منبع: ${config.sourceName}",
                        color = MiniTextSecondary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                    )
                }

                // Ping delay badge
                PingDelayBadge(pingDelay = config.pingMs)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Copy config, Ask Gemini optimization)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("V2Ray Config", config.rawConfig)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "کده کانفیگ کپی شد!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightPurpBg, contentColor = AccentTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("کپی کد کانفیگ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAskGemini,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD8E4), contentColor = Color(0xFF31111D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("آنالیز هوش مصنوعی", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PingDelayBadge(pingDelay: Long) {
    val (color, bgColor, text) = when {
        pingDelay == 0L -> Triple(MetricGray, Color(0xFFECEFF1), "تست نشده")
        pingDelay < 0L -> Triple(MetricRed, Color(0xFFFFEBEE), "قطع / تایم‌اوت")
        pingDelay < 200L -> Triple(Color(0xFF2E7D32), Color(0xFFE8F5E9), "$pingDelay ms")
        pingDelay < 500L -> Triple(Color(0xFFE65100), Color(0xFFFFF3E0), "$pingDelay ms")
        else -> Triple(MetricRed, Color(0xFFFFEBEE), "$pingDelay ms")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SourcesTab(
    sources: List<ProxySourceEntity>,
    onToggleSource: (ProxySourceEntity) -> Unit,
    onAddSource: (String, String, String) -> Unit,
    onDeleteSource: (Int) -> Unit
) {
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var newSourceCat by remember { mutableStateOf("Custom") }
    var isAdding by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        // Source Counts Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LightPurpBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مجموع منابع جستجو", fontSize = 11.sp, color = MiniTextSecondary, fontWeight = FontWeight.Medium)
                        Text("${sources.size} وب‌سایت و مخزن", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MiniTextPrimary)
                    }
                    Button(
                        onClick = { isAdding = !isAdding },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isAdding) "بستن پنل" else "افزودن منبع سفارشی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expanded Panel to Add custom Crawler URLs
        if (isAdding) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, AccentTeal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("افزودن وب‌سایت / ساب‌اسکریپشن جدید", color = MiniTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newSourceName,
                            onValueChange = { newSourceName = it },
                            label = { Text("نام منبع (مثلاً ساب‌اسکریپشن ملی)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MiniTextPrimary,
                                unfocusedTextColor = MiniTextPrimary,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentTeal,
                                unfocusedLabelColor = MiniTextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newSourceUrl,
                            onValueChange = { newSourceUrl = it },
                            label = { Text("لینک کامل وب‌سایت یا لینک raw ساب‌اسکریپشن") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MiniTextPrimary,
                                unfocusedTextColor = MiniTextPrimary,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentTeal,
                                unfocusedLabelColor = MiniTextSecondary
                            ),
                            placeholder = { Text("https://example.com/sub") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newSourceCat,
                            onValueChange = { newSourceCat = it },
                            label = { Text("دسته‌بندی (مثلا وب‌سایت‌های آزاد)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MiniTextPrimary,
                                unfocusedTextColor = MiniTextPrimary,
                                focusedBorderColor = AccentTeal,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = AccentTeal,
                                unfocusedLabelColor = MiniTextSecondary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newSourceName.isNotBlank() && newSourceUrl.isNotBlank()) {
                                    onAddSource(newSourceName, newSourceUrl, newSourceCat)
                                    newSourceName = ""
                                    newSourceUrl = ""
                                    newSourceCat = "Custom"
                                    isAdding = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("منبع را ذخیره کن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Text(
                text = "لیست منابع موتور جستجو (۵۰ الی ۱۰۰ منبع اصلی)",
                color = MiniTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        // List of all active scraper seed URLs
        items(sources, key = { it.id }) { source ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, if (source.isActive) BorderColor else BorderColor.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (source.isActive) AccentTeal else MetricGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = source.name,
                                color = if (source.isActive) MiniTextPrimary else MiniTextSecondary.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = source.url,
                            color = MiniTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "یافت شده: ${source.configsFound}",
                                color = AccentTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "دسته: ${source.category}",
                                color = MiniTextSecondary.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Toggle Switch & Delete
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = source.isActive,
                            onCheckedChange = { onToggleSource(source) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentTeal,
                                uncheckedThumbColor = MiniTextSecondary,
                                uncheckedTrackColor = LightPurpBg
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { onDeleteSource(source.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف منبع", tint = MetricRed.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiTab(
    response: String,
    isLoading: Boolean,
    onFindSources: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LightPurpBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "دستیار تحقیق و آنالیز فیلترشکن هوش مصنوعیجمینی",
                    color = StatusPurpText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "با کمک هوش مصنوعی جمینی مستقیماً آدرس‌های VPN جدید آزاد و ترفندهای تنظیم کلودفلر را بیابید یا کانفیگ‌های سنگین را آنالیز و عیب‌یابی نمایید.",
                    color = MiniTextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onFindSources,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("جستجوی آدرس‌های نوظهور با هوش مصنوعی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (response.isNotEmpty()) {
                        Button(
                            onClick = onClear,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MiniTextSecondary),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("پاکسازی نتایج", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Response Output Space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("هوش مصنوعی جمینی در حال تفکر است...", color = AccentTeal, fontSize = 13.sp)
                    }
                }
            } else if (response.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "هیچ درخواستی ارسال نشده است.\nدر تب کانفیگ‌ها بر روی 'آنالیز هوش مصنوعی' کلیک کنید تا تحلیل کانفیگ را مشاهده نمایید، یا بر روی دکمه جستجو در بالا کلیک کنید تا ساب ساب اسکریپشن‌های تازه استخراج شوند.",
                        color = MiniTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("پاسخ هوش مصنوعی جمینی:", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Gemini Output", response)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "پاسخ هوش مصنوعی با موفقیت کپی شد!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightPurpBg, contentColor = AccentTeal),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("کپی پاسخ", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = response,
                            color = MiniTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Left // Since configuration files might be in LTR
                        )
                    }
                }
            }
        }
    }
}
