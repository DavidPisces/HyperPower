package com.reiraku.hyperpower.privileged

import android.content.Context
import com.reiraku.hyperpower.data.LinuxCpuReader

class PowerUserService @JvmOverloads constructor(
    @Suppress("UNUSED_PARAMETER") context: Context? = null,
) : IPrivilegedMonitor.Stub() {

    override fun readCpuStat(): String = LinuxCpuReader.readCpuStat()

    override fun readCpuInfo(): String = LinuxCpuReader.readCpuInfo()

    override fun readCoreCount(): Int = LinuxCpuReader.readCoreCount()

    override fun readCoreFrequencies(coreCount: Int): LongArray =
        LinuxCpuReader.readCoreFrequencies(coreCount.coerceIn(1, MAX_CORES))

    override fun readCoreMidrs(coreCount: Int): Array<String> =
        LinuxCpuReader.readCoreMidrs(coreCount.coerceIn(1, MAX_CORES))

    override fun readCoreCapacities(coreCount: Int): LongArray =
        LinuxCpuReader.readCoreCapacities(coreCount.coerceIn(1, MAX_CORES))

    override fun readCoreMaxFrequencies(coreCount: Int): LongArray =
        LinuxCpuReader.readCoreMaxFrequencies(coreCount.coerceIn(1, MAX_CORES))

    private companion object {
        const val MAX_CORES = 64
    }
}
