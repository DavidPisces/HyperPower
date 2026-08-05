package com.reiraku.hyperpower.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargePowerHistoryTest {
    @Test
    fun `power is recorded only while charging`() {
        val history = updateChargePowerHistory(
            history = emptyList(),
            wasCharging = false,
            isCharging = false,
            powerWatts = 18.5,
            nowMillis = 1_000L,
        )

        assertTrue(history.isEmpty())
    }

    @Test
    fun `a new charging session replaces the previous session`() {
        val previous = listOf(ChargePowerSample(timestampMillis = 1_000L, powerWatts = 12.0))

        val history = updateChargePowerHistory(
            history = previous,
            wasCharging = false,
            isCharging = true,
            powerWatts = 33.5,
            nowMillis = 10_000L,
        )

        assertEquals(listOf(ChargePowerSample(10_000L, 33.5)), history)
    }

    @Test
    fun `charging history keeps the latest five minutes`() {
        val now = CHARGE_POWER_HISTORY_WINDOW_MILLIS + 10_000L
        val history = updateChargePowerHistory(
            history = listOf(
                ChargePowerSample(timestampMillis = 9_999L, powerWatts = 10.0),
                ChargePowerSample(timestampMillis = 10_000L, powerWatts = 20.0),
            ),
            wasCharging = true,
            isCharging = true,
            powerWatts = 30.0,
            nowMillis = now,
        )

        assertEquals(2, history.size)
        assertEquals(20.0, history.first().powerWatts, 0.0)
        assertEquals(30.0, history.last().powerWatts, 0.0)
    }

    @Test
    fun `completed charging history remains available`() {
        val previous = listOf(ChargePowerSample(timestampMillis = 1_000L, powerWatts = 12.0))

        val history = updateChargePowerHistory(
            history = previous,
            wasCharging = true,
            isCharging = false,
            powerWatts = null,
            nowMillis = 2_000L,
        )

        assertEquals(previous, history)
    }
}
