package com.wendao.run.core.run

import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.database.entity.TrackPointEntity
import kotlin.math.roundToInt

/**
 * Keep 运动数据九格指标（无传感器时用配速/轨迹推演，缺数据显示 —）。
 */
data class KeepRunStats(
    val movingDurationSec: Long,
    val caloriesKcal: Int?,
    val avgHeartRateBpm: Int?,
    val avgCadenceSpm: Int?,
    val avgStrideCm: Int?,
    val elevationGainM: Int?,
    val elevationLossM: Int?,
)

object RunKeepMetrics {

    private const val DEFAULT_WEIGHT_KG = 70.0
    private const val MOVING_SPEED_KMH = RunTrackingEngine.MOVING_SPEED_KMH

    fun compute(
        run: RunRecordEntity,
        points: List<TrackPointEntity>,
    ): KeepRunStats {
        val movingSec = movingDurationSec(points, run.durationSec)
        val calories = estimateCalories(run.distanceM, movingSec)
        val cadence = estimateCadence(run.avgPaceSecPerKm, run.maxSpeedKmh)
        val stride = estimateStrideCm(run.maxSpeedKmh, cadence)
        return KeepRunStats(
            movingDurationSec = movingSec,
            caloriesKcal = calories,
            avgHeartRateBpm = null,
            avgCadenceSpm = cadence,
            avgStrideCm = stride,
            elevationGainM = null,
            elevationLossM = null,
        )
    }

    fun formatCalories(kcal: Int?): String = kcal?.let { "$it" } ?: "—"

    fun formatHeartRate(bpm: Int?): String = bpm?.let { "$it" } ?: "—"

    fun formatCadence(spm: Int?): String = spm?.let { "$it" } ?: "—"

    fun formatStride(cm: Int?): String = cm?.let { "$it cm" } ?: "—"

    fun formatElevation(m: Int?): String = m?.let { "$it m" } ?: "—"

    /** Keep：移动时间 = 相邻点推算速度 ≥ 1.2 km/h 的累计时长 */
    fun movingDurationSec(points: List<TrackPointEntity>, fallbackSec: Long): Long {
        if (points.size < 2) return fallbackSec
        var movingMs = 0L
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val dt = (curr.recordedAt - prev.recordedAt).coerceAtLeast(0)
            val speed = RunGeoUtils.deriveSpeedKmh(
                lat = curr.lat,
                lng = curr.lng,
                prevLat = prev.lat,
                prevLng = prev.lng,
                elapsedMs = dt,
            )
            if (speed >= MOVING_SPEED_KMH) {
                movingMs += dt
            }
        }
        return if (movingMs > 0) movingMs / 1000 else fallbackSec
    }

    /** 千卡：距离法 + 时长法取较大值，短距离试跑也有合理消耗 */
    private fun estimateCalories(distanceM: Double, durationSec: Long): Int? {
        if (durationSec <= 0 && distanceM < 10) return null
        val byDistance = if (distanceM >= 10) {
            (distanceM / 1000.0) * DEFAULT_WEIGHT_KG * 1.036
        } else {
            0.0
        }
        val byDuration = if (durationSec > 0) {
            (durationSec / 3600.0) * 8.0 * DEFAULT_WEIGHT_KG
        } else {
            0.0
        }
        return maxOf(byDistance, byDuration).roundToInt().coerceAtLeast(0).takeIf { it > 0 }
    }

    /** 由配速估算步频（步/分） */
    private fun estimateCadence(paceSecPerKm: Double?, maxSpeedKmh: Double): Int? {
        paceSecPerKm?.takeIf { it > 0 }?.let { pace ->
            val paceMin = pace / 60.0
            return (175 - (paceMin - 6.0) * 8.0).roundToInt().coerceIn(140, 210)
        }
        if (maxSpeedKmh > MOVING_SPEED_KMH) {
            return (160 + maxSpeedKmh * 2.5).roundToInt().coerceIn(140, 210)
        }
        return null
    }

    /** 由速度与步频估算步幅（厘米） */
    private fun estimateStrideCm(maxSpeedKmh: Double, cadenceSpm: Int?): Int? {
        if (maxSpeedKmh <= MOVING_SPEED_KMH || cadenceSpm == null || cadenceSpm <= 0) return null
        val speedMs = maxSpeedKmh / 3.6
        val strideM = speedMs / (cadenceSpm / 60.0)
        return (strideM * 100.0).roundToInt().coerceIn(40, 150)
    }
}
