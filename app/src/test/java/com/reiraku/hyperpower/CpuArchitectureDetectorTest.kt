package com.reiraku.hyperpower

import com.reiraku.hyperpower.data.CpuArchitectureDetector
import com.reiraku.hyperpower.data.CpuCoreRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CpuArchitectureDetectorTest {
    @Test
    fun midr_identifiesArmCoreModelsAndRoles() {
        val result = CpuArchitectureDetector.detect(
            coreCount = 2,
            midrs = arrayOf("0x410fd034", "0x410fd440"),
            cpuInfo = "",
            capacities = longArrayOf(512L, 1_024L),
            maxFrequenciesKhz = longArrayOf(1_800_000L, 2_840_000L),
        )

        assertEquals("Cortex-A53", result[0].architecture)
        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals("Cortex-X1", result[1].architecture)
        assertEquals(CpuCoreRole.ULTRA, result[1].role)
    }

    @Test
    fun cpuInfo_isUsedWhenMidrSysfsIsUnavailable() {
        val cpuInfo = """
            processor       : 0
            CPU implementer : 0x41
            CPU part        : 0xd05

            processor       : 1
            CPU implementer : 0x41
            CPU part        : 0xd0b
        """.trimIndent()

        val result = CpuArchitectureDetector.detect(
            coreCount = 2,
            midrs = arrayOf("", ""),
            cpuInfo = cpuInfo,
            capacities = longArrayOf(),
            maxFrequenciesKhz = longArrayOf(),
        )

        assertEquals("Cortex-A55", result[0].architecture)
        assertEquals("Cortex-A76", result[1].architecture)
    }

    @Test
    fun unknownModels_useCapacityClustersForRoleOnly() {
        val result = CpuArchitectureDetector.detect(
            coreCount = 4,
            midrs = Array(4) { "0x510f0010" },
            cpuInfo = "",
            capacities = longArrayOf(384L, 384L, 768L, 1_024L),
            maxFrequenciesKhz = longArrayOf(),
        )

        assertEquals(null, result[0].architecture)
        assertEquals("Qualcomm 0x001", result[0].rawIdentifier)
        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.PERFORMANCE, result[2].role)
        assertEquals(CpuCoreRole.ULTRA, result[3].role)
    }
}
