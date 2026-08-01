package com.reiraku.hyperpower.notification

import android.content.Context
import androidx.core.content.edit

enum class MonitorNotificationMode(
    val storedValue: String,
    val label: String,
) {
    NATIVE("native", "实时通知"),
    SUPER_ISLAND("super_island", "小米超级岛"),
}

enum class NotificationMetric(
    val storedValue: String,
    val label: String,
    val shortLabel: String = label,
) {
    POWER("power", "实时功耗", "功耗"),
    CURRENT("current", "当前电流", "电流"),
    VOLTAGE("voltage", "电池电压", "电压"),
    CPU_LOAD("cpu_load", "CPU 负载", "CPU"),
    CPU_FREQUENCY("cpu_frequency", "CPU 频率", "频率"),
    BATTERY_LEVEL("battery_level", "剩余电量", "电量"),
    REMAINING_TIME("remaining_time", "预计使用时间", "续航"),
    BATTERY_TEMPERATURE("battery_temperature", "电池温度", "温度"),
    ;

    companion object {
        fun fromStoredValue(value: String?, fallback: NotificationMetric): NotificationMetric =
            entries.firstOrNull { it.storedValue == value } ?: fallback
    }
}

data class SuperIslandLayout(
    val left: NotificationMetric = NotificationMetric.POWER,
    val right: NotificationMetric = NotificationMetric.CPU_LOAD,
)

data class LiveNotificationLayout(
    val primary: NotificationMetric = NotificationMetric.POWER,
    val secondary: NotificationMetric = NotificationMetric.CURRENT,
    val tertiary: NotificationMetric = NotificationMetric.CPU_FREQUENCY,
)

object MonitorNotificationPreferences {
    private const val PREFERENCES_NAME = "monitor_notification_preferences"
    private const val ENABLED_KEY = "enabled"
    private const val MODE_KEY = "mode"
    private const val PERMISSION_PROMPTED_KEY = "permission_prompted"
    private const val ISLAND_LEFT_METRIC_KEY = "island_left_metric"
    private const val ISLAND_RIGHT_METRIC_KEY = "island_right_metric"
    private const val LIVE_PRIMARY_METRIC_KEY = "live_primary_metric"
    private const val LIVE_SECONDARY_METRIC_KEY = "live_secondary_metric"
    private const val LIVE_TERTIARY_METRIC_KEY = "live_tertiary_metric"

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(ENABLED_KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean(ENABLED_KEY, enabled) }
    }

    fun getMode(context: Context): MonitorNotificationMode {
        val storedValue = preferences(context).getString(
            MODE_KEY,
            MonitorNotificationMode.NATIVE.storedValue,
        )
        return MonitorNotificationMode.entries.firstOrNull {
            it.storedValue == storedValue
        } ?: MonitorNotificationMode.NATIVE
    }

    fun setMode(context: Context, mode: MonitorNotificationMode) {
        preferences(context).edit { putString(MODE_KEY, mode.storedValue) }
    }

    fun getSuperIslandLayout(context: Context): SuperIslandLayout {
        val preferences = preferences(context)
        return SuperIslandLayout(
            left = NotificationMetric.fromStoredValue(
                preferences.getString(ISLAND_LEFT_METRIC_KEY, null),
                NotificationMetric.POWER,
            ),
            right = NotificationMetric.fromStoredValue(
                preferences.getString(ISLAND_RIGHT_METRIC_KEY, null),
                NotificationMetric.CPU_LOAD,
            ),
        )
    }

    fun setSuperIslandLayout(context: Context, layout: SuperIslandLayout) {
        preferences(context).edit {
            putString(ISLAND_LEFT_METRIC_KEY, layout.left.storedValue)
            putString(ISLAND_RIGHT_METRIC_KEY, layout.right.storedValue)
        }
    }

    fun getLiveNotificationLayout(context: Context): LiveNotificationLayout {
        val preferences = preferences(context)
        return LiveNotificationLayout(
            primary = NotificationMetric.fromStoredValue(
                preferences.getString(LIVE_PRIMARY_METRIC_KEY, null),
                NotificationMetric.POWER,
            ),
            secondary = NotificationMetric.fromStoredValue(
                preferences.getString(LIVE_SECONDARY_METRIC_KEY, null),
                NotificationMetric.CURRENT,
            ),
            tertiary = NotificationMetric.fromStoredValue(
                preferences.getString(LIVE_TERTIARY_METRIC_KEY, null),
                NotificationMetric.CPU_FREQUENCY,
            ),
        )
    }

    fun setLiveNotificationLayout(context: Context, layout: LiveNotificationLayout) {
        preferences(context).edit {
            putString(LIVE_PRIMARY_METRIC_KEY, layout.primary.storedValue)
            putString(LIVE_SECONDARY_METRIC_KEY, layout.secondary.storedValue)
            putString(LIVE_TERTIARY_METRIC_KEY, layout.tertiary.storedValue)
        }
    }

    fun wasPermissionPrompted(context: Context): Boolean =
        preferences(context).getBoolean(PERMISSION_PROMPTED_KEY, false)

    fun markPermissionPrompted(context: Context) {
        preferences(context).edit { putBoolean(PERMISSION_PROMPTED_KEY, true) }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
}
