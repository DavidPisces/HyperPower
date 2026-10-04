package com.reiraku.hyperpower.data

import java.io.IOException
import java.util.concurrent.TimeUnit

internal enum class RootProbeResult {
    GRANTED,
    DENIED,
    UNAVAILABLE,
}

internal data class RootCpuReadout(
    val rawStat: String,
    val coreCount: Int,
    val frequenciesKhz: LongArray,
)

/**
 * 所有命令都是应用内固定的只读命令，不接收外部输入。
 * 每次采样只启动一次 su，避免逐节点反复唤起 Root 管理器。
 */
internal object RootCpuReader {
    private const val CPU_ROOT = "/sys/devices/system/cpu"

    fun probe(): RootProbeResult {
        val result = runRoot("id -u", ROOT_AUTH_TIMEOUT_SECONDS)
        return when {
            result.unavailable -> RootProbeResult.UNAVAILABLE
            result.exitCode == 0 && result.output.lineSequence().any { it.trim() == "0" } ->
                RootProbeResult.GRANTED
            else -> RootProbeResult.DENIED
        }
    }

    fun readSnapshot(): RootCpuReadout? {
        val coreCount = LinuxCpuReader.readCoreCount().coerceIn(1, MAX_CORES)
        val command = buildString {
            append("echo '$STAT_BEGIN'; cat /proc/stat; echo '$STAT_END'; ")
            repeat(coreCount) { core ->
                val base = "$CPU_ROOT/cpu$core/cpufreq"
                append("value=\$(cat '$base/scaling_cur_freq' 2>/dev/null); ")
                append("if [ -z \"\$value\" ]; then ")
                append("value=\$(cat '$base/cpuinfo_cur_freq' 2>/dev/null); fi; ")
                append("printf '$FREQUENCY_PREFIX$core=%s\\n' \"\${value:-0}\"; ")
            }
        }
        val result = runRoot(command, ROOT_READ_TIMEOUT_SECONDS)
        if (result.exitCode != 0) return null

        val rawStat = result.output.section(STAT_BEGIN, STAT_END)
        if (rawStat.isBlank()) return null
        return RootCpuReadout(
            rawStat = rawStat,
            coreCount = coreCount,
            frequenciesKhz = LongArray(coreCount) { core ->
                result.output.taggedLong("$FREQUENCY_PREFIX$core")
            },
        )
    }

    fun readProfile(coreCount: Int): CpuProfile? {
        val safeCoreCount = coreCount.coerceIn(1, MAX_CORES)
        val command = buildString {
            append("echo '$CPU_INFO_BEGIN'; cat /proc/cpuinfo; echo '$CPU_INFO_END'; ")
            repeat(safeCoreCount) { core ->
                val cpuBase = "$CPU_ROOT/cpu$core"
                val frequencyBase = "$cpuBase/cpufreq"
                appendReadValue(
                    tag = "$MIDR_PREFIX$core",
                    primaryPath = "$cpuBase/regs/identification/midr_el1",
                )
                appendReadValue(
                    tag = "$CAPACITY_PREFIX$core",
                    primaryPath = "$cpuBase/cpu_capacity",
                )
                appendReadValue(
                    tag = "$MAX_FREQUENCY_PREFIX$core",
                    primaryPath = "$frequencyBase/cpuinfo_max_freq",
                    fallbackPath = "$frequencyBase/scaling_max_freq",
                )
            }
        }
        val result = runRoot(command, ROOT_READ_TIMEOUT_SECONDS)
        if (result.exitCode != 0) return null

        return CpuProfile(
            midrs = Array(safeCoreCount) { core ->
                result.output.taggedValue("$MIDR_PREFIX$core")
            },
            cpuInfo = result.output.section(CPU_INFO_BEGIN, CPU_INFO_END),
            capacities = LongArray(safeCoreCount) { core ->
                result.output.taggedLong("$CAPACITY_PREFIX$core")
            },
            maxFrequenciesKhz = LongArray(safeCoreCount) { core ->
                result.output.taggedLong("$MAX_FREQUENCY_PREFIX$core")
            },
        )
    }

    private fun StringBuilder.appendReadValue(
        tag: String,
        primaryPath: String,
        fallbackPath: String? = null,
    ) {
        append("value=\$(cat '$primaryPath' 2>/dev/null); ")
        if (fallbackPath != null) {
            append("if [ -z \"\$value\" ]; then ")
            append("value=\$(cat '$fallbackPath' 2>/dev/null); fi; ")
        }
        append("printf '$tag=%s\\n' \"\${value:-}\"; ")
    }

    private fun runRoot(command: String, timeoutSeconds: Long): RootCommandResult {
        val process = try {
            ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
        } catch (_: IOException) {
            return RootCommandResult(exitCode = -1, output = "", unavailable = true)
        }

        val output = StringBuilder()
        val readerThread = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> output.appendLine(line) }
            }
        }.apply {
            name = "hyperpower-root-output"
            isDaemon = true
            start()
        }

        val finished = runCatching {
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        }.getOrDefault(false)
        if (!finished) {
            process.destroyForcibly()
        }
        runCatching { readerThread.join(1_000L) }
        return RootCommandResult(
            exitCode = if (finished) process.exitValue() else -1,
            output = output.toString(),
            unavailable = false,
        )
    }

    private fun String.section(begin: String, end: String): String =
        substringAfter("$begin\n", "")
            .substringBefore("\n$end", "")
            .trim()

    private fun String.taggedValue(tag: String): String =
        lineSequence()
            .firstOrNull { it.startsWith("$tag=") }
            ?.substringAfter('=')
            ?.trim()
            .orEmpty()

    private fun String.taggedLong(tag: String): Long =
        taggedValue(tag).toLongOrNull()?.takeIf { it > 0L } ?: 0L

    private data class RootCommandResult(
        val exitCode: Int,
        val output: String,
        val unavailable: Boolean,
    )

    private const val STAT_BEGIN = "__HP_STAT_BEGIN__"
    private const val STAT_END = "__HP_STAT_END__"
    private const val CPU_INFO_BEGIN = "__HP_CPU_INFO_BEGIN__"
    private const val CPU_INFO_END = "__HP_CPU_INFO_END__"
    private const val FREQUENCY_PREFIX = "__HP_FREQ_"
    private const val MIDR_PREFIX = "__HP_MIDR_"
    private const val CAPACITY_PREFIX = "__HP_CAP_"
    private const val MAX_FREQUENCY_PREFIX = "__HP_MAX_FREQ_"
    private const val ROOT_AUTH_TIMEOUT_SECONDS = 30L
    private const val ROOT_READ_TIMEOUT_SECONDS = 8L
    private const val MAX_CORES = 64
}
