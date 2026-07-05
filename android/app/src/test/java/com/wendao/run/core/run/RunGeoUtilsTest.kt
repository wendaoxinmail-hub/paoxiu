package com.wendao.run.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunGeoUtilsTest {

    /** ~1m north per 0.000009° latitude */
    private val oneMeterLat = 0.000009

    @Test
    fun haversine_knownSegment_returnsMeters() {
        val m = RunGeoUtils.haversineMeters(39.0, 116.0, 39.001, 116.0)
        assertTrue(m in 100.0..125.0)
    }

    @Test
    fun evaluatePoint_zeroAccuracy_stillAcceptsAnchor() {
        val decision = RunGeoUtils.evaluatePoint(
            accuracyM = 0f,
            lat = 39.9042,
            lng = 116.4074,
            lastLat = null,
            lastLng = null,
            speedKmh = 0f,
            lastRecordedAt = null,
            recordedAt = 1_000L,
        )
        assertTrue(decision.accept)
        assertEquals(RunGeoUtils.RejectReason.ANCHOR, decision.reason)
    }

    @Test
    fun evaluatePoint_walkingSegment_accumulatesDistance() {
        val t0 = 1_000_000L
        val anchor = RunGeoUtils.evaluatePoint(
            accuracyM = 0f,
            lat = 39.9042,
            lng = 116.4074,
            lastLat = null,
            lastLng = null,
            speedKmh = 0f,
            lastRecordedAt = null,
            recordedAt = t0,
        )
        assertTrue(anchor.accept)

        val step = RunGeoUtils.evaluatePoint(
            accuracyM = 10f,
            lat = 39.9042 + oneMeterLat * 10,
            lng = 116.4074,
            lastLat = 39.9042,
            lastLng = 116.4074,
            speedKmh = 4f,
            lastRecordedAt = t0,
            recordedAt = t0 + 10_000L,
        )
        assertTrue(step.accept)
        assertTrue(step.segmentM >= 5.0)
    }

    @Test
    fun evaluatePoint_stationaryDrift_rejected() {
        val t0 = 2_000_000L
        RunGeoUtils.evaluatePoint(
            accuracyM = 10f,
            lat = 39.9042,
            lng = 116.4074,
            lastLat = null,
            lastLng = null,
            speedKmh = 0f,
            lastRecordedAt = null,
            recordedAt = t0,
        )
        val step = RunGeoUtils.evaluatePoint(
            accuracyM = 10f,
            lat = 39.9042 + oneMeterLat,
            lng = 116.4074,
            lastLat = 39.9042,
            lastLng = 116.4074,
            speedKmh = 0.2f,
            lastRecordedAt = t0,
            recordedAt = t0 + 10_000L,
        )
        assertFalse(step.accept)
        assertEquals(RunGeoUtils.RejectReason.DRIFT, step.reason)
    }

    @Test
    fun computePaceSecPerKm_fromDistanceAndDuration() {
        val pace = RunGeoUtils.computePaceSecPerKm(distanceM = 1000.0, durationSec = 360)
        assertNotNull(pace)
        assertEquals(360.0, pace!!, 0.01)
    }

    @Test
    fun computePaceSecPerKm_shortDistanceReturnsNull() {
        assertEquals(null, RunGeoUtils.computePaceSecPerKm(distanceM = 10.0, durationSec = 60))
    }

    @Test
    fun formatPace_shortDistanceShowsPlaceholder() {
        assertEquals("--'--\"", RunGeoUtils.formatPace(null))
        assertEquals("--'--\"", RunGeoUtils.formatPace(8000.0))
    }

    @Test
    fun splitTrackByGap_singlePoint_returnsEmpty() {
        val a = RunTrackPoint(39.9042, 116.4074)
        assertTrue(RunGeoUtils.splitTrackByGap(listOf(a)).isEmpty())
    }

    @Test
    fun splitTrackByGap_breaksOnLargeJump() {
        val a = RunTrackPoint(39.9042, 116.4074)
        val b = RunTrackPoint(39.9060, 116.4090) // ~250m away
        val c = RunTrackPoint(39.9060 + oneMeterLat * 5, 116.4090)
        val segments = RunGeoUtils.splitTrackByGap(listOf(a, b, c))
        assertEquals(1, segments.size)
        assertEquals(2, segments[0].size)
    }

    @Test
    fun evaluatePoint_geometryJump_rejectedWhenCapMuchSmallerThanRaw() {
        val t0 = 3_000_000L
        RunGeoUtils.evaluatePoint(
            accuracyM = 10f,
            lat = 39.9042,
            lng = 116.4074,
            lastLat = null,
            lastLng = null,
            speedKmh = 0f,
            lastRecordedAt = null,
            recordedAt = t0,
        )
        // 30m jump in 2s with poor accuracy cap should reject
        val jump = RunGeoUtils.evaluatePoint(
            accuracyM = 40f,
            lat = 39.9042 + oneMeterLat * 30,
            lng = 116.4074,
            lastLat = 39.9042,
            lastLng = 116.4074,
            speedKmh = 5f,
            lastRecordedAt = t0,
            recordedAt = t0 + 2_000L,
        )
        assertFalse(jump.accept)
        assertEquals(RunGeoUtils.RejectReason.SPIKE, jump.reason)
    }
}
