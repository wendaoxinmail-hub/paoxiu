package com.wendao.run.feature.run

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wendao.run.core.data.AuthRepository
import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.location.hasFineLocationPermission
import com.wendao.run.core.location.isLocationEnabled
import com.wendao.run.core.location.PaoxiuLocationLog
import com.wendao.run.core.location.readLastKnownLocation
import com.wendao.run.core.run.RunGeoUtils
import com.wendao.run.core.run.RunRepository
import com.wendao.run.core.run.RunSessionStore
import com.wendao.run.core.run.TrackEffectResolver
import com.wendao.run.core.run.TrackVisualEffect
import com.wendao.run.core.weather.WeatherRepository
import com.wendao.run.core.weather.WeatherSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RunLocationGate {
    /** 尚未检查或等待系统弹窗 */
    Pending,
    /** 已授权且 GPS 可用 */
    Ready,
    /** 需要弹出系统授权 */
    NeedPermission,
    /** 用户拒绝且可能需去设置 */
    Denied,
    /** 系统定位总开关关闭 */
    GpsOff,
}

@HiltViewModel
class RunViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    runSessionStore: RunSessionStore,
    private val runRepository: RunRepository,
    private val weatherRepository: WeatherRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val runType: String = savedStateHandle.get<String>("runType") ?: RunRecordEntity.RUN_TYPE_NORMAL
    val isSpiritRootTest: Boolean = runType == RunRecordEntity.RUN_TYPE_SPIRIT_ROOT
    val spiritRootTargetM: Double = 1000.0

    val activeRun = runSessionStore.activeRun
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trackPoints = runSessionStore.trackPoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trackEffect: StateFlow<TrackVisualEffect> = authRepository.profile
        .map { profile ->
            TrackEffectResolver.resolve(
                spiritRoot = profile?.spiritRoot,
                weaponId = profile?.equippedWeaponId,
                armorId = profile?.equippedArmorId,
                accessoryId = profile?.equippedAccessoryId,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackVisualEffect.Default)

    private val _finishedRunId = MutableStateFlow<String?>(null)
    val finishedRunId: StateFlow<String?> = _finishedRunId.asStateFlow()

    private val _isFinishing = MutableStateFlow(false)
    val isFinishing: StateFlow<Boolean> = _isFinishing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _locationGate = MutableStateFlow(RunLocationGate.Pending)
    val locationGate: StateFlow<RunLocationGate> = _locationGate.asStateFlow()

    private val _weather = MutableStateFlow<WeatherSnapshot?>(null)
    val weather: StateFlow<WeatherSnapshot?> = _weather.asStateFlow()

    private var autoFinishTriggered = false
    private var runStarted = false
    private var permissionPromptAttempted = false
    private var lastWeatherFetchMs = 0L
    private var lastWeatherLat = 0.0
    private var lastWeatherLng = 0.0

    init {
        viewModelScope.launch { authRepository.refreshProfile() }
        viewModelScope.launch {
            activeRun.collect { run ->
                if (
                    isSpiritRootTest &&
                    run != null &&
                    run.distanceM >= spiritRootTargetM &&
                    !autoFinishTriggered
                ) {
                    autoFinishTriggered = true
                    stopRun()
                }
                val lat = run?.displayLat ?: run?.lastLat
                val lng = run?.displayLng ?: run?.lastLng
                if (lat != null && lng != null) {
                    refreshWeatherIfNeeded(lat, lng)
                }
            }
        }
    }

    private fun refreshWeatherIfNeeded(lat: Double, lng: Double) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val movedM = if (lastWeatherFetchMs > 0L) {
                RunGeoUtils.haversineMeters(lastWeatherLat, lastWeatherLng, lat, lng)
            } else {
                Double.MAX_VALUE
            }
            if (_weather.value != null && now - lastWeatherFetchMs < 600_000 && movedM < 3_000) {
                return@launch
            }
            lastWeatherFetchMs = now
            lastWeatherLat = lat
            lastWeatherLng = lng
            _weather.value = weatherRepository.fetchCurrent(lat, lng)
        }
    }

    /** 进入修炼页 / 从设置返回时调用 */
    fun refreshLocationGate(onRequestPermission: () -> Unit) {
        val fine = context.hasFineLocationPermission()
        val gpsOn = context.isLocationEnabled()
        when {
            !fine -> {
                _locationGate.value = RunLocationGate.NeedPermission
                com.wendao.run.core.location.PaoxiuLocationLog.permission(
                    "NeedPermission",
                    fine,
                    gpsOn,
                )
                if (!permissionPromptAttempted) {
                    permissionPromptAttempted = true
                    onRequestPermission()
                }
            }
            !gpsOn -> {
                _locationGate.value = RunLocationGate.GpsOff
                com.wendao.run.core.location.PaoxiuLocationLog.permission("GpsOff", fine, gpsOn)
                _error.value = "请打开手机「位置信息 / GPS」开关"
            }
            else -> {
                _locationGate.value = RunLocationGate.Ready
                com.wendao.run.core.location.PaoxiuLocationLog.permission("Ready", fine, gpsOn)
                _error.value = null
                context.readLastKnownLocation()?.let { seed ->
                    refreshWeatherIfNeeded(seed.lat, seed.lng)
                }
                startRunIfNeeded()
            }
        }
    }

    fun requestLocationPermission(onLaunch: () -> Unit) {
        permissionPromptAttempted = true
        onLaunch()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            refreshLocationGate(onRequestPermission = {})
            requestNotificationPermissionIfNeeded()
        } else {
            _locationGate.value = RunLocationGate.Denied
            _error.value = "需要「精确位置」权限才能修炼，请允许或前往系统设置开启"
        }
    }

    fun onNotificationPermissionResult() {
        // 通知权限不影响跑步
    }

    fun startRunIfNeeded() {
        if (runStarted || activeRun.value != null) return
        if (!context.hasFineLocationPermission() || !context.isLocationEnabled()) return
        startRun()
    }

    fun startRun() {
        if (!context.hasFineLocationPermission()) {
            _locationGate.value = RunLocationGate.NeedPermission
            _error.value = "请先授予精确位置权限"
            return
        }
        if (!context.isLocationEnabled()) {
            _locationGate.value = RunLocationGate.GpsOff
            _error.value = "请打开手机「位置信息 / GPS」开关"
            return
        }
        _error.value = null
        _locationGate.value = RunLocationGate.Ready
        _finishedRunId.value = null
        autoFinishTriggered = false
        runStarted = true
        val intent = RunTrackingService.startIntent(context, runType)
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseRun() {
        context.startService(RunTrackingService.pauseIntent(context))
    }

    fun resumeRun() {
        context.startService(RunTrackingService.resumeIntent(context))
    }

    fun stopRun() {
        if (_isFinishing.value) return
        viewModelScope.launch {
            try {
                val snapshot = activeRun.value
                val runId = snapshot?.runId ?: runRepository.getActiveRun()?.id
                val durationSec = snapshot?.durationSec
                    ?: runId?.let { runRepository.getRun(it)?.durationSec }
                    ?: 0L
                if (runId == null) {
                    _error.value = "修炼记录保存失败，请重试"
                    return@launch
                }
                _isFinishing.value = true
                _error.value = null

                // 先在本地落库，不依赖 Service/MapView 生命周期
                val finished = runCatching {
                    runRepository.markRunFinished(runId, durationSec)
                }.getOrNull()

                context.startService(RunTrackingService.stopIntent(context))

                runStarted = false
                if (finished?.status == RunRecordEntity.STATUS_FINISHED) {
                    launch { runRepository.syncFinishedRun(runId) }
                    _finishedRunId.value = runId
                } else {
                    _error.value = "修炼记录保存失败，请重试"
                    _isFinishing.value = false
                }
            } catch (e: Exception) {
                _error.value = "收功失败，请重试"
                _isFinishing.value = false
                PaoxiuLocationLog.w("stopRun failed", e)
            }
        }
    }

    fun consumeFinishedRun() {
        _finishedRunId.value = null
    }

    fun clearError() {
        _error.value = null
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // 由 RunScreen 的第二个 launcher 处理；此处仅标记可请求
    }

    companion object {
        val locationPermissions: Array<String> = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
