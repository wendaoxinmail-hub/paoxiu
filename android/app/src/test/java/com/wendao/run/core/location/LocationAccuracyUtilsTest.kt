package com.wendao.run.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationAccuracyUtilsTest {

    @Test
    fun normalize_zeroOrNegative_returnsDefault() {
        assertEquals(18f, LocationAccuracyUtils.normalizeAccuracyM(0f))
        assertEquals(18f, LocationAccuracyUtils.normalizeAccuracyM(-1f))
    }

    @Test
    fun normalize_valid_passesThrough() {
        assertEquals(8f, LocationAccuracyUtils.normalizeAccuracyM(8f))
    }

    @Test
    fun normalize_excessive_capsAtMax() {
        assertEquals(120f, LocationAccuracyUtils.normalizeAccuracyM(500f))
    }
}
