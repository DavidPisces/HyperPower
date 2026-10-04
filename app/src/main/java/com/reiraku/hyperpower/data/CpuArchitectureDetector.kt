package com.reiraku.hyperpower.data

data class CpuIdentity(
    val architecture: String?,
    val role: CpuCoreRole,
    val rawIdentifier: String? = null,
)

/**
 * 一次身份识别所需的原始数据。
 *
 * 来源可以是 Root（`su` 读取）、Shizuku（特权进程读取）或应用直读 sysfs —— 三者结构一致，
 * 便于用 [mergedWith] 互补：例如特权进程读不到 `cpu_capacity` 时用应用直读的那份补齐。
 */
internal data class CpuProfile(
    val midrs: Array<String>,
    val cpuInfo: String,
    val capacities: LongArray,
    val maxFrequenciesKhz: LongArray,
) {
    fun mergedWith(fallback: CpuProfile): CpuProfile = CpuProfile(
        midrs = if (midrs.any { it.isNotBlank() }) midrs else fallback.midrs,
        cpuInfo = cpuInfo.ifBlank { fallback.cpuInfo },
        capacities = if (capacities.any { it > 0L }) capacities else fallback.capacities,
        maxFrequenciesKhz = if (maxFrequenciesKhz.any { it > 0L }) {
            maxFrequenciesKhz
        } else {
            fallback.maxFrequenciesKhz
        },
    )
}

internal object CpuArchitectureDetector {
    fun detect(
        coreCount: Int,
        midrs: Array<String>,
        cpuInfo: String,
        capacities: LongArray,
        maxFrequenciesKhz: LongArray,
    ): List<CpuIdentity> {
        val cpuInfoCores = parseCpuInfo(cpuInfo)
        val fields = List(coreCount) { core ->
            val midrFields = parseMidr(midrs.getOrNull(core))
            val cpuInfoFields = cpuInfoCores[core]
            CoreFields(
                implementer = midrFields?.implementer ?: cpuInfoFields?.implementer,
                part = midrFields?.part ?: cpuInfoFields?.part,
                modelName = cpuInfoFields?.modelName,
            )
        }
        val known = fields.map { knownCore(it.implementer, it.part) }
        val clusterRoles = resolveClusterRoles(
            coreCount = coreCount,
            known = known,
            capacities = capacities,
            maxFrequenciesKhz = maxFrequenciesKhz,
        )

        return List(coreCount) { core ->
            CpuIdentity(
                architecture = known[core]?.architecture
                    ?: fields[core].modelName?.takeIf(::isUsefulModelName),
                // 簇结构优先；判不出来时退回型号表；再判不出来就保持 UNKNOWN（UI 不展示标签）。
                role = clusterRoles[core]
                    ?: known[core]?.role
                    ?: CpuCoreRole.UNKNOWN,
                rawIdentifier = rawIdentifier(fields[core].implementer, fields[core].part),
            )
        }
    }

    /**
     * 角色由**簇结构**决定，核心型号只负责命名。
     *
     * 过去是"型号命中就静态定角色"，同一个 part 出现在多个簇时会出错：1+3+4 的 SoC
     * （prime 与 gold 同为 Cortex-A76/A77/A78）会把 prime 核也标成性能核。
     *
     * 分簇优先用调度器容量 `cpu_capacity`（同簇取值相同），容量读不到时退化为最高频率；
     * 相邻取值相对差在 [CLUSTER_TOLERANCE_PERCENT]% 以内视为同一簇，避免同簇核因逐核微差被拆开。
     * 返回 null 表示该核无法判定，由调用方回退到型号表。
     */
    private fun resolveClusterRoles(
        coreCount: Int,
        known: List<KnownCore?>,
        capacities: LongArray,
        maxFrequenciesKhz: LongArray,
    ): List<CpuCoreRole?> {
        val scores = scoresFor(coreCount, capacities, maxFrequenciesKhz)
            ?: return List(coreCount) { null }

        // 全部核心同型号的同构 SoC 没有大小核之分，直接标为通用核。
        val architectures = known.map { it?.architecture }
        if (scores.all { it != null } &&
            architectures.all { it != null } &&
            architectures.distinct().size == 1
        ) {
            return List(coreCount) { CpuCoreRole.GENERAL }
        }

        val clusters = clusterByScore(scores)
        if (clusters.isEmpty()) return List(coreCount) { null }

        val roles = MutableList<CpuCoreRole?>(coreCount) { null }
        if (clusters.size == 1) {
            clusters.first().forEach { roles[it] = CpuCoreRole.GENERAL }
            return roles
        }

        clusters.first().forEach { roles[it] = CpuCoreRole.EFFICIENCY }
        for (index in 1 until clusters.lastIndex) {
            clusters[index].forEach { roles[it] = CpuCoreRole.PERFORMANCE }
        }
        val topCluster = clusters.last()
        val topRole = if (isPrimeCluster(topCluster, clusters.size, known)) {
            CpuCoreRole.ULTRA
        } else {
            CpuCoreRole.PERFORMANCE
        }
        topCluster.forEach { roles[it] = topRole }
        return roles
    }

    /** 每个核心的簇评分：优先容量，容量全读不到时用最高频率；读不到的核保持 null。 */
    private fun scoresFor(
        coreCount: Int,
        capacities: LongArray,
        maxFrequenciesKhz: LongArray,
    ): List<Long?>? {
        val capacityScores = List(coreCount) { core ->
            capacities.getOrElse(core) { 0L }.takeIf { it > 0L }
        }
        if (capacityScores.any { it != null }) return capacityScores

        val frequencyScores = List(coreCount) { core ->
            maxFrequenciesKhz.getOrElse(core) { 0L }.takeIf { it > 0L }
        }
        return frequencyScores.takeIf { scores -> scores.any { it != null } }
    }

    /** 按评分升序分簇，返回每个簇的核心下标。 */
    private fun clusterByScore(scores: List<Long?>): List<List<Int>> {
        val clusters = mutableListOf<MutableList<Int>>()
        var clusterMax = 0L
        scores.withIndex()
            .filter { it.value != null }
            .sortedBy { it.value }
            .forEach { (index, value) ->
                val score = value ?: return@forEach
                val newCluster = clusters.isEmpty() ||
                    score * 100L > clusterMax * (100L + CLUSTER_TOLERANCE_PERCENT)
                if (newCluster) {
                    clusters += mutableListOf(index)
                    clusterMax = score
                } else {
                    clusters.last() += index
                    clusterMax = maxOf(clusterMax, score)
                }
            }
        return clusters
    }

    /**
     * 顶簇是否是超大核：
     * - 顶簇里有 X 系型号（型号表里角色即 ULTRA）时成立；
     * - 或者存在中间簇、且顶簇只有 1~2 个核心（1+3+4 / 1+2+4 这类 prime 结构）。
     *
     * 只有两个簇时（例如 2+6 的 SoC）不判超大核，避免把"大核"误标成"超大核"。
     */
    private fun isPrimeCluster(
        topCluster: List<Int>,
        clusterCount: Int,
        known: List<KnownCore?>,
    ): Boolean {
        if (topCluster.any { known.getOrNull(it)?.role == CpuCoreRole.ULTRA }) return true
        return clusterCount >= 3 && topCluster.size <= MAX_PRIME_CLUSTER_CORES
    }

    private fun parseMidr(raw: String?): CpuIdFields? {
        val value = parseNumber(raw) ?: return null
        return CpuIdFields(
            implementer = ((value ushr 24) and 0xff).toInt(),
            part = ((value ushr 4) and 0xfff).toInt(),
        )
    }

    private fun parseCpuInfo(raw: String): Map<Int, CpuInfoFields> {
        val result = mutableMapOf<Int, CpuInfoFields>()
        raw.split(Regex("\\r?\\n\\s*\\r?\\n"))
            .forEach { block ->
                val fields = block.lineSequence()
                    .mapNotNull { line ->
                        val separator = line.indexOf(':')
                        if (separator < 0) null
                        else line.substring(0, separator).trim().lowercase() to
                            line.substring(separator + 1).trim()
                    }
                    .toMap()
                val processor = fields["processor"]?.toIntOrNull() ?: return@forEach
                result[processor] = CpuInfoFields(
                    implementer = parseNumber(fields["cpu implementer"])?.toInt(),
                    part = parseNumber(fields["cpu part"])?.toInt(),
                    modelName = fields["model name"] ?: fields["processor model"],
                )
            }
        return result
    }

    private fun parseNumber(raw: String?): Long? {
        val value = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.startsWith("0x")) {
            value.removePrefix("0x").toLongOrNull(16)
        } else {
            value.toLongOrNull()
        }
    }

    private fun knownCore(implementer: Int?, part: Int?): KnownCore? {
        if (implementer != ARM_IMPLEMENTER || part == null) return null
        return ARM_CORES[part]
    }

    private fun rawIdentifier(implementer: Int?, part: Int?): String? {
        if (implementer == null || part == null) return null
        val vendor = IMPLEMENTERS[implementer] ?: "厂商 0x%02X".format(implementer)
        return "$vendor 0x%03X".format(part)
    }

    private fun isUsefulModelName(value: String): Boolean {
        val lower = value.lowercase()
        return value.isNotBlank() &&
            !lower.startsWith("armv") &&
            !lower.startsWith("aarch64 processor")
    }

    private data class CoreFields(
        val implementer: Int?,
        val part: Int?,
        val modelName: String?,
    )

    private data class CpuIdFields(
        val implementer: Int,
        val part: Int,
    )

    private data class CpuInfoFields(
        val implementer: Int?,
        val part: Int?,
        val modelName: String?,
    )

    private data class KnownCore(
        val architecture: String,
        val role: CpuCoreRole,
    )

    private const val ARM_IMPLEMENTER = 0x41

    /** 相邻容量/频率的相对差在这个百分比以内视为同一簇。 */
    private const val CLUSTER_TOLERANCE_PERCENT = 10L

    /** 顶簇核心数不超过该值且存在中间簇时，按超大核处理。 */
    private const val MAX_PRIME_CLUSTER_CORES = 2

    private val IMPLEMENTERS = mapOf(
        0x41 to "ARM",
        0x42 to "Broadcom",
        0x43 to "Cavium",
        0x4e to "NVIDIA",
        0x51 to "Qualcomm",
        0x53 to "Samsung",
        0x61 to "Apple",
    )

    private val ARM_CORES = mapOf(
        0xC05 to KnownCore("Cortex-A5", CpuCoreRole.EFFICIENCY),
        0xC07 to KnownCore("Cortex-A7", CpuCoreRole.EFFICIENCY),
        0xC08 to KnownCore("Cortex-A8", CpuCoreRole.PERFORMANCE),
        0xC09 to KnownCore("Cortex-A9", CpuCoreRole.PERFORMANCE),
        0xC0D to KnownCore("Cortex-A12", CpuCoreRole.PERFORMANCE),
        0xC0F to KnownCore("Cortex-A15", CpuCoreRole.PERFORMANCE),
        0xC0E to KnownCore("Cortex-A17", CpuCoreRole.PERFORMANCE),
        0xD01 to KnownCore("Cortex-A32", CpuCoreRole.EFFICIENCY),
        0xD02 to KnownCore("Cortex-A34", CpuCoreRole.EFFICIENCY),
        0xD03 to KnownCore("Cortex-A53", CpuCoreRole.EFFICIENCY),
        0xD04 to KnownCore("Cortex-A35", CpuCoreRole.EFFICIENCY),
        0xD05 to KnownCore("Cortex-A55", CpuCoreRole.EFFICIENCY),
        0xD06 to KnownCore("Cortex-A65", CpuCoreRole.EFFICIENCY),
        0xD07 to KnownCore("Cortex-A57", CpuCoreRole.PERFORMANCE),
        0xD08 to KnownCore("Cortex-A72", CpuCoreRole.PERFORMANCE),
        0xD09 to KnownCore("Cortex-A73", CpuCoreRole.PERFORMANCE),
        0xD0A to KnownCore("Cortex-A75", CpuCoreRole.PERFORMANCE),
        0xD0B to KnownCore("Cortex-A76", CpuCoreRole.PERFORMANCE),
        0xD0D to KnownCore("Cortex-A77", CpuCoreRole.PERFORMANCE),
        0xD41 to KnownCore("Cortex-A78", CpuCoreRole.PERFORMANCE),
        0xD44 to KnownCore("Cortex-X1", CpuCoreRole.ULTRA),
        0xD46 to KnownCore("Cortex-A510", CpuCoreRole.EFFICIENCY),
        0xD47 to KnownCore("Cortex-A710", CpuCoreRole.PERFORMANCE),
        0xD48 to KnownCore("Cortex-X2", CpuCoreRole.ULTRA),
        0xD4D to KnownCore("Cortex-A715", CpuCoreRole.PERFORMANCE),
        0xD4E to KnownCore("Cortex-X3", CpuCoreRole.ULTRA),
        0xD80 to KnownCore("Cortex-A520", CpuCoreRole.EFFICIENCY),
        0xD81 to KnownCore("Cortex-A720", CpuCoreRole.PERFORMANCE),
        0xD82 to KnownCore("Cortex-X4", CpuCoreRole.ULTRA),
        0xD85 to KnownCore("Cortex-X925", CpuCoreRole.ULTRA),
        0xD87 to KnownCore("Cortex-A725", CpuCoreRole.PERFORMANCE),
    )
}
