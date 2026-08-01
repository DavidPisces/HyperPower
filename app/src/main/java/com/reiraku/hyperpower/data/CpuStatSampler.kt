package com.reiraku.hyperpower.data

import kotlin.math.max

internal data class CpuTicks(
    val total: Long,
    val idle: Long,
)

internal data class ParsedCpuStats(
    val aggregate: CpuTicks?,
    val cores: Map<Int, CpuTicks>,
)

internal object CpuStatParser {
    fun parse(raw: String): ParsedCpuStats {
        var aggregate: CpuTicks? = null
        val cores = linkedMapOf<Int, CpuTicks>()

        raw.lineSequence()
            .filter { line -> line.startsWith("cpu") }
            .forEach { line ->
                val fields = line.trim().split(Regex("\\s+"))
                val name = fields.firstOrNull() ?: return@forEach
                val values = fields.drop(1).mapNotNull(String::toLongOrNull)
                if (values.size < 4) return@forEach

                val total = values.sum()
                val idle = values.getOrElse(3) { 0L } + values.getOrElse(4) { 0L }
                val ticks = CpuTicks(total = total, idle = idle)

                if (name == "cpu") {
                    aggregate = ticks
                } else {
                    name.removePrefix("cpu").toIntOrNull()?.let { cores[it] = ticks }
                }
            }

        return ParsedCpuStats(aggregate = aggregate, cores = cores)
    }
}

class CpuStatSampler {
    private var previous: ParsedCpuStats? = null

    fun reset() {
        previous = null
    }

    fun sample(
        rawStat: String,
        frequenciesKhz: LongArray,
        fallbackCoreCount: Int,
        identities: List<CpuIdentity> = emptyList(),
    ): CpuMetrics {
        val current = CpuStatParser.parse(rawStat)
        val prior = previous
        if (current.aggregate != null || current.cores.isNotEmpty()) {
            previous = current
        }

        val parsedCount = (current.cores.keys.maxOrNull()?.plus(1)) ?: 0
        val coreCount = max(
            fallbackCoreCount,
            max(parsedCount, frequenciesKhz.size),
        )

        val cores = List(coreCount) { id ->
            CpuCoreMetric(
                id = id,
                usage = calculateUsage(prior?.cores?.get(id), current.cores[id]),
                frequencyKhz = frequenciesKhz.getOrNull(id)?.takeIf { it > 0L },
                architecture = identities.getOrNull(id)?.architecture,
                role = identities.getOrNull(id)?.role ?: CpuCoreRole.UNKNOWN,
            )
        }

        return CpuMetrics(
            overallUsage = calculateUsage(prior?.aggregate, current.aggregate),
            cores = cores,
        )
    }

    private fun calculateUsage(previous: CpuTicks?, current: CpuTicks?): Float? {
        if (previous == null || current == null) return null
        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        if (totalDelta <= 0L) return null
        return ((totalDelta - idleDelta).toFloat() / totalDelta.toFloat())
            .coerceIn(0f, 1f)
    }
}
