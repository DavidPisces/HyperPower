package com.reiraku.hyperpower.data

import java.io.File

object LinuxCpuReader {
    private const val PROC_STAT = "/proc/stat"
    private const val CPU_ROOT = "/sys/devices/system/cpu"

    fun readCpuStat(): String = readText(PROC_STAT).orEmpty()

    fun readCpuInfo(): String = readText("/proc/cpuinfo").orEmpty()

    fun readCoreCount(): Int {
        val configured = readText("$CPU_ROOT/present")
            ?.trim()
            ?.let(::countCpuRange)
            ?: 0
        return configured.coerceAtLeast(Runtime.getRuntime().availableProcessors())
    }

    fun readCoreFrequencies(coreCount: Int): LongArray {
        return LongArray(coreCount) { core ->
            readFrequencyForCore(core) ?: 0L
        }
    }

    fun readCoreMidrs(coreCount: Int): Array<String> =
        Array(coreCount) { core ->
            readText("$CPU_ROOT/cpu$core/regs/identification/midr_el1")
                ?.trim()
                .orEmpty()
        }

    fun readCoreCapacities(coreCount: Int): LongArray =
        LongArray(coreCount) { core ->
            readLong("$CPU_ROOT/cpu$core/cpu_capacity") ?: 0L
        }

    fun readCoreMaxFrequencies(coreCount: Int): LongArray =
        LongArray(coreCount) { core ->
            readMaxFrequencyForCore(core) ?: 0L
        }

    internal fun countCpuRange(value: String): Int {
        return value.split(',')
            .sumOf { section ->
                val bounds = section.trim().split('-', limit = 2)
                when (bounds.size) {
                    1 -> bounds[0].toIntOrNull()?.let { 1 } ?: 0
                    else -> {
                        val start = bounds[0].toIntOrNull()
                        val end = bounds[1].toIntOrNull()
                        if (start == null || end == null || end < start) 0 else end - start + 1
                    }
                }
            }
    }

    private fun readFrequencyForCore(core: Int): Long? {
        val coreBase = "$CPU_ROOT/cpu$core/cpufreq"
        readLong("$coreBase/scaling_cur_freq")?.let { return it }
        readLong("$coreBase/cpuinfo_cur_freq")?.let { return it }

        return readPolicyFrequency(core, "scaling_cur_freq", "cpuinfo_cur_freq")
    }

    private fun readMaxFrequencyForCore(core: Int): Long? {
        val coreBase = "$CPU_ROOT/cpu$core/cpufreq"
        readLong("$coreBase/cpuinfo_max_freq")?.let { return it }
        readLong("$coreBase/scaling_max_freq")?.let { return it }

        return readPolicyFrequency(core, "cpuinfo_max_freq", "scaling_max_freq")
    }

    private fun readPolicyFrequency(core: Int, vararg fileNames: String): Long? {
        val policyRoot = File("$CPU_ROOT/cpufreq")
        val policies = runCatching {
            policyRoot.listFiles { file -> file.name.startsWith("policy") }
        }.getOrNull().orEmpty()

        for (policy in policies) {
            val related = readText(File(policy, "related_cpus").path)
                ?.trim()
                ?.split(Regex("\\s+"))
                ?.mapNotNull(String::toIntOrNull)
                .orEmpty()
            if (core in related) {
                fileNames.forEach { fileName ->
                    readLong(File(policy, fileName).path)?.let { return it }
                }
            }
        }
        return null
    }

    private fun readText(path: String): String? {
        return runCatching { File(path).readText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun readLong(path: String): Long? =
        readText(path)?.trim()?.toLongOrNull()?.takeIf { it > 0 }
}
