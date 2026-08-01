package com.reiraku.hyperpower.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reiraku.hyperpower.MainActivity
import com.reiraku.hyperpower.R
import com.reiraku.hyperpower.data.DashboardState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class LiveNotificationMetrics(
    val current: String,
    val voltage: String,
    val power: String,
    val cpuLoad: String,
    val frequency: String,
    val batteryLevel: String,
    val remainingTime: String,
    val batteryTemperature: String,
) {
    fun value(metric: NotificationMetric): String = when (metric) {
        NotificationMetric.POWER -> power
        NotificationMetric.CURRENT -> current
        NotificationMetric.VOLTAGE -> voltage
        NotificationMetric.CPU_LOAD -> cpuLoad
        NotificationMetric.CPU_FREQUENCY -> frequency
        NotificationMetric.BATTERY_LEVEL -> batteryLevel
        NotificationMetric.REMAINING_TIME -> remainingTime
        NotificationMetric.BATTERY_TEMPERATURE -> batteryTemperature
    }

    fun compactValue(metric: NotificationMetric): String = value(metric)
        .replace(" ", "")
        .replace("GHz", "G")
        .replace("MHz", "M")
        .replace("小时", "h")
        .replace("分钟", "m")
        .replace("分", "m")
        .replace("°C", "°")

    fun labeledValue(metric: NotificationMetric): String =
        "${metric.shortLabel} ${value(metric)}"
}

object PowerMonitorNotification {
    const val NOTIFICATION_ID = 20260731
    // 使用新 ID，避免已经创建的 DEFAULT 通道无法被应用提升到 HIGH。
    private const val NATIVE_CHANNEL_ID = "live_power_monitor_high_v2"
    private const val SUPER_ISLAND_CHANNEL_ID = "live_power_monitor_island_v1"
    private const val CHANNEL_NAME = "实时功耗监控"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(
                NATIVE_CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ),
            NotificationChannel(
                SUPER_ISLAND_CHANNEL_ID,
                "小米超级岛",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        ).forEach { channel ->
            channel.apply {
                description = "持续显示设备功耗、电池和 CPU 实时数据"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun build(
        context: Context,
        state: DashboardState,
    ): Notification {
        val metrics = state.toLiveNotificationMetrics()
        val useSuperIsland =
            MonitorNotificationPreferences.getMode(context) ==
            MonitorNotificationMode.SUPER_ISLAND &&
                SuperIslandHelper.isSupported(context)
        val islandLayout = MonitorNotificationPreferences.getSuperIslandLayout(context)
        val liveLayout = MonitorNotificationPreferences.getLiveNotificationLayout(context)
        val channelId = if (useSuperIsland) {
            SUPER_ISLAND_CHANNEL_ID
        } else {
            NATIVE_CHANNEL_ID
        }
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (useSuperIsland) {
            "${metrics.labeledValue(islandLayout.left)} · " +
                metrics.labeledValue(islandLayout.right)
        } else {
            metrics.labeledValue(liveLayout.primary)
        }
        val secondLine = if (useSuperIsland) {
            metrics.labeledValue(islandLayout.right)
        } else {
            metrics.labeledValue(liveLayout.secondary)
        }
        val thirdLine = metrics.labeledValue(liveLayout.tertiary)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_hyperpower)
            .setContentTitle(title)
            .setContentText(secondLine)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (useSuperIsland) {
            SuperIslandHelper.applyToNotification(
                context = context,
                builder = builder,
                islandParams = SuperIslandHelper.buildMetricsParams(metrics, islandLayout),
                openIntent = openIntent,
                closeIntent = PowerMonitorService.createStopPendingIntent(context),
            )
        } else {
            builder
                .setSubText(thirdLine)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("$secondLine\n$thirdLine"),
                )
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
        }

        if (Build.VERSION.SDK_INT >= 36 && !useSuperIsland) {
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(
                    metrics.compactValue(liveLayout.primary).take(7),
                )
        }
        return builder.build()
    }

    private fun DashboardState.toLiveNotificationMetrics(): LiveNotificationMetrics {
        val currentUa = battery.currentUa
        val voltageMv = battery.voltageMv
        val powerWatts = if (currentUa != null && voltageMv != null) {
            abs(currentUa.toDouble()) * voltageMv.toDouble() / 1_000_000_000.0
        } else {
            null
        }
        val cpuPercent = cpu.overallUsage
            ?.times(100)
            ?.roundToInt()
            ?.coerceIn(0, 100)

        return LiveNotificationMetrics(
            current = currentUa?.let {
                formatDecimal(it / 1_000.0, 0, includeSign = true) + " mA"
            } ?: "—",
            voltage = voltageMv?.let {
                formatDecimal(it / 1_000.0, 2) + " V"
            } ?: "—",
            power = powerWatts?.let {
                formatDecimal(it, 2) + " W"
            } ?: "— W",
            cpuLoad = cpuPercent?.let { "$it%" } ?: "—",
            frequency = formatFrequency(cpu.averageFrequencyKhz),
            batteryLevel = battery.levelPercent?.let { "$it%" } ?: "—",
            remainingTime = when {
                battery.isCharging -> "充电中"
                else -> formatDuration(battery.estimatedRemainingMillis)
            },
            batteryTemperature = battery.temperatureCelsius?.let {
                formatDecimal(it.toDouble(), 1) + " °C"
            } ?: "—",
        )
    }

    private fun formatFrequency(frequencyKhz: Long?): String = when {
        frequencyKhz == null -> "—"
        frequencyKhz >= 1_000_000L ->
            formatDecimal(frequencyKhz / 1_000_000.0, 2) + " GHz"
        else ->
            formatDecimal(frequencyKhz / 1_000.0, 0) + " MHz"
    }

    private fun formatDuration(durationMillis: Long?): String {
        durationMillis ?: return "—"
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours 小时 $minutes 分钟"
            hours > 0L -> "$hours 小时"
            else -> "$minutes 分钟"
        }
    }

    private fun formatDecimal(
        value: Double,
        decimals: Int,
        includeSign: Boolean = false,
    ): String {
        val pattern = if (includeSign) "%+.$decimals" + "f" else "%.$decimals" + "f"
        return String.format(Locale.getDefault(), pattern, value)
    }
}
