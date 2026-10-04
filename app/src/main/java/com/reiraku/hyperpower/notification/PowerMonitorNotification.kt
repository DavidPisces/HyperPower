package com.reiraku.hyperpower.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.reiraku.hyperpower.MainActivity
import com.reiraku.hyperpower.R
import com.reiraku.hyperpower.data.DashboardState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** 数值不可用时的占位符，仅用于 content 文字路径。 */
private const val PLACEHOLDER = "—"

/**
 * 实时通知的原始数据快照。
 *
 * 同时服务两条渲染路径：
 * - Android 17（API 37）及以上：转换为 [NotificationCompat.MetricStyle] 的结构化指标，
 *   数值与单位分离，由系统负责排版，无需手工拼接字符串。
 * - Android 16（API 36）及以下：格式化为 content 文字，退化展示。
 */
data class LiveNotificationMetrics(
    val currentUa: Long? = null,
    val voltageMv: Int? = null,
    val powerWatts: Double? = null,
    val cpuPercent: Int? = null,
    val frequencyKhz: Long? = null,
    val batteryLevelPercent: Int? = null,
    val isCharging: Boolean = false,
    val remainingMillis: Long? = null,
    val temperatureCelsius: Float? = null,
) {
    val current: String
        get() = currentUa?.let {
            formatDecimal(it / 1_000.0, 0, includeSign = true) + " mA"
        } ?: PLACEHOLDER

    val voltage: String
        get() = voltageMv?.let { formatDecimal(it / 1_000.0, 2) + " V" } ?: PLACEHOLDER

    val power: String
        get() = powerWatts?.let { formatDecimal(it, 2) + " W" } ?: "$PLACEHOLDER W"

    val cpuLoad: String
        get() = cpuPercent?.let { "$it%" } ?: PLACEHOLDER

    val frequency: String
        get() = formatFrequency(frequencyKhz)

    val batteryLevel: String
        get() = batteryLevelPercent?.let { "$it%" } ?: PLACEHOLDER

    val remainingTime: String
        get() = when {
            isCharging -> "充电中"
            else -> formatDuration(remainingMillis)
        }

    val batteryTemperature: String
        get() = temperatureCelsius?.let { formatDecimal(it.toDouble(), 1) + " °C" } ?: PLACEHOLDER

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

    /**
     * Android 17 的指标样式：按用户选择的顺序取 3 个指标，全部使用结构化数值 + 单位。
     *
     * 无可用数值的指标会被跳过（避免规范禁止的空状态），一个都不剩时返回 null，
     * 由调用方退化为 content 文字展示——`MetricStyle` 必须至少包含一个指标，否则 build 会抛异常。
     */
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    fun buildMetricStyle(layout: LiveNotificationLayout): NotificationCompat.MetricStyle? {
        // distinct：设置页会避免重复选择，但偏好文件被外部改写时可能出现重复项。
        val metrics = listOf(layout.primary, layout.secondary, layout.tertiary)
            .distinct()
            .mapNotNull(::toNotificationMetric)
        if (metrics.isEmpty()) return null
        return NotificationCompat.MetricStyle()
            .setMetrics(metrics)
            .setCriticalMetric(0)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun toNotificationMetric(metric: NotificationMetric): NotificationCompat.Metric? =
        when (metric) {
            NotificationMetric.POWER -> powerWatts?.let {
                metricOf(NotificationCompat.Metric.FixedFloat(it.toFloat(), "W", 2, 2), metric)
            }

            NotificationMetric.CURRENT -> currentUa?.let {
                metricOf(
                    NotificationCompat.Metric.FixedInt((it / 1_000L).toInt(), "mA"),
                    metric,
                )
            }

            NotificationMetric.VOLTAGE -> voltageMv?.let {
                metricOf(
                    NotificationCompat.Metric.FixedFloat(it / 1_000f, "V", 2, 2),
                    metric,
                )
            }

            NotificationMetric.CPU_LOAD -> cpuPercent?.let {
                metricOf(NotificationCompat.Metric.FixedInt(it, "%"), metric)
            }

            NotificationMetric.CPU_FREQUENCY -> frequencyKhz?.let { khz ->
                if (khz >= 1_000_000L) {
                    metricOf(
                        NotificationCompat.Metric.FixedFloat(
                            khz / 1_000_000f,
                            "GHz",
                            2,
                            2,
                        ),
                        metric,
                    )
                } else {
                    metricOf(
                        NotificationCompat.Metric.FixedInt((khz / 1_000L).toInt(), "MHz"),
                        metric,
                    )
                }
            }

            NotificationMetric.BATTERY_LEVEL -> batteryLevelPercent?.let {
                metricOf(NotificationCompat.Metric.FixedInt(it, "%"), metric)
            }

            NotificationMetric.REMAINING_TIME -> {
                val text = when {
                    isCharging -> "充电中"
                    remainingMillis != null -> formatDuration(remainingMillis)
                    else -> null
                }
                text?.let { metricOf(NotificationCompat.Metric.FixedText(it), metric) }
            }

            NotificationMetric.BATTERY_TEMPERATURE -> temperatureCelsius?.let {
                metricOf(NotificationCompat.Metric.FixedFloat(it, "°C", 1, 1), metric)
            }
        }
}

@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
private fun metricOf(
    value: NotificationCompat.Metric.MetricValue,
    metric: NotificationMetric,
): NotificationCompat.Metric = NotificationCompat.Metric(value, metric.shortLabel)

private fun formatFrequency(frequencyKhz: Long?): String = when {
    frequencyKhz == null -> PLACEHOLDER
    frequencyKhz >= 1_000_000L ->
        formatDecimal(frequencyKhz / 1_000_000.0, 2) + " GHz"

    else ->
        formatDecimal(frequencyKhz / 1_000.0, 0) + " MHz"
}

private fun formatDuration(durationMillis: Long?): String {
    durationMillis ?: return PLACEHOLDER
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
        // 标题固定为应用名：主数据由正文首行（或 MetricStyle 的首个指标）承担，
        // 标题再写一遍主数据会与正文/MetricStyle 重复。
        val title = context.getString(R.string.app_name)
        val firstLine = metrics.labeledValue(liveLayout.primary)
        val secondLine = metrics.labeledValue(liveLayout.secondary)
        val thirdLine = metrics.labeledValue(liveLayout.tertiary)
        val bodyLine = if (useSuperIsland) {
            "${metrics.labeledValue(islandLayout.left)} · " +
                metrics.labeledValue(islandLayout.right)
        } else {
            "$firstLine · $secondLine"
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_hyperpower)
            .setContentTitle(title)
            .setContentText(bodyLine)
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
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)

            // Android 17（API 37）：MetricStyle 结构化指标展示，可在设置中关闭。
            // 关闭、或指标全部无数据时返回 null，自动落回下面的文字路径。
            val metricStyle = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
                MonitorNotificationPreferences.isMetricStyleEnabled(context)
            ) {
                metrics.buildMetricStyle(liveLayout)
            } else {
                null
            }

            if (metricStyle != null) {
                // MetricStyle 不显示上下文文本，指标单位由系统按展开/收起状态处理。
                builder.setStyle(metricStyle)
            } else {
                // Android 16（API 36）及以下、或用户关闭 MetricStyle：退化为 content 文字展示。
                //
                // 不设置 subText：它渲染在标题行（AOSP 的 header_text，与 app 名同一行），
                // 而展开态仍会保留该行，导致同一份数据在标题行与 bigText 里同屏出现两次。
                // 收起态只看 contentText、展开态只看 bigText，两者互斥，不会重复。
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("$firstLine\n$secondLine\n$thirdLine"),
                )
            }
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
            currentUa = currentUa,
            voltageMv = voltageMv,
            powerWatts = powerWatts,
            cpuPercent = cpuPercent,
            frequencyKhz = cpu.averageFrequencyKhz,
            batteryLevelPercent = battery.levelPercent,
            isCharging = battery.isCharging,
            remainingMillis = battery.estimatedRemainingMillis,
            temperatureCelsius = battery.temperatureCelsius,
        )
    }
}
