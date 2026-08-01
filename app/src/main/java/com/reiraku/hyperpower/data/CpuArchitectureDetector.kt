package com.reiraku.hyperpower.data

data class CpuIdentity(
    val architecture: String?,
    val role: CpuCoreRole,
    val rawIdentifier: String? = null,
)

internal object CpuArchitectureDetector {
    fun detect(
        coreCount: Int,
        midrs: Array<String>,
        cpuInfo: String,
        capacities: LongArray,
        maxFrequenciesKhz: LongArray,
    ): List<CpuIdentity> {
        val cpuInfoCores = parseCpuInfo(cpuInfo)
        val identities = List(coreCount) { core ->
            val midrFields = parseMidr(midrs.getOrNull(core))
            val cpuInfoFields = cpuInfoCores[core]
            val implementer = midrFields?.implementer ?: cpuInfoFields?.implementer
            val part = midrFields?.part ?: cpuInfoFields?.part
            val known = knownCore(implementer, part)
            val rawIdentifier = rawIdentifier(implementer, part)
            CpuIdentity(
                architecture = known?.architecture
                    ?: cpuInfoFields?.modelName?.takeIf(::isUsefulModelName)
                    ?: null,
                role = known?.role ?: CpuCoreRole.UNKNOWN,
                rawIdentifier = rawIdentifier,
            )
        }

        val inferredRoles = inferRoles(
            coreCount = coreCount,
            capacities = capacities,
            maxFrequenciesKhz = maxFrequenciesKhz,
        )
        return identities.mapIndexed { index, identity ->
            if (identity.role == CpuCoreRole.UNKNOWN) {
                identity.copy(role = inferredRoles.getOrElse(index) { CpuCoreRole.UNKNOWN })
            } else {
                identity
            }
        }
    }

    private fun inferRoles(
        coreCount: Int,
        capacities: LongArray,
        maxFrequenciesKhz: LongArray,
    ): List<CpuCoreRole> {
        val capacityScores = List(coreCount) { capacities.getOrElse(it) { 0L } }
        val frequencyScores = List(coreCount) { maxFrequenciesKhz.getOrElse(it) { 0L } }
        val scores = when {
            capacityScores.count { it > 0L } == coreCount -> capacityScores
            frequencyScores.count { it > 0L } == coreCount -> frequencyScores
            else -> return List(coreCount) { CpuCoreRole.UNKNOWN }
        }
        val clusters = scores.distinct().sorted()
        return when (clusters.size) {
            0 -> List(coreCount) { CpuCoreRole.UNKNOWN }
            1 -> List(coreCount) { CpuCoreRole.GENERAL }
            2 -> scores.map { score ->
                if (score == clusters.first()) CpuCoreRole.EFFICIENCY
                else CpuCoreRole.PERFORMANCE
            }
            else -> scores.map { score ->
                when (score) {
                    clusters.first() -> CpuCoreRole.EFFICIENCY
                    clusters.last() -> CpuCoreRole.ULTRA
                    else -> CpuCoreRole.PERFORMANCE
                }
            }
        }
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
        0xD06 to KnownCore("Cortex-A65", CpuCoreRole.PERFORMANCE),
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
