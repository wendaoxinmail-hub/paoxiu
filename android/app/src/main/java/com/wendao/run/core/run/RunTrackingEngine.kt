package com.wendao.run.core.run

import kotlin.math.max

/**
 * Keep / Strava 式跑步测算：
 * - **距离/配速**：原始 GPS + 瞬移/漂移过滤
 * - **轨迹线**：EMA 平滑坐标（仅展示）
 */
class RunTrackingEngine {

    private var smoothLat: Double? = null
    private var smoothLng: Double? = null

    private var lastAcceptedRawLat: Double? = null
    private var lastAcceptedRawLng: Double? = null
    private var lastAcceptedAt: Long? = null

    private var lastRawLat: Double? = null
    private var lastRawLng: Double? = null
    private var lastRawAt: Long? = null

    private var pendingReanchor = false

    private var distanceM = 0.0
    private var movingDurationMs = 0L
    private var maxSpeedKmh = 0.0
    private val rollingSamples = ArrayDeque<RollingSample>()

    fun reset() {
        smoothLat = null
        smoothLng = null
        lastAcceptedRawLat = null
        lastAcceptedRawLng = null
        lastAcceptedAt = null
        lastRawLat = null
        lastRawLng = null
        lastRawAt = null
        pendingReanchor = false
        distanceM = 0.0
        movingDurationMs = 0L
        maxSpeedKmh = 0.0
        rollingSamples.clear()
    }

    /** 暂停/恢复：下一有效点重锚，不计暂停期间位移 */
    fun markBreak() {
        pendingReanchor = true
        lastRawLat = null
        lastRawLng = null
        lastRawAt = null
        smoothLat = null
        smoothLng = null
    }

    fun ingest(
        rawLat: Double,
        rawLng: Double,
        accuracyM: Float,
        recordedAt: Long,
        maxAccuracyM: Float = DEFAULT_MAX_ACCURACY_M,
        isNetworkFix: Boolean = false,
    ): TrackingSnapshot {
        val instantSpeedKmh = deriveRawSpeedKmh(rawLat, rawLng, recordedAt)
        val (trackLat, trackLng) = smoothCoordinate(rawLat, rawLng)

        if (pendingReanchor) {
            reanchorWithoutDistance(rawLat, rawLng, recordedAt)
            pendingReanchor = false
        }

        val segmentSpeedKmh = segmentSpeedKmh(rawLat, rawLng, recordedAt)
        val moveSpeedKmh = max(instantSpeedKmh, segmentSpeedKmh)
        val decision = RunGeoUtils.evaluatePoint(
            accuracyM = accuracyM,
            lat = rawLat,
            lng = rawLng,
            lastLat = lastAcceptedRawLat,
            lastLng = lastAcceptedRawLng,
            speedKmh = moveSpeedKmh,
            lastRecordedAt = lastAcceptedAt,
            recordedAt = recordedAt,
            maxAccuracyM = maxAccuracyM,
            isNetworkFix = isNetworkFix,
        )

        var segmentM = 0.0
        if (decision.accept) {
            segmentM = when (decision.reason) {
                RunGeoUtils.RejectReason.ANCHOR -> 0.0
                else -> decision.segmentM
            }
            if (segmentM > 0) {
                distanceM += segmentM
            }
            lastAcceptedAt?.let { prevAt ->
                val dtMs = (recordedAt - prevAt).coerceAtLeast(0)
                if (moveSpeedKmh >= MOVING_SPEED_KMH) {
                    movingDurationMs += dtMs
                }
            }
            maxSpeedKmh = max(maxSpeedKmh, moveSpeedKmh.toDouble())
            lastAcceptedRawLat = rawLat
            lastAcceptedRawLng = rawLng
            lastAcceptedAt = recordedAt
            if (segmentM > 0) {
                appendRollingSample(rawLat, rawLng, recordedAt, moveSpeedKmh)
            }
        }

        val movingSec = movingDurationMs / 1000
        return TrackingSnapshot(
            accepted = decision.accept,
            rejectReason = decision.reason,
            rawLat = rawLat,
            rawLng = rawLng,
            trackLat = trackLat,
            trackLng = trackLng,
            accuracyM = accuracyM,
            instantSpeedKmh = instantSpeedKmh,
            derivedSpeedKmh = segmentSpeedKmh,
            segmentM = segmentM,
            distanceM = distanceM,
            movingDurationSec = movingSec,
            rollingPaceSecPerKm = computeRollingPace(recordedAt),
            avgPaceSecPerKm = RunGeoUtils.computePaceSecPerKm(distanceM, movingSec.coerceAtLeast(1)),
            maxSpeedKmh = maxSpeedKmh,
        )
    }

    private fun reanchorWithoutDistance(rawLat: Double, rawLng: Double, recordedAt: Long) {
        lastAcceptedRawLat = rawLat
        lastAcceptedRawLng = rawLng
        lastAcceptedAt = recordedAt
        lastRawLat = rawLat
        lastRawLng = rawLng
        lastRawAt = recordedAt
        smoothLat = rawLat
        smoothLng = rawLng
    }

    private fun deriveRawSpeedKmh(rawLat: Double, rawLng: Double, recordedAt: Long): Float {
        val prevLat = lastRawLat ?: run {
            lastRawLat = rawLat
            lastRawLng = rawLng
            lastRawAt = recordedAt
            return 0f
        }
        val prevLng = lastRawLng ?: return 0f
        val prevAt = lastRawAt ?: return 0f
        lastRawLat = rawLat
        lastRawLng = rawLng
        lastRawAt = recordedAt
        val elapsedMs = recordedAt - prevAt
        if (elapsedMs <= 0) return 0f
        return RunGeoUtils.deriveSpeedKmh(rawLat, rawLng, prevLat, prevLng, elapsedMs)
    }

    private fun segmentSpeedKmh(rawLat: Double, rawLng: Double, recordedAt: Long): Float {
        val prevLat = lastAcceptedRawLat ?: return 0f
        val prevLng = lastAcceptedRawLng ?: return 0f
        val prevAt = lastAcceptedAt ?: return 0f
        val elapsedMs = recordedAt - prevAt
        if (elapsedMs <= 0) return 0f
        return RunGeoUtils.deriveSpeedKmh(rawLat, rawLng, prevLat, prevLng, elapsedMs)
    }

    private fun smoothCoordinate(rawLat: Double, rawLng: Double): Pair<Double, Double> {
        val prevLat = smoothLat
        val prevLng = smoothLng
        if (prevLat == null || prevLng == null) {
            smoothLat = rawLat
            smoothLng = rawLng
            return rawLat to rawLng
        }
        val lat = prevLat * (1 - SMOOTH_ALPHA) + rawLat * SMOOTH_ALPHA
        val lng = prevLng * (1 - SMOOTH_ALPHA) + rawLng * SMOOTH_ALPHA
        smoothLat = lat
        smoothLng = lng
        return lat to lng
    }

    private fun appendRollingSample(lat: Double, lng: Double, recordedAt: Long, speedKmh: Float) {
        rollingSamples.addLast(RollingSample(recordedAt, lat, lng, speedKmh))
        trimRollingWindow(recordedAt)
    }

    private fun trimRollingWindow(now: Long) {
        val cutoff = now - ROLLING_WINDOW_MS
        while (rollingSamples.isNotEmpty() && rollingSamples.first().recordedAt < cutoff) {
            rollingSamples.removeFirst()
        }
    }

    private fun computeRollingPace(now: Long): Double? {
        trimRollingWindow(now)
        if (rollingSamples.size < 2) return null
        var distM = 0.0
        for (i in 1 until rollingSamples.size) {
            val prev = rollingSamples[i - 1]
            val curr = rollingSamples[i]
            if (max(prev.speedKmh, curr.speedKmh) < MOVING_SPEED_KMH) continue
            distM += RunGeoUtils.haversineMeters(prev.lat, prev.lng, curr.lat, curr.lng)
        }
        val dtSec = (rollingSamples.last().recordedAt - rollingSamples.first().recordedAt) / 1000.0
        if (distM < ROLLING_MIN_DIST_M || dtSec < ROLLING_MIN_TIME_SEC) return null
        val pace = dtSec / distM * 1_000.0
        return pace.takeIf { it.isFinite() && it in PACE_MIN_SEC..PACE_MAX_SEC }
    }

    private data class RollingSample(
        val recordedAt: Long,
        val lat: Double,
        val lng: Double,
        val speedKmh: Float,
    )

    companion object {
        const val MOVING_SPEED_KMH = 0.6f
        private const val SMOOTH_ALPHA = 0.28
        private const val ROLLING_WINDOW_MS = 30_000L
        private const val ROLLING_MIN_DIST_M = 3.0
        private const val ROLLING_MIN_TIME_SEC = 2.0
        private const val PACE_MIN_SEC = 120.0
        private const val PACE_MAX_SEC = 1200.0
        private const val DEFAULT_MAX_ACCURACY_M = 100f
    }
}

data class TrackingSnapshot(
    val accepted: Boolean,
    val rejectReason: RunGeoUtils.RejectReason = RunGeoUtils.RejectReason.NONE,
    val rawLat: Double,
    val rawLng: Double,
    val trackLat: Double,
    val trackLng: Double,
    val accuracyM: Float,
    val instantSpeedKmh: Float,
    val derivedSpeedKmh: Float,
    val segmentM: Double,
    val distanceM: Double,
    val movingDurationSec: Long,
    val rollingPaceSecPerKm: Double?,
    val avgPaceSecPerKm: Double?,
    val maxSpeedKmh: Double,
)
