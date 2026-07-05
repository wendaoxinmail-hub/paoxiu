package com.wendao.run.core.run

import com.wendao.run.core.database.entity.RunRecordEntity
import com.wendao.run.core.database.entity.TrackPointEntity
import kotlin.math.roundToInt

data class KmSplit(
    val kmIndex: Int,
    val durationSec: Long,
    val paceSecPerKm: Double?,
    val avgQiIndex: Int,
)

data class PaceSample(
    /** 相对开始时间的分钟数 */
    val minute: Float,
    val paceSecPerKm: Double,
)

data class PaceZoneSlice(
    val label: String,
    val ratio: Float,
    val colorArgb: Int,
)

data class RunTrendComparison(
    val paceDeltaSec: Double?,
    val distanceDeltaM: Double?,
    val paceTrendLabel: String?,
    val distanceTrendLabel: String?,
)

data class RunAnalytics(
    val splits: List<KmSplit>,
    val paceSeries: List<PaceSample>,
    val paceZones: List<PaceZoneSlice>,
    val avgQiIndex: Int,
    val trend: RunTrendComparison,
    val trainingAerobic: Float,
    val trainingSpirit: Float,
)

object RunAnalyticsEngine {

    fun analyze(
        run: RunRecordEntity,
        points: List<TrackPointEntity>,
        previousRun: RunRecordEntity?,
    ): RunAnalytics {
        val splits = computeKmSplits(points)
        val paceSeries = computePaceSeries(points)
        val paceZones = computePaceZones(points)
        val avgQi = averageQiIndex(run, points)
        val trend = compareTrend(run, previousRun)
        val aerobic = ((run.durationSec / 3600.0) * (run.distanceM / 1000.0) / 10.0)
            .toFloat()
            .coerceIn(0.2f, 5f)
        val spirit = ((run.rewardXp ?: 0L) / 100.0).toFloat().coerceIn(0.2f, 5f)
        return RunAnalytics(
            splits = splits,
            paceSeries = paceSeries,
            paceZones = paceZones,
            avgQiIndex = avgQi,
            trend = trend,
            trainingAerobic = aerobic,
            trainingSpirit = spirit,
        )
    }

    fun computeKmSplits(points: List<TrackPointEntity>): List<KmSplit> {
        if (points.size < 2) return emptyList()
        val splits = mutableListOf<KmSplit>()
        var kmIndex = 1
        var kmStartTime = points.first().recordedAt
        var kmStartIdx = 0
        var accumulated = 0.0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            accumulated += RunGeoUtils.haversineMeters(prev.lat, prev.lng, curr.lat, curr.lng)
            while (accumulated >= 1000.0) {
                val segPoints = points.subList(kmStartIdx, i + 1)
                val durationSec = ((curr.recordedAt - kmStartTime) / 1000).coerceAtLeast(1)
                val pace = RunGeoUtils.computePaceSecPerKm(1000.0, durationSec)
                val qi = segPoints.map { QiIndexUtils.estimate(null, it.speedKmh) }
                    .filter { it > 0 }
                    .average()
                    .roundToInt()
                splits.add(
                    KmSplit(
                        kmIndex = kmIndex,
                        durationSec = durationSec,
                        paceSecPerKm = pace,
                        avgQiIndex = qi,
                    ),
                )
                kmIndex++
                accumulated -= 1000.0
                kmStartTime = curr.recordedAt
                kmStartIdx = i
            }
        }
        return splits
    }

    fun computePaceSeries(points: List<TrackPointEntity>): List<PaceSample> {
        if (points.size < 2) return emptyList()
        val start = points.first().recordedAt
        val samples = mutableListOf<PaceSample>()
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val dtSec = ((curr.recordedAt - prev.recordedAt) / 1000.0).coerceAtLeast(1.0)
            val dist = RunGeoUtils.haversineMeters(prev.lat, prev.lng, curr.lat, curr.lng)
            if (dist < 3) continue
            val pace = (dtSec / dist) * 1000.0
            if (pace.isFinite() && pace in 120.0..1200.0) {
                val minute = ((curr.recordedAt - start) / 60000f)
                samples.add(PaceSample(minute = minute, paceSecPerKm = pace))
            }
        }
        return if (samples.size > 80) {
            samples.filterIndexed { index, _ -> index % (samples.size / 80) == 0 }
        } else {
            samples
        }
    }

    fun computePaceZones(points: List<TrackPointEntity>): List<PaceZoneSlice> {
        if (points.size < 2) {
            return listOf(
                PaceZoneSlice("热身", 1f, 0xFF8A94A8.toInt()),
            )
        }
        var warm = 0
        var fat = 0
        var aerobic = 0
        var fast = 0
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val dtSec = ((curr.recordedAt - prev.recordedAt) / 1000.0).coerceAtLeast(1.0)
            val dist = RunGeoUtils.haversineMeters(prev.lat, prev.lng, curr.lat, curr.lng)
            if (dist < 3) continue
            val pace = (dtSec / dist) * 1000.0
            when {
                pace >= 420 -> warm++
                pace >= 330 -> fat++
                pace >= 270 -> aerobic++
                else -> fast++
            }
        }
        val total = (warm + fat + aerobic + fast).coerceAtLeast(1)
        return listOf(
            PaceZoneSlice("热身", warm / total.toFloat(), 0xFF8E9399.toInt()),
            PaceZoneSlice("稳态", fat / total.toFloat(), 0xFF24C789.toInt()),
            PaceZoneSlice("修行", aerobic / total.toFloat(), 0xFFC9A227.toInt()),
            PaceZoneSlice("冲刺", fast / total.toFloat(), 0xFFE05555.toInt()),
        ).filter { it.ratio > 0.01f }
    }

    private fun averageQiIndex(run: RunRecordEntity, points: List<TrackPointEntity>): Int {
        val fromPoints = points.map { QiIndexUtils.estimate(null, it.speedKmh) }.filter { it > 0 }
        if (fromPoints.isNotEmpty()) return fromPoints.average().roundToInt()
        return QiIndexUtils.estimate(run.avgPaceSecPerKm, run.maxSpeedKmh.toFloat())
    }

    private fun compareTrend(current: RunRecordEntity, previous: RunRecordEntity?): RunTrendComparison {
        if (previous == null) {
            return RunTrendComparison(null, null, null, null)
        }
        val paceDelta = when {
            current.avgPaceSecPerKm != null && previous.avgPaceSecPerKm != null ->
                current.avgPaceSecPerKm - previous.avgPaceSecPerKm
            else -> null
        }
        val distDelta = current.distanceM - previous.distanceM
        val paceLabel = paceDelta?.let { delta ->
            if (kotlin.math.abs(delta) < 5) "配速 · 与上次持平"
            else if (delta < 0) "配速 · 较上次快 ${formatPaceDelta(-delta)}"
            else "配速 · 较上次慢 ${formatPaceDelta(delta)}"
        }
        val distLabel = when {
            kotlin.math.abs(distDelta) < 50 -> "里程 · 与上次接近"
            distDelta > 0 -> "里程 · 较上次多 ${RunGeoUtils.formatDistance(distDelta)}"
            else -> "里程 · 较上次少 ${RunGeoUtils.formatDistance(-distDelta)}"
        }
        return RunTrendComparison(paceDelta, distDelta, paceLabel, distLabel)
    }

    private fun formatPaceDelta(sec: Double): String {
        val s = sec.roundToInt()
        return "%d'%02d\"".format(s / 60, s % 60)
    }
}

fun formatSplitDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
