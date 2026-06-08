package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.ProxySourceEntity
import com.example.data.model.V2RayConfigEntity
import com.example.data.repository.V2RayRepository
import com.example.data.api.GeminiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class V2RayViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "v2ray_pulse_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        V2RayRepository(
            context = application,
            proxySourceDao = db.proxySourceDao(),
            v2RayConfigDao = db.v2RayConfigDao()
        )
    }

    val configs: StateFlow<List<V2RayConfigEntity>> = repository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<V2RayConfigEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sources: StateFlow<List<ProxySourceEntity>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCrawling = MutableStateFlow(false)
    val isCrawling = _isCrawling.asStateFlow()

    private val _crawlProgressText = MutableStateFlow("")
    val crawlProgressText = _crawlProgressText.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging = _isPinging.asStateFlow()

    private val _pingProgressCurrent = MutableStateFlow(0)
    val pingProgressCurrent = _pingProgressCurrent.asStateFlow()

    private val _pingProgressTotal = MutableStateFlow(0)
    val pingProgressTotal = _pingProgressTotal.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    init {
        // Pre-populate sources on startup
        viewModelScope.launch {
            repository.checkAndSeedSources()
        }
    }

    fun startCrawling() {
        if (_isCrawling.value) return
        viewModelScope.launch {
            _isCrawling.value = true
            _crawlProgressText.value = "شروع جستجو در منابع..."
            try {
                repository.refreshAllConfigs { sourceName, found ->
                    _crawlProgressText.value = "منبع: $sourceName ($found کانفیگ یافت شد)"
                }
                _crawlProgressText.value = "جستجو به پایان رسید!"
            } catch (e: Exception) {
                _crawlProgressText.value = "خطا در جستجو: ${e.message}"
            } finally {
                _isCrawling.value = false
            }
        }
    }

    fun startPinging() {
        if (_isPinging.value) return
        viewModelScope.launch {
            _isPinging.value = true
            _pingProgressCurrent.value = 0
            _pingProgressTotal.value = 0
            try {
                repository.pingAllConfigs { current, total ->
                    _pingProgressCurrent.value = current
                    _pingProgressTotal.value = total
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                _isPinging.value = false
            }
        }
    }

    fun toggleFavorite(config: V2RayConfigEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(config)
        }
    }

    fun toggleSourceActive(source: ProxySourceEntity) {
        viewModelScope.launch {
            repository.updateSource(source.copy(isActive = !source.isActive))
        }
    }

    fun addCustomSource(name: String, url: String, category: String) {
        if (name.isBlank() || url.isBlank()) return
        viewModelScope.launch {
            repository.insertSource(
                ProxySourceEntity(
                    name = name.trim(),
                    url = url.trim(),
                    category = category.trim().ifEmpty { "Custom" }
                )
            )
        }
    }

    fun deleteSource(id: Int) {
        viewModelScope.launch {
            repository.deleteSource(id)
        }
    }

    fun deleteConfig(hash: String) {
        viewModelScope.launch {
            repository.deleteConfig(hash)
        }
    }

    fun clearAllConfigs() {
        viewModelScope.launch {
            repository.clearConfigs()
        }
    }

    fun clearNonFavorites() {
        viewModelScope.launch {
            repository.clearNonFavorites()
        }
    }

    fun askGeminiForOptimization(config: V2RayConfigEntity) {
        _isAiLoading.value = true
        _aiResponse.value = "در حال تحلیل کانفیگ توسط هوش مصنوعی..."
        viewModelScope.launch {
            val prompt = """
                من این کانفیگ V2Ray را دارم:
                نام:${config.name}
                پروتکل: ${config.protocol}
                میزبان: ${config.host}
                پورت: ${config.port}
                کد اصلی:
                ${config.rawConfig}
                
                لطفاً این کانفیگ را ارزیابی کن و بگو:
                ۱. آیا این پروتکل (${config.protocol}) امن است یا احتمال فیلتر شدنش در ایران زیاد است؟
                ۲. اگر فیلتر است، چه تغییراتی در فیلدهای TLS، SNI، Path، یا شبکه می‌توان داد تا دور زدن فیلترینگ MCI یا Irancell بهتر کار کند؟
                ۳. یک راهنمای خلاصه ارائه بده. پاسخ فارسی، صمیمی و بسیار کاربردی و بدون جزئیات طولانی فنی خسته‌کننده باشد. بسیار شیک بنویس.
            """.trimIndent()

            val systemInstruction = "شما یک متخصص ارشد و با تجربه شبکه و امنیت و دور زدن فیلترینگ اینترنت هستید."
            val response = GeminiHelper.generateResponse(prompt, systemInstruction)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun searchFreshProxiesWithGemini() {
        _isAiLoading.value = true
        _aiResponse.value = "در حال جستجو و کسب اطلاعات از جدیدترین منابع وی‌توری توسط هوش مصنوعی..."
        viewModelScope.launch {
            val prompt = """
                به عنوان یک متخصص فیلترشکن و v2ray، لطفاً:
                ۱. ۷ مورد از جدیدترین و فعال‌ترین آدرس‌های ساب‌اسکریپشن (Subscription Link) یا مخازن گیت‌هاب که به صورت روزانه و ساعتی لینک‌های رایگان vmess, vless, trojan منتشر می‌کنند را با آدرس کامل را معرفی کن.
                ۲. ترفندهای پیدا کردن سرورهای تمیز و بدون تاخیر (مثلا بررسی آی‌پی تمیز کلودفلر) را کوتاه توضیح بده.
                ۳. پاسخ به زبان فارسی روان، کوتاه، بسیار مرتب به صورت جدول یا لیست بالت‌دار و شیک باشد.
            """.trimIndent()

            val systemInstruction = "شما یک مشاور حرفه‌ای عبور از فیلترینگ و اشتراک‌گذاری کانفیگ هستید."
            val response = GeminiHelper.generateResponse(prompt, systemInstruction)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun clearAiOutput() {
        _aiResponse.value = ""
    }
}
