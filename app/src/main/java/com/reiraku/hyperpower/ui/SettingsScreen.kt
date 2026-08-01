package com.reiraku.hyperpower.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reiraku.hyperpower.data.CpuAccessMode
import com.reiraku.hyperpower.data.CpuAccessStatus
import com.reiraku.hyperpower.data.DashboardState
import com.reiraku.hyperpower.notification.MonitorNotificationMode
import com.reiraku.hyperpower.notification.MonitorNotificationPreferences
import com.reiraku.hyperpower.notification.PowerMonitorService
import com.reiraku.hyperpower.notification.SuperIslandHelper
import com.reiraku.hyperpower.ui.theme.HyperAmber
import com.reiraku.hyperpower.ui.theme.HyperCyan
import com.reiraku.hyperpower.ui.theme.HyperGreen
import com.reiraku.hyperpower.ui.theme.HyperOnSurfaceMuted
import com.reiraku.hyperpower.ui.theme.HyperRed
import com.reiraku.hyperpower.ui.theme.LocalThemeState
import com.reiraku.hyperpower.ui.theme.ThemeMode
import com.reiraku.hyperpower.ui.theme.ThemePreferences
import com.reiraku.hyperpower.ui.theme.miuixBarColor
import com.reiraku.hyperpower.ui.theme.miuixGaussianBlur
import com.reiraku.hyperpower.ui.theme.rememberMiuixBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    state: DashboardState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onBack: () -> Unit,
    onOpenSuperIslandSettings: () -> Unit,
    onOpenLiveNotificationSettings: () -> Unit,
    onSelectMode: (CpuAccessMode) -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestRoot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val themeState = LocalThemeState.current
    val currentTheme = themeState.value
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop()
    var notificationsEnabled by remember {
        mutableStateOf(MonitorNotificationPreferences.isEnabled(context))
    }
    var notificationMode by remember {
        mutableStateOf(MonitorNotificationPreferences.getMode(context))
    }
    var superIslandSupported by remember { mutableStateOf(false) }
    var superIslandSupportChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        superIslandSupported = withContext(Dispatchers.IO) {
            SuperIslandHelper.isSupported(context, refresh = true)
        }
        superIslandSupportChecked = true
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                modifier = Modifier.miuixGaussianBlur(backdrop),
                color = miuixBarColor(backdrop),
                title = "设置",
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
        ) {
            SmallTitle(text = "外观")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                val themeModes = ThemeMode.entries
                val themeItems = remember {
                    themeModes.map { DropdownItem(title = it.label) }
                }
                val selectedThemeIndex = themeModes.indexOf(currentTheme.mode).coerceAtLeast(0)

                WindowSpinnerPreference(
                    title = "显示模式",
                    summary = themeModes[selectedThemeIndex].label,
                    items = themeItems,
                    selectedIndex = selectedThemeIndex,
                    onSelectedIndexChange = { index ->
                        val mode = themeModes[index]
                        ThemePreferences.saveMode(context, mode)
                        themeState.value = currentTheme.copy(mode = mode)
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SmallTitle(text = "实时状态")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                BasicComponent(
                    title = "实时监控通知",
                    summary = if (notificationPermissionGranted) {
                        "持续显示电流、电压、功耗、CPU 负载和平均频率"
                    } else {
                        "需要先授予通知权限"
                    },
                    endActions = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                MonitorNotificationPreferences.setEnabled(context, enabled)
                                if (enabled) {
                                    if (notificationPermissionGranted) {
                                        PowerMonitorService.sync(context)
                                    } else {
                                        onRequestNotificationPermission()
                                    }
                                } else {
                                    PowerMonitorService.stop(context)
                                }
                            },
                        )
                    },
                )

                val notificationModes = if (superIslandSupported) {
                    MonitorNotificationMode.entries
                } else {
                    listOf(MonitorNotificationMode.NATIVE)
                }
                val notificationItems = remember(superIslandSupported) {
                    notificationModes.map { DropdownItem(title = it.label) }
                }
                val selectedNotificationIndex = notificationModes
                    .indexOf(notificationMode)
                    .coerceAtLeast(0)

                if (superIslandSupported) {
                    WindowSpinnerPreference(
                        title = "通知样式",
                        summary = notificationModes[selectedNotificationIndex].label,
                        items = notificationItems,
                        selectedIndex = selectedNotificationIndex,
                        onSelectedIndexChange = { index ->
                            notificationMode = notificationModes[index]
                            MonitorNotificationPreferences.setMode(context, notificationMode)
                            PowerMonitorService.refresh(context)
                            Toast.makeText(
                                context,
                                "已切换为${notificationMode.label}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    )
                } else {
                    BasicComponent(
                        title = "通知样式",
                        summary = when {
                            !superIslandSupportChecked -> "正在检测小米超级岛支持情况…"
                            else -> "实时通知 · 当前设备或权限不支持小米超级岛"
                        },
                    )
                }

                if (
                    superIslandSupported &&
                    notificationMode == MonitorNotificationMode.SUPER_ISLAND
                ) {
                    BasicComponent(
                        title = "超级岛设置",
                        summary = "自定义摘要态左右数据",
                        onClick = onOpenSuperIslandSettings,
                    )
                }

                BasicComponent(
                    title = "实时通知设置",
                    summary = "自定义通知显示的数据",
                    onClick = onOpenLiveNotificationSettings,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SmallTitle(text = "CPU 采集")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                val accessModes = CpuAccessMode.entries
                val accessItems = remember {
                    accessModes.map { DropdownItem(title = "${it.label} 模式") }
                }
                val selectedAccessIndex =
                    accessModes.indexOf(state.cpuAccessMode).coerceAtLeast(0)

                WindowSpinnerPreference(
                    title = "授权模式",
                    summary = "${state.cpuAccessMode.label} 模式",
                    items = accessItems,
                    selectedIndex = selectedAccessIndex,
                    onSelectedIndexChange = { index ->
                        onSelectMode(accessModes[index])
                    },
                )

                val isRoot = state.cpuAccessMode == CpuAccessMode.ROOT
                val status = permissionStatus(state.cpuAccessStatus, isRoot)
                val isConnecting = state.cpuAccessStatus in listOf(
                    CpuAccessStatus.CONNECTING,
                    CpuAccessStatus.ROOT_CONNECTING,
                )

                BasicComponent(
                    title = if (isRoot) "Root 授权" else "Shizuku 授权",
                    summary = status.text,
                    enabled = !isConnecting,
                    onClick = if (isRoot) onRequestRoot else onRequestShizuku,
                    endActions = {
                        Text(
                            text = if (isConnecting) "等待中" else "授权",
                            color = status.color,
                            style = MiuixTheme.textStyles.footnote1,
                        )
                    },
                )
            }
        }
    }
}

private data class PermissionStatus(
    val text: String,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
private fun permissionStatus(
    status: CpuAccessStatus,
    isRoot: Boolean,
): PermissionStatus {
    if (isRoot) {
        return when (status) {
            CpuAccessStatus.ROOT ->
                PermissionStatus("已授权", HyperGreen)
            CpuAccessStatus.ROOT_CONNECTING ->
                PermissionStatus("等待授权", HyperAmber)
            CpuAccessStatus.ROOT_UNAVAILABLE ->
                PermissionStatus("未检测到 su", HyperRed)
            CpuAccessStatus.ROOT_DENIED ->
                PermissionStatus("授权失败", HyperRed)
            else ->
                PermissionStatus("未授权", HyperOnSurfaceMuted)
        }
    }
    return when (status) {
        CpuAccessStatus.SHIZUKU ->
            PermissionStatus("已连接", HyperGreen)
        CpuAccessStatus.CONNECTING ->
            PermissionStatus("正在连接", HyperAmber)
        CpuAccessStatus.SHIZUKU_STOPPED ->
            PermissionStatus("未运行", HyperRed)
        CpuAccessStatus.DENIED ->
            PermissionStatus("授权失败", HyperRed)
        CpuAccessStatus.DIRECT ->
            PermissionStatus("无需授权", HyperCyan)
        else ->
            PermissionStatus("未授权", HyperOnSurfaceMuted)
    }
}
