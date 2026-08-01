package com.reiraku.hyperpower.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reiraku.hyperpower.notification.LiveNotificationLayout
import com.reiraku.hyperpower.notification.MonitorNotificationPreferences
import com.reiraku.hyperpower.notification.NotificationMetric
import com.reiraku.hyperpower.notification.PowerMonitorService
import com.reiraku.hyperpower.notification.SuperIslandLayout
import com.reiraku.hyperpower.ui.theme.miuixBarColor
import com.reiraku.hyperpower.ui.theme.miuixGaussianBlur
import com.reiraku.hyperpower.ui.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SuperIslandSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var layout by remember {
        mutableStateOf(MonitorNotificationPreferences.getSuperIslandLayout(context))
    }
    val metrics = NotificationMetric.entries
    val items = remember {
        metrics.map { DropdownItem(title = it.label) }
    }

    NotificationSettingsScaffold(
        title = "超级岛设置",
        onBack = onBack,
        modifier = modifier,
    ) {
        SmallTitle(text = "摘要态")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            WindowSpinnerPreference(
                title = "左侧数据",
                summary = layout.left.label,
                items = items,
                selectedIndex = metrics.indexOf(layout.left),
                onSelectedIndexChange = { index ->
                    val selected = metrics[index]
                    layout = if (selected == layout.right) {
                        SuperIslandLayout(left = selected, right = layout.left)
                    } else {
                        layout.copy(left = selected)
                    }
                    MonitorNotificationPreferences.setSuperIslandLayout(context, layout)
                    PowerMonitorService.refresh(context)
                },
            )
            WindowSpinnerPreference(
                title = "右侧数据",
                summary = layout.right.label,
                items = items,
                selectedIndex = metrics.indexOf(layout.right),
                onSelectedIndexChange = { index ->
                    val selected = metrics[index]
                    layout = if (selected == layout.left) {
                        SuperIslandLayout(left = layout.right, right = selected)
                    } else {
                        layout.copy(right = selected)
                    }
                    MonitorNotificationPreferences.setSuperIslandLayout(context, layout)
                    PowerMonitorService.refresh(context)
                },
            )
        }
    }
}

@Composable
fun LiveNotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var layout by remember {
        mutableStateOf(MonitorNotificationPreferences.getLiveNotificationLayout(context))
    }
    val metrics = NotificationMetric.entries
    val items = remember {
        metrics.map { DropdownItem(title = it.label) }
    }

    fun selectMetric(slot: Int, selected: NotificationMetric) {
        val current = listOf(layout.primary, layout.secondary, layout.tertiary).toMutableList()
        val existingIndex = current.indexOf(selected)
        if (existingIndex >= 0 && existingIndex != slot) {
            current[existingIndex] = current[slot]
        }
        current[slot] = selected
        layout = LiveNotificationLayout(
            primary = current[0],
            secondary = current[1],
            tertiary = current[2],
        )
        MonitorNotificationPreferences.setLiveNotificationLayout(context, layout)
        PowerMonitorService.refresh(context)
    }

    NotificationSettingsScaffold(
        title = "实时通知设置",
        onBack = onBack,
        modifier = modifier,
    ) {
        SmallTitle(text = "通知内容")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            MetricSpinner(
                title = "主要数据",
                selected = layout.primary,
                metrics = metrics,
                items = items,
                onSelect = { selectMetric(0, it) },
            )
            MetricSpinner(
                title = "第二行数据",
                selected = layout.secondary,
                metrics = metrics,
                items = items,
                onSelect = { selectMetric(1, it) },
            )
            MetricSpinner(
                title = "第三行数据",
                selected = layout.tertiary,
                metrics = metrics,
                items = items,
                onSelect = { selectMetric(2, it) },
            )
        }
    }
}

@Composable
private fun MetricSpinner(
    title: String,
    selected: NotificationMetric,
    metrics: List<NotificationMetric>,
    items: List<DropdownItem>,
    onSelect: (NotificationMetric) -> Unit,
) {
    WindowSpinnerPreference(
        title = title,
        summary = selected.label,
        items = items,
        selectedIndex = metrics.indexOf(selected),
        onSelectedIndexChange = { index -> onSelect(metrics[index]) },
    )
}

@Composable
private fun NotificationSettingsScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                modifier = Modifier.miuixGaussianBlur(backdrop),
                color = miuixBarColor(backdrop),
                title = title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier)
                .verticalScroll(rememberScrollState())
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
            content = content,
        )
    }
}
