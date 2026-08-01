package com.reiraku.hyperpower

import com.reiraku.hyperpower.data.CpuStatSampler
import com.reiraku.hyperpower.data.LinuxCpuReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CpuStatSamplerTest {
    private val first = """
        cpu  100 0 100 800 0 0 0 0 0 0
        cpu0 50 0 50 400 0 0 0 0 0 0
        cpu1 50 0 50 400 0 0 0 0 0 0
    """.trimIndent()

    private val second = """
        cpu  150 0 150 900 0 0 0 0 0 0
        cpu0 75 0 75 450 0 0 0 0 0 0
        cpu1 75 0 75 450 0 0 0 0 0 0
    """.trimIndent()

    @Test
    fun firstSample_hasNoUsageDelta_butKeepsFrequencyAndCoreCount() {
        val result = CpuStatSampler().sample(
            rawStat = first,
            frequenciesKhz = longArrayOf(1_200_000L, 2_400_000L),
            fallbackCoreCount = 2,
        )

        assertNull(result.overallUsage)
        assertEquals(2, result.coreCount)
        assertEquals(1_200_000L, result.cores[0].frequencyKhz)
        assertNull(result.cores[0].usage)
    }

    @Test
    fun secondSample_calculatesAggregateAndPerCoreUsage() {
        val sampler = CpuStatSampler()
        sampler.sample(first, longArrayOf(1_200_000L, 2_400_000L), 2)

        val result = sampler.sample(
            rawStat = second,
            frequenciesKhz = longArrayOf(1_400_000L, 2_600_000L),
            fallbackCoreCount = 2,
        )

        assertEquals(0.5f, result.overallUsage ?: -1f, 0.0001f)
        assertEquals(0.5f, result.cores[0].usage ?: -1f, 0.0001f)
        assertEquals(0.5f, result.cores[1].usage ?: -1f, 0.0001f)
    }

    @Test
    fun cpuRangeParser_supportsClustersAndSingleCores() {
        assertEquals(8, LinuxCpuReader.countCpuRange("0-3,6-9"))
        assertEquals(4, LinuxCpuReader.countCpuRange("0,2,4-5"))
        assertEquals(0, LinuxCpuReader.countCpuRange(""))
    }
}
