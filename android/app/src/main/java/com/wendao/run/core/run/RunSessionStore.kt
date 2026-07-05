package com.wendao.run.core.run

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RunSessionStatus {
    RUNNING,
    PAUSED,
}

data class RunTrackPoint(
    val lat: Double,
    val lng: Double,
)

data class ActiveRunState(
    val runId: String,
    val status: RunSessionStatus,
    val startedAt: Long,
    val durationSec: Long,
    val distanceM: Double,
    val currentSpeedKmh: Float,
    val pointCount: Int,
    val runType: String,
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    /** 地图蓝点与计距同源（系统 GPS/Fused → GCJ02） */
    val displayLat: Double? = null,
    val displayLng: Double? = null,
    val accuracyM: Float? = null,
    /** 跑步期间收到的定位回调次数 */
    val locationFixCount: Int = 0,
    val rollingPaceSecPerKm: Double? = null,
    val avgPaceSecPerKm: Double? = null,
    val movingDurationSec: Long = 0,
    /** 定位/地图鉴权提示（如百度 AK 未配置） */
    val locationHint: String? = null,
) {
    /** 主配速：总时长 / 总距离（Keep 平均配速） */
    val paceSecPerKm: Double? =
        RunGeoUtils.computePaceSecPerKm(distanceM, durationSec.coerceAtLeast(1))
}

@Singleton
class RunSessionStore @Inject constructor() {

    private val _activeRun = MutableStateFlow<ActiveRunState?>(null)
    val activeRun: StateFlow<ActiveRunState?> = _activeRun.asStateFlow()

    private val _trackPoints = MutableStateFlow<List<RunTrackPoint>>(emptyList())
    val trackPoints: StateFlow<List<RunTrackPoint>> = _trackPoints.asStateFlow()

    fun update(state: ActiveRunState?) {
        _activeRun.value = state
        if (state == null) {
            _trackPoints.value = emptyList()
        }
    }

    fun clearTrackPoints() {
        _trackPoints.value = emptyList()
    }

    fun appendTrackPoint(lat: Double, lng: Double) {
        val next = _trackPoints.value + RunTrackPoint(lat, lng)
        _trackPoints.value = if (next.size > 2000) next.takeLast(2000) else next
    }

    fun patch(transform: (ActiveRunState) -> ActiveRunState) {
        _activeRun.value?.let { current ->
            _activeRun.value = transform(current)
        }
    }
}
