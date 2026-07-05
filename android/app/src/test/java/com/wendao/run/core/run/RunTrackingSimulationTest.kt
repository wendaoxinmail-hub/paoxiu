package com.wendao.run.core.run

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模拟真机 GPS 轨迹：1Hz、约 1m/步、accuracy=0、含 cap 滤噪。
 * 与 [RunTrackingEngineTest] 的理想步进不同，这里故意加入 Android 常见输入特征。
 */
class RunTrackingSimulationTest {

    private val stepLat = 0.000009

    @Test
    fun simulatedOutdoorWalk_accumulatesDistance() {
        val engine = RunTrackingEngine()
        var t = 1_000_000L
        var total = 0.0
        engine.ingest(39.9042, 116.4074, 0f, t)
        repeat(100) { i ->
            t += 1_000L
            val snap = engine.ingest(
                rawLat = 39.9042 + stepLat * (i + 1),
                rawLng = 116.4074,
                accuracyM = 0f,
                recordedAt = t,
            )
            total = snap.distanceM
        }
        assertTrue("simulated 100m walk expected >70m got $total", total > 70.0)
        assertTrue("simulated walk expected <130m got $total", total < 130.0)
    }

    @Test
    fun simulatedWalk_paceInReasonableRange() {
        val engine = RunTrackingEngine()
        var t = 0L
        engine.ingest(39.0, 116.0, 0f, t)
        repeat(60) { i ->
            t += 1_000L
            engine.ingest(39.0 + stepLat * (i + 1), 116.0, 0f, t)
        }
        val snap = engine.ingest(39.0 + stepLat * 61, 116.0, 0f, t + 1_000L)
        val pace = RunGeoUtils.computePaceSecPerKm(snap.distanceM, t / 1000)!!
        assertTrue("pace=$pace dist=${snap.distanceM}", pace in 200.0..1500.0)
    }
}
