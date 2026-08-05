package com.reiraku.hyperpower.data

enum class CpuCoreRole(val label: String) {
    ULTRA("超大核"),
    PERFORMANCE("性能核"),
    EFFICIENCY("能效核"),
    GENERAL("通用核"),
    UNKNOWN("待识别"),
}

enum class CpuAccessMode(val label: String) {
    SHIZUKU("Shizuku"),
    ROOT("Root"),
}

data class CpuCoreMetric(
    val id: Int,
    val usage: Float? = null,
    val frequencyKhz: Long? = null,
    val architecture: String? = null,
    val role: CpuCoreRole = CpuCoreRole.UNKNOWN,
)

data class CpuMetrics(
    val overallUsage: Float? = null,
    val cores: List<CpuCoreMetric> = emptyList(),
) {
    val coreCount: Int get() = cores.size

    val averageFrequencyKhz: Long?
        get() {
            val available = cores.mapNotNull(CpuCoreMetric::frequencyKhz)
            return available.takeIf { it.isNotEmpty() }?.average()?.toLong()
        }
}

data class CpuLoadSample(
    val timestampMillis: Long,
    val usage: Float,
)

data class ChargePowerSample(
    val timestampMillis: Long,
    val powerWatts: Double,
)

internal fun updateCpuLoadHistory(
    history: List<CpuLoadSample>,
    usage: Float?,
    nowMillis: Long,
): List<CpuLoadSample> {
    val cutoff = nowMillis - CPU_LOAD_HISTORY_WINDOW_MILLIS
    val retained = history.filter { it.timestampMillis >= cutoff }
    val updated = if (usage == null) {
        retained
    } else {
        retained + CpuLoadSample(
            timestampMillis = nowMillis,
            usage = usage.coerceIn(0f, 1f),
        )
    }
    return updated.takeLast(MAX_CPU_LOAD_HISTORY_SAMPLES)
}

internal fun updateChargePowerHistory(
    history: List<ChargePowerSample>,
    wasCharging: Boolean,
    isCharging: Boolean,
    powerWatts: Double?,
    nowMillis: Long,
): List<ChargePowerSample> {
    if (!isCharging) return history

    val sessionHistory = if (wasCharging) history else emptyList()
    val cutoff = nowMillis - CHARGE_POWER_HISTORY_WINDOW_MILLIS
    val retained = sessionHistory.filter { it.timestampMillis >= cutoff }
    val power = powerWatts?.takeIf { it.isFinite() && it >= 0.0 } ?: return retained
    return (retained + ChargePowerSample(nowMillis, power))
        .takeLast(MAX_CHARGE_POWER_HISTORY_SAMPLES)
}

data class BatteryMetrics(
    val levelPercent: Int? = null,
    val voltageMv: Int? = null,
    val currentUa: Long? = null,
    val chargeUah: Long? = null,
    val estimatedRemainingMillis: Long? = null,
    val temperatureCelsius: Float? = null,
    val isCharging: Boolean = false,
    val isPowerConnected: Boolean = false,
) {
    val estimatedPowerWatts: Double?
        get() {
            val current = currentUa ?: return null
            val voltage = voltageMv ?: return null
            return kotlin.math.abs(current.toDouble()) * voltage.toDouble() / 1_000_000_000.0
    }
}

data class MemoryMetrics(
    val totalBytes: Long? = null,
    val availableBytes: Long? = null,
    val lowMemoryThresholdBytes: Long? = null,
    val isLowMemory: Boolean = false,
) {
    val usedBytes: Long?
        get() {
            val total = totalBytes ?: return null
            val available = availableBytes ?: return null
            return (total - available).coerceIn(0L, total)
        }

    val usage: Float?
        get() {
            val total = totalBytes?.takeIf { it > 0L } ?: return null
            val used = usedBytes ?: return null
            return (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
        }
}

enum class CpuAccessStatus {
    CHECKING,
    DIRECT,
    NEEDS_SHIZUKU,
    CONNECTING,
    SHIZUKU,
    SHIZUKU_STOPPED,
    DENIED,
    ROOT_REQUIRED,
    ROOT_CONNECTING,
    ROOT,
    ROOT_DENIED,
    ROOT_UNAVAILABLE,
}

data class DashboardState(
    val cpu: CpuMetrics = CpuMetrics(),
    val cpuLoadHistory: List<CpuLoadSample> = emptyList(),
    val chargePowerHistory: List<ChargePowerSample> = emptyList(),
    val battery: BatteryMetrics = BatteryMetrics(),
    val memory: MemoryMetrics = MemoryMetrics(),
    val cpuAccessMode: CpuAccessMode = CpuAccessMode.SHIZUKU,
    val cpuAccessStatus: CpuAccessStatus = CpuAccessStatus.CHECKING,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

const val CPU_LOAD_HISTORY_WINDOW_MILLIS = 30_000L
const val CHARGE_POWER_HISTORY_WINDOW_MILLIS = 5L * 60L * 1_000L
private const val MAX_CPU_LOAD_HISTORY_SAMPLES = 120
private const val MAX_CHARGE_POWER_HISTORY_SAMPLES = 600
