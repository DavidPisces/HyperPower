package com.reiraku.hyperpower.notification

import androidx.core.app.NotificationCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MetricStyle 是 Android 17 才落地的样式，这里断言的是"指标映射"本身，与运行设备的
 * SDK_INT 无关（低版本上 MetricStyle 会被 NotificationCompat 忽略，由调用方退化到文字）。
 */
class LiveNotificationMetricsTest {
    private val fullMetrics = LiveNotificationMetrics(
        currentUa = -1_234_000L,
        voltageMv = 4_200,
        powerWatts = 5.18,
        cpuPercent = 47,
        frequencyKhz = 2_400_000L,
        batteryLevelPercent = 82,
        isCharging = false,
        remainingMillis = 3 * 60 * 60 * 1_000L + 20 * 60 * 1_000L,
        temperatureCelsius = 38.5f,
    )

    /** 只让频率槽位有数据，便于断言单位切换。 */
    private val frequencyOnly = LiveNotificationLayout(
        primary = NotificationMetric.CPU_FREQUENCY,
        secondary = NotificationMetric.REMAINING_TIME,
        tertiary = NotificationMetric.BATTERY_TEMPERATURE,
    )

    @Test
    fun `selected metrics become structured metrics with label and unit`() {
        val style = fullMetrics.buildMetricStyle(
            LiveNotificationLayout(
                primary = NotificationMetric.POWER,
                secondary = NotificationMetric.CPU_LOAD,
                tertiary = NotificationMetric.BATTERY_TEMPERATURE,
            ),
        )

        val metrics = requireNotNull(style).metrics
        assertEquals(listOf("功耗", "CPU", "温度"), metrics.map { it.label.toString() })

        val power = metrics[0].value as NotificationCompat.Metric.FixedFloat
        assertEquals(5.18f, power.value, 0.0001f)
        assertEquals("W", power.unit.toString())

        val cpu = metrics[1].value as NotificationCompat.Metric.FixedInt
        assertEquals(47, cpu.value)
        assertEquals("%", cpu.unit.toString())

        val temperature = metrics[2].value as NotificationCompat.Metric.FixedFloat
        assertEquals(38.5f, temperature.value, 0.0001f)
        assertEquals("°C", temperature.unit.toString())
    }

    @Test
    fun `the first available metric is the critical one`() {
        val style = requireNotNull(
            fullMetrics.buildMetricStyle(
                LiveNotificationLayout(
                    primary = NotificationMetric.POWER,
                    secondary = NotificationMetric.CURRENT,
                    tertiary = NotificationMetric.VOLTAGE,
                ),
            ),
        )

        assertEquals(style.metrics.first(), style.criticalMetric)
    }

    @Test
    fun `metrics without data are skipped instead of showing placeholders`() {
        val partial = LiveNotificationMetrics(cpuPercent = 12)

        val style = requireNotNull(
            partial.buildMetricStyle(
                LiveNotificationLayout(
                    primary = NotificationMetric.POWER,
                    secondary = NotificationMetric.CPU_LOAD,
                    tertiary = NotificationMetric.VOLTAGE,
                ),
            ),
        )

        assertEquals(1, style.metrics.size)
        assertEquals("CPU", style.metrics.single().label.toString())
    }

    @Test
    fun `a notification without any data yields no style so callers fall back to text`() {
        assertNull(LiveNotificationMetrics().buildMetricStyle(LiveNotificationLayout()))
    }

    @Test
    fun `frequency switches unit at one gigahertz`() {
        val gigahertzValue = requireNotNull(
            LiveNotificationMetrics(frequencyKhz = 2_400_000L).buildMetricStyle(frequencyOnly),
        ).metrics.single().value as NotificationCompat.Metric.FixedFloat
        assertEquals(2.4f, gigahertzValue.value, 0.0001f)
        assertEquals("GHz", gigahertzValue.unit.toString())

        val megahertzValue = requireNotNull(
            LiveNotificationMetrics(frequencyKhz = 800_000L).buildMetricStyle(frequencyOnly),
        ).metrics.single().value as NotificationCompat.Metric.FixedInt
        assertEquals(800, megahertzValue.value)
        assertEquals("MHz", megahertzValue.unit.toString())
    }

    @Test
    fun `duplicate selections collapse into a single metric`() {
        val style = requireNotNull(
            LiveNotificationMetrics(powerWatts = 5.18).buildMetricStyle(
                LiveNotificationLayout(
                    primary = NotificationMetric.POWER,
                    secondary = NotificationMetric.POWER,
                    tertiary = NotificationMetric.POWER,
                ),
            ),
        )

        assertEquals(1, style.metrics.size)
    }

    @Test
    fun `charging remaining time is shown as text`() {
        val charging = LiveNotificationMetrics(isCharging = true, remainingMillis = 600_000L)

        val value = requireNotNull(
            charging.buildMetricStyle(
                LiveNotificationLayout(primary = NotificationMetric.REMAINING_TIME),
            ),
        ).metrics.single().value as NotificationCompat.Metric.FixedText

        assertEquals("充电中", value.value.toString())
    }

    @Test
    fun `content text keeps the previous formatting`() {
        assertEquals("功耗 5.18 W", fullMetrics.labeledValue(NotificationMetric.POWER))
        assertEquals("电流 -1234 mA", fullMetrics.labeledValue(NotificationMetric.CURRENT))
        assertEquals("电压 4.20 V", fullMetrics.labeledValue(NotificationMetric.VOLTAGE))
        assertEquals("CPU 47%", fullMetrics.labeledValue(NotificationMetric.CPU_LOAD))
        assertEquals("频率 2.40 GHz", fullMetrics.labeledValue(NotificationMetric.CPU_FREQUENCY))
        assertEquals("电量 82%", fullMetrics.labeledValue(NotificationMetric.BATTERY_LEVEL))
        assertEquals(
            "续航 3 小时 20 分钟",
            fullMetrics.labeledValue(NotificationMetric.REMAINING_TIME),
        )
        assertEquals("温度 38.5 °C", fullMetrics.labeledValue(NotificationMetric.BATTERY_TEMPERATURE))
    }

    @Test
    fun `compact values drop spaces and shorten units for the status chip`() {
        assertEquals("5.18W", fullMetrics.compactValue(NotificationMetric.POWER))
        assertEquals("2.40G", fullMetrics.compactValue(NotificationMetric.CPU_FREQUENCY))
        assertEquals("3h20m", fullMetrics.compactValue(NotificationMetric.REMAINING_TIME))
    }
}
