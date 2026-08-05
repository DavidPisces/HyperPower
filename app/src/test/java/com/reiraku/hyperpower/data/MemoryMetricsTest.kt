package com.reiraku.hyperpower.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemoryMetricsTest {
    @Test
    fun `used memory and usage are derived from available memory`() {
        val memory = MemoryMetrics(
            totalBytes = 16_000L,
            availableBytes = 6_000L,
        )

        assertEquals(10_000L, memory.usedBytes)
        assertEquals(0.625f, memory.usage ?: 0f, 0f)
    }

    @Test
    fun `used memory is clamped to valid bounds`() {
        assertEquals(
            0L,
            MemoryMetrics(totalBytes = 8_000L, availableBytes = 9_000L).usedBytes,
        )
        assertEquals(
            8_000L,
            MemoryMetrics(totalBytes = 8_000L, availableBytes = -1_000L).usedBytes,
        )
    }

    @Test
    fun `missing totals keep derived values unavailable`() {
        val memory = MemoryMetrics(availableBytes = 4_000L)

        assertNull(memory.usedBytes)
        assertNull(memory.usage)
    }
}
