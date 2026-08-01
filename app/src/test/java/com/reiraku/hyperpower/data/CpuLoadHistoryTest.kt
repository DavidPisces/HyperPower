package com.reiraku.hyperpower.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CpuLoadHistoryTest {
    @Test
    fun `history keeps only the latest thirty seconds`() {
        val now = 100_000L
        val history = listOf(
            CpuLoadSample(timestampMillis = now - 31_000L, usage = 0.1f),
            CpuLoadSample(timestampMillis = now - 30_000L, usage = 0.2f),
            CpuLoadSample(timestampMillis = now - 1_000L, usage = 0.3f),
        )

        val updated = updateCpuLoadHistory(history, usage = 0.4f, nowMillis = now)

        assertEquals(3, updated.size)
        assertEquals(now - 30_000L, updated.first().timestampMillis)
        assertEquals(0.4f, updated.last().usage)
    }

    @Test
    fun `history clamps invalid load values`() {
        val updated = updateCpuLoadHistory(emptyList(), usage = 1.4f, nowMillis = 1L)

        assertEquals(1f, updated.single().usage)
    }
}
