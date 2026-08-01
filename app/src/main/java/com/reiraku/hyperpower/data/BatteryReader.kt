package com.reiraku.hyperpower.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlin.math.abs

class BatteryReader(context: Context) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(BatteryManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)

    fun read(): BatteryMetrics {
        val intent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        ) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0

        val currentNow = readProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val averageCurrent = readProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val charge = readProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatteryMetrics(
            levelPercent = if (level >= 0 && scale > 0) {
                (level * 100f / scale).toInt().coerceIn(0, 100)
            } else {
                null
            },
            voltageMv = intent
                ?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                ?.takeIf { it > 0 },
            currentUa = currentNow,
            chargeUah = charge,
            estimatedRemainingMillis = if (isCharging) {
                null
            } else {
                readSystemPrediction() ?: estimateFromFuelGauge(
                    chargeUah = charge,
                    currentUa = averageCurrent ?: currentNow,
                )
            },
            temperatureCelsius = intent
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?.takeIf { it != Int.MIN_VALUE }
                ?.div(10f),
            isCharging = isCharging,
            isPowerConnected = plugged != 0,
        )
    }

    private fun readProperty(property: Int): Long? {
        return runCatching { batteryManager.getLongProperty(property) }
            .getOrNull()
            ?.takeUnless { it == Long.MIN_VALUE }
    }

    private fun readSystemPrediction(): Long? {
        return runCatching { powerManager.batteryDischargePrediction?.toMillis() }
            .getOrNull()
            ?.takeIf { it > 0L }
    }

    private fun estimateFromFuelGauge(chargeUah: Long?, currentUa: Long?): Long? {
        val charge = chargeUah?.takeIf { it > 0L } ?: return null
        val current = currentUa?.let(::abs)?.takeIf { it >= 1_000L } ?: return null
        val hours = charge.toDouble() / current.toDouble()
        return (hours * 60.0 * 60.0 * 1_000.0)
            .toLong()
            .takeIf { it in 1L..MAX_REASONABLE_ESTIMATE_MILLIS }
    }

    private companion object {
        const val MAX_REASONABLE_ESTIMATE_MILLIS = 10L * 24L * 60L * 60L * 1_000L
    }
}
