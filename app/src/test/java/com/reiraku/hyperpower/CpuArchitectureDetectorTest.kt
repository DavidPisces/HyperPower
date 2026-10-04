package com.reiraku.hyperpower

import com.reiraku.hyperpower.data.CpuArchitectureDetector
import com.reiraku.hyperpower.data.CpuCoreRole
import org.junit.Assert.assertEquals
import org.junit.Test

class CpuArchitectureDetectorTest {
    @Test
    fun midr_identifiesArmCoreModelsAndRoles() {
        val result = detect(
            parts = listOf(CORTEX_A53, CORTEX_X1),
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
        val result = detect(
            parts = List(4) { UNKNOWN_QUALCOMM },
            implementer = QUALCOMM_IMPLEMENTER,
            capacities = longArrayOf(384L, 384L, 768L, 1_024L),
        )

        assertEquals(null, result[0].architecture)
        assertEquals("Qualcomm 0x001", result[0].rawIdentifier)
        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.PERFORMANCE, result[2].role)
        assertEquals(CpuCoreRole.ULTRA, result[3].role)
    }

    @Test
    fun sameModelPrimeCore_isUltraWhileGoldClusterStaysPerformance() {
        // 1+3+4：prime 与 gold 同为 Cortex-A76，静态型号表无法区分，只能靠簇结构。
        val result = detect(
            parts = listOf(
                CORTEX_A55, CORTEX_A55, CORTEX_A55, CORTEX_A55,
                CORTEX_A76, CORTEX_A76, CORTEX_A76, CORTEX_A76,
            ),
            capacities = longArrayOf(256L, 256L, 256L, 256L, 768L, 768L, 768L, 1_024L),
        )

        assertEquals("Cortex-A76", result[4].architecture)
        assertEquals("Cortex-A76", result[7].architecture)
        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.PERFORMANCE, result[4].role)
        assertEquals(CpuCoreRole.ULTRA, result[7].role)
    }

    @Test
    fun twoClusterSoc_doesNotInventAnUltraCore() {
        // 2+6：只有两个簇时顶簇是"大核"，不应标成超大核。
        val result = detect(
            parts = listOf(
                CORTEX_A55, CORTEX_A55, CORTEX_A55,
                CORTEX_A55, CORTEX_A55, CORTEX_A55,
                CORTEX_A76, CORTEX_A76,
            ),
            capacities = longArrayOf(256L, 256L, 256L, 256L, 256L, 256L, 768L, 768L),
        )

        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.PERFORMANCE, result[6].role)
        assertEquals(CpuCoreRole.PERFORMANCE, result[7].role)
    }

    @Test
    fun missingCapacityOnSomeCores_doesNotDiscardTheWholeInference() {
        val result = detect(
            parts = listOf(CORTEX_A55, CORTEX_A55, CORTEX_A76, CORTEX_X1),
            capacities = longArrayOf(0L, 0L, 512L, 1_024L),
        )

        // 有数据的核按簇判定，没数据的核回退到型号表，而不是整批 UNKNOWN。
        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.EFFICIENCY, result[2].role)
        assertEquals(CpuCoreRole.ULTRA, result[3].role)
    }

    @Test
    fun nearEqualCapacities_stayInTheSameCluster() {
        val result = detect(
            parts = listOf(CORTEX_A78, CORTEX_A78, CORTEX_A78, CORTEX_X1),
            capacities = longArrayOf(1_000L, 1_000L, 1_050L, 2_100L),
        )

        assertEquals(CpuCoreRole.EFFICIENCY, result[0].role)
        assertEquals(CpuCoreRole.EFFICIENCY, result[2].role)
        assertEquals(CpuCoreRole.ULTRA, result[3].role)
    }

    @Test
    fun homogeneousSoc_isGeneralEvenWhenClocksDifferWithinOneCluster() {
        // 8×A53 单簇、只是频率档位不同：没有大小核之分。
        val result = detect(
            parts = List(8) { CORTEX_A53 },
            maxFrequenciesKhz = longArrayOf(
                1_800_000L, 1_800_000L, 1_800_000L, 1_800_000L,
                2_200_000L, 2_200_000L, 2_200_000L, 2_200_000L,
            ),
        )

        assertEquals(List(8) { CpuCoreRole.GENERAL }, result.map { it.role })
    }

    @Test
    fun noReadableData_leavesEverythingUnknown() {
        val result = CpuArchitectureDetector.detect(
            coreCount = 4,
            midrs = arrayOf("", "", "", ""),
            cpuInfo = "",
            capacities = longArrayOf(),
            maxFrequenciesKhz = longArrayOf(),
        )

        assertEquals(List(4) { null }, result.map { it.architecture })
        assertEquals(List(4) { CpuCoreRole.UNKNOWN }, result.map { it.role })
    }

    private fun detect(
        parts: List<Int>,
        implementer: Int = ARM_IMPLEMENTER,
        capacities: LongArray = longArrayOf(),
        maxFrequenciesKhz: LongArray = longArrayOf(),
    ) = CpuArchitectureDetector.detect(
        coreCount = parts.size,
        midrs = parts.map { midr(it, implementer) }.toTypedArray(),
        cpuInfo = "",
        capacities = capacities,
        maxFrequenciesKhz = maxFrequenciesKhz,
    )

    /** 组装 MIDR_EL1：implementer=[31:24]、part=[15:4]。 */
    private fun midr(part: Int, implementer: Int = ARM_IMPLEMENTER): String =
        "0x%08x".format((implementer shl 24) or (part shl 4))

    private companion object {
        const val ARM_IMPLEMENTER = 0x41
        const val QUALCOMM_IMPLEMENTER = 0x51

        const val CORTEX_A53 = 0xD03
        const val CORTEX_A55 = 0xD05
        const val CORTEX_A76 = 0xD0B
        const val CORTEX_A78 = 0xD41
        const val CORTEX_X1 = 0xD44

        /** part 不在任何型号表里（配合 [QUALCOMM_IMPLEMENTER] 使用）。 */
        const val UNKNOWN_QUALCOMM = 0x001
    }
}
