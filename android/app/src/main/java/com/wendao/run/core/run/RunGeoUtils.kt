package com.wendao.run.core.run

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object RunGeoUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0

    private const val MAX_ACCURACY_M = 100f
    private const val FIRST_FIX_MAX_ACCURACY_M = 100f
    private const val MIN_SEGMENT_M = 0.15
    private const val STATIONARY_SPEED_KMH = 0.5f
    private const val STATIONARY_DRIFT_M = 1.5
    private const val MAX_IMPLIED_SPEED_KMH = 25.0

    enum class RejectReason {
        NONE,
        ACCURACY,
        ANCHOR,
        GPS_WAIT,
        DUPLICATE,
        DRIFT,
        SPIKE,
    }

    data class AcceptDecision(
        val accept: Boolean,
        val reason: RejectReason = RejectReason.NONE,
        val segmentM: Double = 0.0,
    )

    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun evaluatePoint(
        accuracyM: Float,
        lat: Double,
        lng: Double,
        lastLat: Double?,
        lastLng: Double?,
        speedKmh: Float,
        lastRecordedAt: Long?,
        recordedAt: Long,
        maxAccuracyM: Float = MAX_ACCURACY_M,
        isNetworkFix: Boolean = false,
    ): AcceptDecision {
        val accuracy = com.wendao.run.core.location.LocationAccuracyUtils.normalizeAccuracyM(accuracyM)
            .coerceAtMost(maxAccuracyM)
        if (accuracy > maxAccuracyM) {
            return AcceptDecision(false, RejectReason.ACCURACY)
        }
        if (lastLat == null || lastLng == null || lastRecordedAt == null) {
            val ok = accuracy <= FIRST_FIX_MAX_ACCURACY_M
            return AcceptDecision(ok, if (ok) RejectReason.ANCHOR else RejectReason.ACCURACY)
        }

        val segment = haversineMeters(lastLat, lastLng, lat, lng)
        val elapsedMs = (recordedAt - lastRecordedAt).coerceAtLeast(1L)
        val impliedSpeedKmh = segment / elapsedMs * 3_600.0

        if (impliedSpeedKmh > MAX_IMPLIED_SPEED_KMH) {
            return AcceptDecision(false, RejectReason.SPIKE, segment)
        }

        if (segment < MIN_SEGMENT_M) {
            return AcceptDecision(false, RejectReason.DUPLICATE, segment)
        }

        val moveSpeedKmh = max(speedKmh, impliedSpeedKmh.toFloat())
        if (moveSpeedKmh < STATIONARY_SPEED_KMH && segment < STATIONARY_DRIFT_M) {
            return AcceptDecision(false, RejectReason.DRIFT, segment)
        }

        // 户外 GPS：直接累加段距，不做 cap（cap 是距离偏少的根因）
        return AcceptDecision(true, RejectReason.NONE, segment)
    }

    /** 相邻轨迹点超过此距离则断开折线，避免 GPS 跳点「穿墙」 */
    const val TRACK_GAP_BREAK_M = 60.0

    fun splitTrackByGap(
        points: List<RunTrackPoint>,
        maxGapM: Double = TRACK_GAP_BREAK_M,
    ): List<List<RunTrackPoint>> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<MutableList<RunTrackPoint>>()
        var current = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val gap = haversineMeters(prev.lat, prev.lng, curr.lat, curr.lng)
            if (gap > maxGapM) {
                if (current.size >= 2) segments.add(current)
                current = mutableListOf(curr)
            } else {
                current.add(curr)
            }
        }
        if (current.size >= 2) segments.add(current)
        return segments
    }

    fun shouldAcceptPoint(
        accuracyM: Float,
        lat: Double,
        lng: Double,
        lastLat: Double?,
        lastLng: Double?,
        speedKmh: Float,
        lastRecordedAt: Long?,
        recordedAt: Long,
        maxAccuracyM: Float = MAX_ACCURACY_M,
    ): Boolean = evaluatePoint(
        accuracyM, lat, lng, lastLat, lastLng, speedKmh, lastRecordedAt, recordedAt, maxAccuracyM,
    ).accept

    /** 总时长平均配速；距离过短时不显示，避免 5m/69s 被格式化成 60'00" */
    fun computePaceSecPerKm(distanceM: Double, durationSec: Long): Double? {
        if (distanceM < 15 || durationSec <= 0) return null
        val pace = durationSec / (distanceM / 1000.0)
        if (!pace.isFinite() || pace > 7_200.0) return null
        return pace
    }

    fun paceFromSpeedKmh(speedKmh: Float): Double? {
        if (speedKmh <= 0.4f) return null
        return 3600.0 / speedKmh
    }

    fun deriveSpeedKmh(
        lat: Double,
        lng: Double,
        prevLat: Double,
        prevLng: Double,
        elapsedMs: Long,
    ): Float {
        if (elapsedMs <= 0) return 0f
        val segmentM = haversineMeters(prevLat, prevLng, lat, lng)
        return (segmentM / elapsedMs * 3_600.0).toFloat().coerceAtLeast(0f)
    }

    fun formatPace(paceSecPerKm: Double?): String {
        if (paceSecPerKm == null || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) {
            return "--'--\""
        }
        if (paceSecPerKm > 7_200.0) return "--'--\""
        val totalSec = paceSecPerKm.toInt().coerceIn(120, 7_200)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%d'%02d\"".format(minutes, seconds)
    }

    fun formatDuration(durationSec: Long): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        val seconds = durationSec % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatDistance(distanceM: Double): String {
        return if (distanceM >= 1000) {
            "%.2f km".format(distanceM / 1000.0)
        } else {
            "%.0f m".format(distanceM)
        }
    }
}
