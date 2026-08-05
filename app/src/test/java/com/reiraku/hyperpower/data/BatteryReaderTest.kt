package com.reiraku.hyperpower.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryReaderTest {
    @Test
    fun `discharging current is always negative`() {
        assertEquals(-482_000L, normalizeBatteryCurrent(482_000L, isCharging = false))
        assertEquals(-482_000L, normalizeBatteryCurrent(-482_000L, isCharging = false))
    }

    @Test
    fun `charging current is always positive`() {
        assertEquals(482_000L, normalizeBatteryCurrent(482_000L, isCharging = true))
        assertEquals(482_000L, normalizeBatteryCurrent(-482_000L, isCharging = true))
    }

    @Test
    fun `missing and invalid current stay unavailable`() {
        assertNull(normalizeBatteryCurrent(null, isCharging = false))
        assertNull(normalizeBatteryCurrent(Long.MIN_VALUE, isCharging = false))
    }
}
