package com.wendao.run.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 配速必须与「总时长 / 总距离」一致（Keep 平均配速）。
 *
 * 注意：JVM 单测不覆盖 Android 定位 SDK、坐标系转换、前台服务生命周期；
 * 那些问题需真机 / instrumented test 验证。
 */
class PaceConsistencyTest {

    @Test
    fun pace_formula_matchesDurationAndDistance() {
        val distanceM = 823.0
        val durationSec = 312L
        val pace = RunGeoUtils.computePaceSecPerKm(distanceM, durationSec)
        assertNotNull(pace)
        assertEquals(durationSec.toDouble(), pace!! * distanceM / 1000.0, 0.01)
    }

    @Test
    fun engine_distance_and_wallPace_areConsistent() {
        val engine = RunTrackingEngine()
        val stepLat = 0.000009
        var t = 0L
        engine.ingest(39.9042, 116.4074, 10f, t)
        repeat(30) { i ->
            t += 1_000L
            engine.ingest(39.9042 + stepLat * (i + 1), 116.4074, 10f, t)
        }
        val durationSec = t / 1000
        val snap = engine.ingest(39.9042 + stepLat * 31, 116.4074, 10f, t + 1_000L)
        val pace = RunGeoUtils.computePaceSecPerKm(snap.distanceM, durationSec)
        assertNotNull(pace)
        assertEquals(durationSec.toDouble(), pace!! * snap.distanceM / 1000.0, 1.0)
    }
}
