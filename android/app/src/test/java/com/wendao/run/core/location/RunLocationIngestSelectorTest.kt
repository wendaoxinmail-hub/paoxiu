package com.wendao.run.core.location

import com.wendao.run.core.location.RunLocationUpdate
import org.junit.Assert.assertEquals
import org.junit.Test

class RunLocationIngestSelectorTest {

    @Test
    fun system_alwaysIngests() {
        val ingested = mutableListOf<String>()
        val selector = RunLocationIngestSelector(
            onReanchor = {},
            onIngest = { _, source -> ingested += source },
        )
        val fix = fix()
        selector.onSystem(fix)
        selector.onSystem(fix)
        assertEquals(listOf("system", "system"), ingested)
    }

    @Test
    fun baidu_skippedWhenSystemRecent() {
        val ingested = mutableListOf<String>()
        val selector = RunLocationIngestSelector(
            onReanchor = {},
            onIngest = { _, source -> ingested += source },
        )
        selector.onSystem(fix())
        selector.onBaidu(fix())
        assertEquals(listOf("system"), ingested)
    }

    private fun fix() = RunLocationUpdate(
        lat = 39.9,
        lng = 116.4,
        accuracyM = 10f,
        speedKmh = 5f,
        recordedAt = System.currentTimeMillis(),
        locType = SystemRunLocationSource.SYSTEM_LOC_TYPE,
        provider = "gps",
    )
}
