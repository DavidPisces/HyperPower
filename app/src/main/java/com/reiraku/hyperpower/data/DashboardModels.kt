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
    val battery: BatteryMetrics = BatteryMetrics(),
    val cpuAccessMode: CpuAccessMode = CpuAccessMode.SHIZUKU,
    val cpuAccessStatus: CpuAccessStatus = CpuAccessStatus.CHECKING,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

const val CPU_LOAD_HISTORY_WINDOW_MILLIS = 30_000L
private const val MAX_CPU_LOAD_HISTORY_SAMPLES = 120
