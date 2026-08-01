package com.reiraku.hyperpower.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.reiraku.hyperpower.R
import org.json.JSONObject

object SuperIslandHelper {
    private const val BUSINESS_ID = "hyperpower_monitor"
    private const val TIMEOUT_MINUTES = 60
    private const val KEY_FOCUS_PARAM = "miui.focus.param"
    private const val KEY_FOCUS_PICS = "miui.focus.pics"
    private const val KEY_FOCUS_ACTIONS = "miui.focus.actions"
    private const val PIC_APP_ICON = "miui.focus.pic_app_icon"
    private const val ACTION_OPEN = "miui.focus.action_open"
    private const val ACTION_CLOSE = "miui.focus.action_close"

    @Volatile
    private var cachedSupport: Boolean? = null

    @Synchronized
    fun isSupported(context: Context, refresh: Boolean = false): Boolean {
        if (!refresh) cachedSupport?.let { return it }
        return (
            isIslandFeatureEnabled() &&
                getFocusProtocolVersion(context) >= 3 &&
                hasFocusPermission(context)
            ).also { cachedSupport = it }
    }

    @SuppressLint("PrivateApi")
    private fun isIslandFeatureEnabled(): Boolean = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod(
            "getBoolean",
            String::class.java,
            Boolean::class.javaPrimitiveType,
        )
        method.invoke(null, "persist.sys.feature.island", false) as? Boolean ?: false
    } catch (_: Exception) {
        false
    }

    private fun getFocusProtocolVersion(context: Context): Int = try {
        android.provider.Settings.System.getInt(
            context.contentResolver,
            "notification_focus_protocol",
            0,
        )
    } catch (_: Exception) {
        0
    }

    private fun hasFocusPermission(context: Context): Boolean = try {
        val uri = "content://miui.statusbar.notification.public".toUri()
        val extras = Bundle().apply {
            putString("package", context.packageName)
        }
        context.contentResolver.call(uri, "canShowFocus", null, extras)
            ?.getBoolean("canShowFocus", false) ?: false
    } catch (_: Exception) {
        false
    }

    fun buildMetricsParams(
        metrics: LiveNotificationMetrics,
        layout: SuperIslandLayout,
    ): String {
        val highlightColor = "#64B5F6"
        val ticker = "${metrics.labeledValue(NotificationMetric.POWER)} · " +
            metrics.labeledValue(NotificationMetric.CPU_LOAD)
        val paramV2 = JSONObject().apply {
            put("protocol", 1)
            put("business", BUSINESS_ID)
            put("islandFirstFloat", true)
            put("enableFloat", false)
            put("timeout", TIMEOUT_MINUTES)
            put("updatable", true)
            put("reopen", "reopen")
            put("filterWhenNoPermission", false)
            put("ticker", ticker)
            put("tickerPic", PIC_APP_ICON)
            put("aodTitle", ticker)
            put("aodPic", PIC_APP_ICON)
            put("param_island", JSONObject().apply {
                put("islandProperty", 1)
                put("islandTimeout", TIMEOUT_MINUTES * 60)
                put("dismissIsland", false)
                put("highlightColor", highlightColor)
                put("bigIslandArea", buildBigIslandArea(metrics, layout))
                put("smallIslandArea", buildSmallIslandArea())
            })
            put("baseInfo", buildTemplate2Text(metrics, highlightColor))
            put("picInfo", buildRecognitionGraphic())
            put("hintInfo", buildButtonHint(metrics))
        }
        return JSONObject().apply {
            put("param_v2", paramV2)
        }.toString()
    }

    /**
     * 展开态模板 2：文本组件 2。
     *
     * 展开态固定展示功耗、CPU、电池和续航信息，并启用两组文本分隔符；
     * 摘要态的左右自定义不会改变这里的内容。
     */
    private fun buildTemplate2Text(
        metrics: LiveNotificationMetrics,
        highlightColor: String,
    ) = JSONObject().apply {
        put("type", 2)
        put("title", metrics.compactValue(NotificationMetric.POWER))
        put("subTitle", "CPU ${metrics.compactValue(NotificationMetric.CPU_LOAD)}")
        put("extraTitle", metrics.compactValue(NotificationMetric.CPU_FREQUENCY))
        put("specialTitle", metrics.compactValue(NotificationMetric.BATTERY_LEVEL))
        put(
            "content",
            "电流 ${metrics.value(NotificationMetric.CURRENT)} · " +
                "电压 ${metrics.value(NotificationMetric.VOLTAGE)}",
        )
        put("colorTitle", highlightColor)
        put("colorTitleDark", highlightColor)
        put("showDivider", true)
        put("showContentDivider", true)
    }

    /** 按钮组件 2：承载次级数据，并提供关闭实时通知操作。 */
    private fun buildButtonHint(metrics: LiveNotificationMetrics) = JSONObject().apply {
        put("type", 2)
        put("title", metrics.compactValue(NotificationMetric.REMAINING_TIME))
        put("subTitle", metrics.compactValue(NotificationMetric.BATTERY_TEMPERATURE))
        put("content", "预计使用")
        put("subContent", "电池温度")
        put("actionInfo", JSONObject().apply {
            put("action", ACTION_CLOSE)
            put("clickWithCollapse", true)
        })
    }

    /** 展开态模板 2：识别图形组件 1。 */
    private fun buildRecognitionGraphic() = JSONObject().apply {
        put("type", 1)
        put("pic", PIC_APP_ICON)
        put("actionInfo", JSONObject().apply {
            put("action", ACTION_OPEN)
            put("clickWithCollapse", true)
        })
    }

    /**
     * 摘要态使用“图文组件 1 + 文本组件”，左右数据由用户选择。
     */
    private fun buildBigIslandArea(
        metrics: LiveNotificationMetrics,
        layout: SuperIslandLayout,
    ) = JSONObject().apply {
        put("imageTextInfoLeft", JSONObject().apply {
            put("type", 1)
            put("picInfo", JSONObject().apply {
                put("type", 1)
                put("pic", PIC_APP_ICON)
            })
            put("textInfo", JSONObject().apply {
                put("title", metrics.compactValue(layout.left))
                put("showHighlightColor", false)
            })
        })
        put("textInfo", JSONObject().apply {
            put("frontTitle", layout.right.shortLabel)
            put("title", metrics.compactValue(layout.right))
            put("showHighlightColor", false)
        })
    }

    /** 小岛按规范只提供正方形应用图标。 */
    private fun buildSmallIslandArea() = JSONObject().apply {
        put("picInfo", JSONObject().apply {
            put("type", 1)
            put("pic", PIC_APP_ICON)
        })
    }

    fun applyToNotification(
        context: Context,
        builder: NotificationCompat.Builder,
        islandParams: String,
        openIntent: PendingIntent,
        closeIntent: PendingIntent,
    ) {
        val appIcon = Icon.createWithResource(context, R.mipmap.ic_launcher_round)
        val closeIcon = Icon.createWithResource(context, R.drawable.ic_close)
        builder.addExtras(
            Bundle().apply {
                putString(KEY_FOCUS_PARAM, islandParams)
                putBundle(
                    KEY_FOCUS_PICS,
                    Bundle().apply {
                        putParcelable(PIC_APP_ICON, appIcon)
                    },
                )
                putBundle(
                    KEY_FOCUS_ACTIONS,
                    Bundle().apply {
                        putParcelable(
                            ACTION_OPEN,
                            Notification.Action.Builder(
                                appIcon,
                                "查看",
                                openIntent,
                            ).build(),
                        )
                        putParcelable(
                            ACTION_CLOSE,
                            Notification.Action.Builder(
                                closeIcon,
                                "关闭",
                                closeIntent,
                            ).build(),
                        )
                    },
                )
            },
        )
    }
}
