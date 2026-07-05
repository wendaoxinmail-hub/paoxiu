package com.wendao.run.core.run

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunTrackingEngineTest {

    private lateinit var engine: RunTrackingEngine

    /** ~1m north per step */
    private val stepLat = 0.000009

    @Before
    fun setUp() {
        engine = RunTrackingEngine()
    }

    @Test
    fun ingest_walkingPath_accumulatesDistanceAndPace() {
        val baseLat = 39.9042
        val baseLng = 116.4074
        var t = 10_000L
        var total = 0.0

        engine.ingest(baseLat, baseLng, 0f, t)
        repeat(60) { i ->
            t += 1_000L
            val snap = engine.ingest(
                rawLat = baseLat + stepLat * (i + 1),
                rawLng = baseLng,
                accuracyM = 0f,
                recordedAt = t,
            )
            total = snap.distanceM
        }

        assertTrue("expected >50m got $total", total > 50.0)

        val pace = RunGeoUtils.computePaceSecPerKm(total, 60)
        assertNotNull(pace)
        assertTrue(pace!! in 120.0..1200.0)
    }

    @Test
    fun ingest_markBreak_doesNotCountGapSegment() {
        val lat = 39.9042
        val lng = 116.4074
        engine.ingest(lat, lng, 10f, 1_000L)
        engine.ingest(lat + stepLat * 10, lng, 10f, 11_000L)
        val before = engine.ingest(lat + stepLat * 20, lng, 10f, 21_000L).distanceM
        engine.markBreak()
        val after = engine.ingest(lat + stepLat * 200, lng + stepLat * 200, 10f, 120_000L)
        assertTrue(after.distanceM >= before)
        assertTrue(after.segmentM < 200.0)
    }

    @Test
    fun ingest_zeroAccuracyPoints_stillAccumulate() {
        engine.ingest(39.0, 116.0, 0f, 1_000L)
        val snap = engine.ingest(39.0 + stepLat * 10, 116.0, 0f, 12_000L)
        assertTrue(snap.accepted)
        assertTrue(snap.distanceM > 0.0)
    }

    @Test
    fun ingest_gpsGapOver8s_stillAccumulatesDistance() {
        engine.ingest(39.0, 116.0, 10f, 1_000L)
        val snap = engine.ingest(39.0 + stepLat * 10, 116.0, 10f, 12_000L)
        assertTrue(snap.accepted)
        assertTrue(snap.distanceM > 5.0)
    }
}
