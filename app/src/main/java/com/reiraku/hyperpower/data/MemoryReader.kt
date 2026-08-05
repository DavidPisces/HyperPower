package com.reiraku.hyperpower.data

import android.app.ActivityManager
import android.content.Context

class MemoryReader(context: Context) {
    private val activityManager = context.applicationContext
        .getSystemService(ActivityManager::class.java)

    fun read(): MemoryMetrics {
        val info = ActivityManager.MemoryInfo()
        return runCatching {
            activityManager.getMemoryInfo(info)
            MemoryMetrics(
                totalBytes = info.totalMem.takeIf { it > 0L },
                availableBytes = info.availMem.takeIf { it >= 0L },
                lowMemoryThresholdBytes = info.threshold.takeIf { it > 0L },
                isLowMemory = info.lowMemory,
            )
        }.getOrDefault(MemoryMetrics())
    }
}
