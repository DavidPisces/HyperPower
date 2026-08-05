package com.reiraku.hyperpower.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reiraku.hyperpower.data.BatteryMetrics
import com.reiraku.hyperpower.data.CHARGE_POWER_HISTORY_WINDOW_MILLIS
import com.reiraku.hyperpower.data.ChargePowerSample
import com.reiraku.hyperpower.data.CpuAccessStatus
import com.reiraku.hyperpower.data.CpuAccessMode
import com.reiraku.hyperpower.data.CpuCoreMetric
import com.reiraku.hyperpower.data.CpuCoreRole
import com.reiraku.hyperpower.data.CpuLoadSample
import com.reiraku.hyperpower.data.CpuMetrics
import com.reiraku.hyperpower.data.CPU_LOAD_HISTORY_WINDOW_MILLIS
import com.reiraku.hyperpower.data.DashboardMonitor
import com.reiraku.hyperpower.data.DashboardState
import com.reiraku.hyperpower.data.MemoryMetrics
import com.reiraku.hyperpower.ui.components.AnimatedMetricNumber
import com.reiraku.hyperpower.ui.components.HyperBottomBar
import com.reiraku.hyperpower.ui.components.HyperBottomNavItem
import com.reiraku.hyperpower.ui.navigation.Route
import com.reiraku.hyperpower.ui.theme.HyperAmber
import com.reiraku.hyperpower.ui.theme.HyperBackground
import com.reiraku.hyperpower.ui.theme.HyperCyan
import com.reiraku.hyperpower.ui.theme.HyperGreen
import com.reiraku.hyperpower.ui.theme.HyperOnSurfaceMuted
import com.reiraku.hyperpower.ui.theme.HyperOutline
import com.reiraku.hyperpower.ui.theme.HyperPowerTheme
import com.reiraku.hyperpower.ui.theme.HyperRed
import com.reiraku.hyperpower.ui.theme.HyperSurface
import com.reiraku.hyperpower.ui.theme.HyperSurfaceHigh
import com.reiraku.hyperpower.ui.theme.miuixBarColor
import com.reiraku.hyperpower.ui.theme.miuixGaussianBlur
import com.reiraku.hyperpower.ui.theme.rememberMiuixBlurBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

private const val TAB_ENTER_DURATION_MS = 300
private const val TAB_EXIT_DURATION_MS = 180
private const val TAB_FADE_IN_DURATION_MS = 210
private const val TAB_FADE_IN_DELAY_MS = 45
private const val TAB_FADE_OUT_DURATION_MS = 140
private const val TAB_ENTER_OFFSET_DIVISOR = 10
private const val TAB_EXIT_OFFSET_DIVISOR = 14

private val TabEnterEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val TabExitEasing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

private enum class DashboardTab {
    CPU,
    BATTERY,
    MEMORY,
}

private fun AnimatedContentTransitionScope<*>.horizontalContentSlide(
    forward: Boolean,
): ContentTransform {
    val direction = if (forward) 1 else -1
    return ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = TAB_ENTER_DURATION_MS,
                easing = TabEnterEasing,
            ),
            initialOffsetX = { width -> direction * width / TAB_ENTER_OFFSET_DIVISOR },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = TAB_FADE_IN_DURATION_MS,
                delayMillis = TAB_FADE_IN_DELAY_MS,
                easing = TabEnterEasing,
            ),
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = TAB_EXIT_DURATION_MS,
                easing = TabExitEasing,
            ),
            targetOffsetX = { width -> -direction * width / TAB_EXIT_OFFSET_DIVISOR },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = TAB_FADE_OUT_DURATION_MS,
                easing = TabExitEasing,
            ),
        ),
        sizeTransform = null,
    )
}

@Composable
fun DashboardRoute(
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val monitor = remember(context) { DashboardMonitor(context) }
    val state by monitor.state.collectAsState()
    val backStack = rememberNavBackStack(Route.Dashboard)

    DisposableEffect(monitor) {
        onDispose(monitor::close)
    }

    var gestureState: NavigationEventState<SceneInfo<NavKey>>? = null
    val onBack: (() -> Unit) -> Unit = { completeTransition ->
        completeTransition()
        backStack.removeLastOrNull()
    }

    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<Route.Dashboard> {
                DashboardScreen(
                    state = state,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRefresh = monitor::refresh,
                    onRequestShizuku = monitor::requestShizukuAccess,
                    onRequestRoot = monitor::requestRootAccess,
                    onOpenSettings = { backStack.add(Route.Settings) },
                )
            }
            entry<Route.Settings> {
                SettingsScreen(
                    state = state,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onBack = { onBack {} },
                    onOpenSuperIslandSettings = {
                        backStack.add(Route.SuperIslandSettings)
                    },
                    onOpenLiveNotificationSettings = {
                        backStack.add(Route.LiveNotificationSettings)
                    },
                    onSelectMode = monitor::setCpuAccessMode,
                    onRequestShizuku = monitor::requestShizukuAccess,
                    onRequestRoot = monitor::requestRootAccess,
                )
            }
            entry<Route.SuperIslandSettings> {
                SuperIslandSettingsScreen(
                    onBack = { onBack {} },
                )
            }
            entry<Route.LiveNotificationSettings> {
                LiveNotificationSettingsScreen(
                    onBack = { onBack {} },
                )
            }
        },
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = null,
        onBack = { onBack {} },
    )
    val scene = sceneState.currentScene
    gestureState = rememberNavigationEventState(
        currentInfo = SceneInfo(scene),
        backInfo = sceneState.previousScenes.map { SceneInfo(it) },
    )

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = { onBack {} },
        onBackCancelled = {},
    )

    NavDisplay(
        sceneState = sceneState,
        navigationEventState = gestureState,
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart,
        sizeTransform = null,
        transitionEffects = NavDisplayTransitionEffects(
            blockInputDuringTransition = true,
        ),
    )
}

@Composable
fun DashboardScreen(
    state: DashboardState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRefresh: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestRoot: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop()
    val tabStateHolder = rememberSaveableStateHolder()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshRequestedAfter by remember { mutableLongStateOf(Long.MAX_VALUE) }
    val tabs = remember {
        listOf(
            HyperBottomNavItem(label = "CPU", icon = Icons.Rounded.Speed),
            HyperBottomNavItem(label = "电池", icon = Icons.Rounded.BatteryChargingFull),
            HyperBottomNavItem(label = "内存", icon = Icons.Rounded.Memory),
        )
    }

    LaunchedEffect(state.updatedAtMillis, isRefreshing, refreshRequestedAfter) {
        if (isRefreshing && state.updatedAtMillis > refreshRequestedAfter) {
            delay(450)
            isRefreshing = false
        }
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
                title = "HyperPower",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "设置",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        bottomBar = {
            HyperBottomBar(
                items = tabs,
                selectedIndex = selectedTabIndex,
                onSelected = { index ->
                    if (index in tabs.indices) selectedTabIndex = index
                },
                backdrop = backdrop,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                transitionSpec = {
                    horizontalContentSlide(forward = targetState > initialState)
                },
                contentKey = { DashboardTab.entries[it] },
                label = "DashboardTabContentSlide",
            ) { pageIndex ->
                tabStateHolder.SaveableStateProvider(pageIndex) {
                    val requestRefresh = {
                        if (!isRefreshing) {
                            refreshRequestedAfter = state.updatedAtMillis
                            isRefreshing = true
                            onRefresh()
                        }
                    }
                    when (DashboardTab.entries[pageIndex]) {
                        DashboardTab.CPU -> CpuDashboardPage(
                            state = state,
                            notificationPermissionGranted = notificationPermissionGranted,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onRequestShizuku = onRequestShizuku,
                            onRequestRoot = onRequestRoot,
                            isRefreshing = isRefreshing,
                            onRefresh = requestRefresh,
                            contentPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )

                        DashboardTab.BATTERY -> BatteryDashboardPage(
                            state = state,
                            isRefreshing = isRefreshing,
                            onRefresh = requestRefresh,
                            contentPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )

                        DashboardTab.MEMORY -> MemoryDashboardPage(
                            state = state,
                            isRefreshing = isRefreshing,
                            onRefresh = requestRefresh,
                            contentPadding = innerPadding,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CpuDashboardPage(
    state: DashboardState,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestShizuku: () -> Unit,
    onRequestRoot: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    DashboardTabPage(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
    ) {
        if (!notificationPermissionGranted) {
            item {
                NotificationPermissionCard(
                    onRequestPermission = onRequestNotificationPermission,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (state.cpuAccessStatus !in listOf(
            CpuAccessStatus.DIRECT,
            CpuAccessStatus.SHIZUKU,
            CpuAccessStatus.ROOT,
            CpuAccessStatus.CHECKING,
        )
        ) {
            item {
                AccessCard(
                    status = state.cpuAccessStatus,
                    accessMode = state.cpuAccessMode,
                    onRequestAccess = if (state.cpuAccessMode == CpuAccessMode.ROOT) {
                        onRequestRoot
                    } else {
                        onRequestShizuku
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            DashboardSection(
                eyebrow = "CPU",
                title = "处理器负载",
                trailing = { SourcePill(state.cpuAccessStatus) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                CpuOverviewCard(cpu = state.cpu)
            }
        }

        item {
            DashboardSection(
                eyebrow = "CPU LOAD",
                title = "最近 30 秒",
                trailing = { CurrentCpuLoad(state.cpu.overallUsage) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                CpuLoadHistoryCard(
                    samples = state.cpuLoadHistory,
                    windowEndMillis = state.updatedAtMillis,
                    currentUsage = state.cpu.overallUsage,
                )
            }
        }

        item {
            SectionTitle(
                eyebrow = "CORE TELEMETRY",
                title = "核心状态",
                trailing = "${state.cpu.coreCount} 核",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
        }

        items(
            items = state.cpu.cores.chunked(2),
            key = { row -> row.firstOrNull()?.id ?: -1 },
        ) { rowCores ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowCores.forEach { core ->
                    CoreCard(
                        core = core,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowCores.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        item {
            MonitorFooter(
                description = if (state.cpuAccessMode == CpuAccessMode.ROOT) {
                    "采样间隔 1 秒 · Root 只读采集 CPU 系统节点"
                } else {
                    "采样间隔 1 秒 · Shizuku 采集 Linux 调度统计"
                },
                updatedAtMillis = state.updatedAtMillis,
            )
        }
    }
}

@Composable
private fun BatteryDashboardPage(
    state: DashboardState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    DashboardTabPage(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
    ) {
        item {
            DashboardSection(
                eyebrow = "BATTERY",
                title = "电池状态",
                trailing = { BatteryStatusPill(state.battery) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                BatteryCard(battery = state.battery)
            }
        }
        item {
            val currentPowerWatts = if (state.battery.isCharging) {
                state.battery.estimatedPowerWatts
            } else {
                null
            }
            DashboardSection(
                eyebrow = "CHARGE POWER",
                title = "充电功率曲线",
                trailing = {
                    ChargePowerSummary(
                        samples = state.chargePowerHistory,
                        isCharging = state.battery.isCharging,
                        currentPowerWatts = currentPowerWatts,
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                ChargePowerHistoryCard(
                    samples = state.chargePowerHistory,
                    isCharging = state.battery.isCharging,
                    currentPowerWatts = currentPowerWatts,
                )
            }
        }
        item {
            MonitorFooter(
                description = "采样间隔 1 秒 · Android 电池管理器实时统计",
                updatedAtMillis = state.updatedAtMillis,
            )
        }
    }
}

@Composable
private fun MemoryDashboardPage(
    state: DashboardState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    DashboardTabPage(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
    ) {
        item {
            DashboardSection(
                eyebrow = "MEMORY",
                title = "内存状态",
                trailing = { MemoryStatusPill(state.memory) },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                MemoryOverviewCard(memory = state.memory)
            }
        }
        item {
            DashboardSection(
                eyebrow = "RAM DETAILS",
                title = "内存详情",
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                MemoryDetailsCard(memory = state.memory)
            }
        }
        item {
            MonitorFooter(
                description = "采样间隔 1 秒 · 系统 ActivityManager 内存统计",
                updatedAtMillis = state.updatedAtMillis,
            )
        }
    }
}

@Composable
private fun DashboardTabPage(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    content: LazyListScope.() -> Unit,
) {
    PullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        topAppBarScrollBehavior = scrollBehavior,
        refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新", "刷新完成"),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .scrollEndHaptic()
                .overScrollVertical(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            overscrollEffect = null,
            content = content,
        )
    }
}

@Composable
private fun MonitorFooter(
    description: String,
    updatedAtMillis: Long,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = description,
            color = HyperOnSurfaceMuted,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(modifier = Modifier.height(3.dp))
        MiuixText(
            text = "上次刷新 · ${formatUpdateTime(updatedAtMillis)}",
            color = HyperOnSurfaceMuted,
            style = MiuixTheme.textStyles.footnote2,
        )
    }
}

@Composable
private fun NotificationPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = HyperAmber.copy(alpha = 0.10f).compositeOver(HyperSurface),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        BasicComponent(
            title = "开启实时监控通知",
            summary = "允许通知后可在后台显示电流、电压、功耗和 CPU 状态",
            endActions = {
                TextButton(
                    text = "开启",
                    onClick = onRequestPermission,
                )
            },
            onClick = onRequestPermission,
        )
    }
}

@Composable
private fun AccessCard(
    status: CpuAccessStatus,
    accessMode: CpuAccessMode,
    onRequestAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = when (status) {
        CpuAccessStatus.CONNECTING -> "正在连接 Shizuku 高级采集服务…"
        CpuAccessStatus.SHIZUKU_STOPPED -> "Shizuku 尚未运行。请先启动 Shizuku，再返回重试。"
        CpuAccessStatus.DENIED -> "未授予 Shizuku 权限，CPU 占用可能无法读取。"
        CpuAccessStatus.ROOT_REQUIRED -> "Root 模式需要超级用户授权后才能读取受限 CPU 节点。"
        CpuAccessStatus.ROOT_CONNECTING -> "正在等待 Root 管理器确认授权…"
        CpuAccessStatus.ROOT_DENIED -> "Root 授权被拒绝或已经失效，请在 Root 管理器中重新允许。"
        CpuAccessStatus.ROOT_UNAVAILABLE -> "未发现可用的 su，请确认设备已通过 Magisk、KernelSU 或 APatch Root。"
        else -> "Android 限制了全局 CPU 统计，授权 Shizuku 后可读取每核占用。"
    }
    val isRoot = accessMode == CpuAccessMode.ROOT
    val accent = if (isRoot) HyperRed else HyperGreen
    val isConnecting = status in listOf(
        CpuAccessStatus.CONNECTING,
        CpuAccessStatus.ROOT_CONNECTING,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = accent.copy(alpha = 0.08f).compositeOver(HyperSurface),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isRoot) "#" else "S",
                    color = accent,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRoot) "启用 Root 精确采集" else "启用完整 CPU 监控",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    color = HyperOnSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onRequestAccess,
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = HyperBackground,
                ),
            ) {
                Text(
                    text = if (isConnecting) "授权中" else "授权",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CpuOverviewCard(
    cpu: CpuMetrics,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UsageRing(
                progress = cpu.overallUsage,
                color = HyperGreen,
                label = "总占用",
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                InlineMetric(
                    label = "逻辑核心",
                    value = if (cpu.coreCount > 0) "${cpu.coreCount}" else "—",
                    unit = "CORES",
                )
                HorizontalDivider(color = HyperOutline)
                AnimatedFrequencyMetric(
                    label = "平均频率",
                    frequencyKhz = cpu.averageFrequencyKhz,
                )
            }
        }
    }
}

@Composable
private fun CpuLoadHistoryCard(
    samples: List<CpuLoadSample>,
    windowEndMillis: Long,
    currentUsage: Float?,
    modifier: Modifier = Modifier,
) {
    val lineColor by animateColorAsState(
        targetValue = usageColor(currentUsage ?: 0f),
        label = "cpu-history-line-color",
    )

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            CpuLoadChart(
                samples = samples,
                windowEndMillis = windowEndMillis,
                currentUsage = currentUsage,
                lineColor = lineColor,
            )
        }
    }
}

@Composable
private fun CpuLoadChart(
    samples: List<CpuLoadSample>,
    windowEndMillis: Long,
    currentUsage: Float?,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    var displayedEndMillis by remember {
        mutableLongStateOf(maxOf(windowEndMillis, System.currentTimeMillis()))
    }
    val animatedCurrentUsage by animateFloatAsState(
        targetValue = currentUsage
            ?: samples.lastOrNull()?.usage
            ?: 0f,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "cpu-history-latest-value",
    )

    LaunchedEffect(Unit) {
        var previousUpdateNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (previousUpdateNanos == 0L ||
                    frameTimeNanos - previousUpdateNanos >= CHART_FRAME_INTERVAL_NANOS
                ) {
                    displayedEndMillis = System.currentTimeMillis()
                    previousUpdateNanos = frameTimeNanos
                }
            }
        }
    }

    val gridColor = HyperOutline
    val emptyTextColor = HyperOnSurfaceMuted

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topInset = 4.dp.toPx()
                val bottomInset = 4.dp.toPx()
                val chartHeight = size.height - topInset - bottomInset

                listOf(0f, 0.5f, 1f).forEach { level ->
                    val y = topInset + chartHeight * level
                    drawLine(
                        color = gridColor.copy(alpha = if (level == 0.5f) 0.48f else 0.32f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                val windowStartMillis =
                    displayedEndMillis - CPU_LOAD_HISTORY_WINDOW_MILLIS
                val chartWidth = size.width
                val points = buildList {
                    samples.forEachIndexed { index, sample ->
                        val elapsed = sample.timestampMillis - windowStartMillis
                        val usage = if (index == samples.lastIndex) {
                            animatedCurrentUsage
                        } else {
                            sample.usage
                        }
                        add(
                            Offset(
                                x = elapsed.toFloat() /
                                    CPU_LOAD_HISTORY_WINDOW_MILLIS.toFloat() * chartWidth,
                                y = topInset +
                                    (1f - usage.coerceIn(0f, 1f)) * chartHeight,
                            ),
                        )
                    }
                    if (samples.isNotEmpty()) {
                        add(
                            Offset(
                                x = chartWidth,
                                y = topInset +
                                    (1f - animatedCurrentUsage.coerceIn(0f, 1f)) *
                                    chartHeight,
                            ),
                        )
                    }
                }

                if (points.isNotEmpty()) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.zipWithNext().forEach { (previous, current) ->
                            val controlX = (previous.x + current.x) / 2f
                            cubicTo(
                                controlX,
                                previous.y,
                                controlX,
                                current.y,
                                current.x,
                                current.y,
                            )
                        }
                    }
                    val fillPath = Path().apply {
                        moveTo(points.first().x, size.height)
                        lineTo(points.first().x, points.first().y)
                        points.zipWithNext().forEach { (previous, current) ->
                            val controlX = (previous.x + current.x) / 2f
                            cubicTo(
                                controlX,
                                previous.y,
                                controlX,
                                current.y,
                                current.x,
                                current.y,
                            )
                        }
                        lineTo(points.last().x, size.height)
                        close()
                    }

                    clipRect {
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.20f),
                                    lineColor.copy(alpha = 0.015f),
                                ),
                                startY = topInset,
                                endY = size.height,
                            ),
                        )
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        points.lastOrNull()
                            ?.takeIf { it.x in 0f..size.width }
                            ?.let { latest ->
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.20f),
                                    radius = 6.dp.toPx(),
                                    center = latest,
                                )
                                drawCircle(
                                    color = lineColor,
                                    radius = 2.6.dp.toPx(),
                                    center = latest,
                                )
                            }
                    }
                }
            }

            if (samples.isEmpty()) {
                MiuixText(
                    text = "正在收集负载数据",
                    color = emptyTextColor,
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MiuixText(
                text = "−30s",
                color = HyperOnSurfaceMuted,
                style = MiuixTheme.textStyles.footnote2,
                modifier = Modifier.weight(1f),
            )
            MiuixText(
                text = "现在",
                color = HyperOnSurfaceMuted,
                style = MiuixTheme.textStyles.footnote2,
            )
        }
    }
}

@Composable
private fun ChargePowerHistoryCard(
    samples: List<ChargePowerSample>,
    isCharging: Boolean,
    currentPowerWatts: Double?,
    modifier: Modifier = Modifier,
) {
    val peakPower = samples.maxOfOrNull(ChargePowerSample::powerWatts)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    text = "最近 5 分钟",
                    color = HyperOnSurfaceMuted,
                    style = MiuixTheme.textStyles.footnote2,
                    modifier = Modifier.weight(1f),
                )
                MiuixText(
                    text = peakPower?.let { "峰值 ${formatDecimal(it, 2)} W" } ?: "等待数据",
                    color = HyperOnSurfaceMuted,
                    style = MiuixTheme.textStyles.footnote2,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ChargePowerChart(
                samples = samples,
                isCharging = isCharging,
                currentPowerWatts = currentPowerWatts,
            )
        }
    }
}

@Composable
private fun ChargePowerChart(
    samples: List<ChargePowerSample>,
    isCharging: Boolean,
    currentPowerWatts: Double?,
    modifier: Modifier = Modifier,
) {
    val lastSampleTime = samples.lastOrNull()?.timestampMillis
    var displayedEndMillis by remember {
        mutableLongStateOf(lastSampleTime ?: System.currentTimeMillis())
    }
    val animatedCurrentPower by animateFloatAsState(
        targetValue = (currentPowerWatts ?: samples.lastOrNull()?.powerWatts ?: 0.0).toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "charge-power-latest-value",
    )
    val animatedScaleMaximum by animateFloatAsState(
        targetValue = chargePowerScaleMaximum(samples, currentPowerWatts).toFloat(),
        animationSpec = tween(durationMillis = 450),
        label = "charge-power-scale",
    )

    LaunchedEffect(isCharging, lastSampleTime) {
        if (!isCharging) {
            displayedEndMillis = lastSampleTime ?: System.currentTimeMillis()
        }
    }
    LaunchedEffect(isCharging) {
        if (!isCharging) return@LaunchedEffect
        var previousUpdateNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (previousUpdateNanos == 0L ||
                    frameTimeNanos - previousUpdateNanos >= CHART_FRAME_INTERVAL_NANOS
                ) {
                    displayedEndMillis = System.currentTimeMillis()
                    previousUpdateNanos = frameTimeNanos
                }
            }
        }
    }

    val scaleMaximum = animatedScaleMaximum.coerceAtLeast(1f)
    val gridColor = HyperOutline
    val lineColor = HyperGreen

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .height(112.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                PowerScaleLabel(scaleMaximum.toDouble())
                PowerScaleLabel(scaleMaximum.toDouble() / 2.0)
                PowerScaleLabel(0.0)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val topInset = 4.dp.toPx()
                    val bottomInset = 4.dp.toPx()
                    val chartHeight = size.height - topInset - bottomInset

                    listOf(0f, 0.5f, 1f).forEach { level ->
                        val y = topInset + chartHeight * level
                        drawLine(
                            color = gridColor.copy(alpha = if (level == 0.5f) 0.48f else 0.32f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val windowStartMillis =
                        displayedEndMillis - CHARGE_POWER_HISTORY_WINDOW_MILLIS
                    val chartWidth = size.width
                    val visibleSamples = samples.filter {
                        it.timestampMillis >= windowStartMillis &&
                            it.timestampMillis <= displayedEndMillis
                    }
                    val points = buildList {
                        visibleSamples.forEachIndexed { index, sample ->
                            val elapsed = sample.timestampMillis - windowStartMillis
                            val power = if (index == visibleSamples.lastIndex) {
                                animatedCurrentPower.toDouble()
                            } else {
                                sample.powerWatts
                            }
                            add(
                                Offset(
                                    x = elapsed.toFloat() /
                                        CHARGE_POWER_HISTORY_WINDOW_MILLIS.toFloat() * chartWidth,
                                    y = topInset +
                                        (1f - (power / scaleMaximum).toFloat().coerceIn(0f, 1f)) *
                                        chartHeight,
                                ),
                            )
                        }
                        if (isCharging && visibleSamples.isNotEmpty()) {
                            add(
                                Offset(
                                    x = chartWidth,
                                    y = topInset +
                                        (1f - (animatedCurrentPower / scaleMaximum).coerceIn(0f, 1f)) *
                                        chartHeight,
                                ),
                            )
                        }
                    }

                    if (points.isNotEmpty()) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.zipWithNext().forEach { (previous, current) ->
                                val controlX = (previous.x + current.x) / 2f
                                cubicTo(
                                    controlX,
                                    previous.y,
                                    controlX,
                                    current.y,
                                    current.x,
                                    current.y,
                                )
                            }
                        }
                        val fillPath = Path().apply {
                            moveTo(points.first().x, size.height)
                            lineTo(points.first().x, points.first().y)
                            points.zipWithNext().forEach { (previous, current) ->
                                val controlX = (previous.x + current.x) / 2f
                                cubicTo(
                                    controlX,
                                    previous.y,
                                    controlX,
                                    current.y,
                                    current.x,
                                    current.y,
                                )
                            }
                            lineTo(points.last().x, size.height)
                            close()
                        }

                        clipRect {
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.22f),
                                        lineColor.copy(alpha = 0.015f),
                                    ),
                                    startY = topInset,
                                    endY = size.height,
                                ),
                            )
                            drawPath(
                                path = linePath,
                                color = lineColor,
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
                            )
                            points.lastOrNull()
                                ?.takeIf { it.x in 0f..size.width }
                                ?.let { latest ->
                                    drawCircle(
                                        color = lineColor.copy(alpha = 0.20f),
                                        radius = 6.dp.toPx(),
                                        center = latest,
                                    )
                                    drawCircle(
                                        color = lineColor,
                                        radius = 2.6.dp.toPx(),
                                        center = latest,
                                    )
                                }
                        }
                    }
                }

                if (samples.isEmpty()) {
                    MiuixText(
                        text = if (isCharging) {
                            "正在收集充电功率"
                        } else {
                            "检测到充电后开始记录"
                        },
                        color = HyperOnSurfaceMuted,
                        style = MiuixTheme.textStyles.footnote2,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp),
        ) {
            MiuixText(
                text = "−5m",
                color = HyperOnSurfaceMuted,
                style = MiuixTheme.textStyles.footnote2,
                modifier = Modifier.weight(1f),
            )
            MiuixText(
                text = if (!isCharging && samples.isNotEmpty()) "充电结束" else "现在",
                color = HyperOnSurfaceMuted,
                style = MiuixTheme.textStyles.footnote2,
            )
        }
    }
}

@Composable
private fun PowerScaleLabel(value: Double) {
    val decimals = if (value % 1.0 == 0.0) 0 else 1
    MiuixText(
        text = "${formatDecimal(value, decimals)} W",
        color = HyperOnSurfaceMuted,
        style = MiuixTheme.textStyles.footnote2.copy(fontSize = 9.sp),
    )
}

private fun chargePowerScaleMaximum(
    samples: List<ChargePowerSample>,
    currentPowerWatts: Double?,
): Double {
    val observedMaximum = maxOf(
        samples.maxOfOrNull(ChargePowerSample::powerWatts) ?: 0.0,
        currentPowerWatts ?: 0.0,
    )
    return maxOf(5.0, kotlin.math.ceil(observedMaximum * 1.1 / 5.0) * 5.0)
}

@Composable
private fun CoreCard(
    core: CpuCoreMetric,
    modifier: Modifier = Modifier,
) {
    val progress = core.usage ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "core-${core.id}-usage",
    )
    val loadColor by animateColorAsState(
        targetValue = usageColor(progress),
        label = "core-${core.id}-color",
    )

    Card(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CPU ${core.id}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (core.usage == null) {
                    Text(
                        text = "—",
                        color = HyperOnSurfaceMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    )
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedMetricNumber(
                            value = (core.usage * 100).roundToInt(),
                            style = MiuixTheme.textStyles.title4,
                            color = loadColor,
                            animationLabel = "core-${core.id}-number",
                        )
                        MiuixText(
                            text = "%",
                            color = loadColor,
                            style = MiuixTheme.textStyles.footnote2.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
                        )
                    }
                }
            }
            if (core.architecture != null || core.role != CpuCoreRole.UNKNOWN) {
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    core.architecture?.let { architecture ->
                        CoreTag(
                            text = architecture,
                            color = HyperOnSurfaceMuted,
                        )
                    }
                    if (core.role != CpuCoreRole.UNKNOWN) {
                        CoreTag(
                            text = core.role.label,
                            color = coreRoleColor(core.role),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = loadColor,
                trackColor = HyperSurfaceHigh,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(11.dp))
            Text(
                text = "${formatFrequencyWithUnit(core.frequencyKhz)}",
                color = HyperOnSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoreTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MemoryOverviewCard(
    memory: MemoryMetrics,
    modifier: Modifier = Modifier,
) {
    val usage = memory.usage
    val color = memoryUsageColor(usage, memory.isLowMemory)
    MetricCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UsageRing(
                progress = usage,
                color = color,
                label = "内存占用",
            )
            Spacer(modifier = Modifier.width(22.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InlineMetric(
                    label = "可用内存",
                    value = formatMemoryValue(memory.availableBytes),
                    unit = memoryUnit(memory.availableBytes),
                )
                HorizontalDivider(color = HyperOutline)
                InlineMetric(
                    label = "已用内存",
                    value = formatMemoryValue(memory.usedBytes),
                    unit = memoryUnit(memory.usedBytes),
                )
            }
        }
    }
}

@Composable
private fun MemoryDetailsCard(
    memory: MemoryMetrics,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile(
                    label = "物理内存",
                    value = formatMemoryValue(memory.totalBytes),
                    unit = memoryUnit(memory.totalBytes),
                    accent = HyperCyan,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "当前占用",
                    value = formatMemoryValue(memory.usedBytes),
                    unit = memoryUnit(memory.usedBytes),
                    accent = memoryUsageColor(memory.usage, memory.isLowMemory),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile(
                    label = "当前可用",
                    value = formatMemoryValue(memory.availableBytes),
                    unit = memoryUnit(memory.availableBytes),
                    accent = HyperGreen,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "低内存阈值",
                    value = formatMemoryValue(memory.lowMemoryThresholdBytes),
                    unit = memoryUnit(memory.lowMemoryThresholdBytes),
                    accent = HyperAmber,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BatteryCard(
    battery: BatteryMetrics,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UsageRing(
                progress = battery.levelPercent?.div(100f),
                color = batteryLevelColor(battery.levelPercent),
                label = "剩余电量",
            )
            Spacer(modifier = Modifier.width(22.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InlineMetric(
                    label = "预计使用时间",
                    value = when {
                        battery.isCharging -> "充电中"
                        else -> formatDuration(battery.estimatedRemainingMillis)
                    },
                    unit = if (battery.isCharging) "" else "EST.",
                )
                HorizontalDivider(color = HyperOutline)
                InlineMetric(
                    label = "瞬时功率",
                    value = battery.estimatedPowerWatts?.let { formatDecimal(it, 2) } ?: "—",
                    unit = "W",
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "电压",
                value = battery.voltageMv?.let { formatDecimal(it / 1_000.0, 2) } ?: "—",
                unit = "V",
                accent = HyperGreen,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "当前电流",
                value = formatCurrent(battery.currentUa),
                unit = "mA",
                accent = HyperAmber,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricTile(
                label = "剩余电荷",
                value = battery.chargeUah?.let { formatDecimal(it / 1_000.0, 0) } ?: "—",
                unit = "mAh",
                accent = HyperGreen,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "电池温度",
                value = battery.temperatureCelsius?.let { formatDecimal(it.toDouble(), 1) } ?: "—",
                unit = "°C",
                accent = HyperRed,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun UsageRing(
    progress: Float?,
    color: Color,
    label: String,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        label = "$label-ring",
    )
    val trackColor = HyperSurfaceHigh

    Box(
        modifier = Modifier.size(118.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress != null) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedProgress.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (progress == null) {
                Text(
                    text = "—",
                    color = HyperOnSurfaceMuted,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedMetricNumber(
                        value = (progress * 100).roundToInt(),
                        style = MiuixTheme.textStyles.title2.copy(fontSize = 27.sp),
                        color = color,
                        animationLabel = "$label-number",
                    )
                    MiuixText(
                        text = "%",
                        color = color.copy(alpha = 0.82f),
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 1.dp, bottom = 4.dp),
                    )
                }
            }
            Text(
                text = label,
                color = HyperOnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    unit: String,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = HyperOnSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        if (unit.isNotBlank()) {
            Text(
                text = unit,
                color = HyperOnSurfaceMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun AnimatedFrequencyMetric(
    label: String,
    frequencyKhz: Long?,
) {
    val unit = frequencyUnit(frequencyKhz)
    val valueStyle = MiuixTheme.textStyles.title3.copy(fontSize = 22.sp)

    Row(verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = HyperOnSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            if (frequencyKhz == null) {
                Text(
                    text = "—",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                androidx.compose.runtime.key(unit) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (unit == "GHz") {
                            val hundredths = (frequencyKhz / 10_000.0).roundToInt()
                            AnimatedMetricNumber(
                                value = hundredths / 100,
                                style = valueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                animationLabel = "average-frequency-whole",
                            )
                            MiuixText(
                                text = ".",
                                style = valueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                            AnimatedMetricNumber(
                                value = hundredths % 100,
                                style = valueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                animationLabel = "average-frequency-fraction",
                                minimumDigits = 2,
                            )
                        } else {
                            AnimatedMetricNumber(
                                value = (frequencyKhz / 1_000.0).roundToInt(),
                                style = valueStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                                animationLabel = "average-frequency-mhz",
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = unit,
            color = HyperOnSurfaceMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.defaultColors(
            color = HyperSurfaceHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = label,
                    color = HyperOnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = HyperOnSurfaceMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow,
                    color = HyperOnSurfaceMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            trailing()
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CurrentCpuLoad(usage: Float?) {
    val color = usageColor(usage ?: 0f)
    if (usage == null) {
        MiuixText(
            text = "—",
            color = HyperOnSurfaceMuted,
            style = MiuixTheme.textStyles.title4,
        )
    } else {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedMetricNumber(
                value = (usage * 100).roundToInt(),
                style = MiuixTheme.textStyles.title4,
                color = color,
                animationLabel = "cpu-history-current-load",
            )
            MiuixText(
                text = "%",
                color = color.copy(alpha = 0.82f),
                style = MiuixTheme.textStyles.footnote2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 1.dp, bottom = 1.dp),
            )
        }
    }
}

@Composable
private fun BatteryStatusPill(battery: BatteryMetrics) {
    StatusPill(
        text = when {
            battery.isCharging -> "充电中"
            battery.isPowerConnected -> "已接电源"
            else -> "正在放电"
        },
        color = if (battery.isCharging) HyperGreen else HyperAmber,
    )
}

@Composable
private fun MemoryStatusPill(memory: MemoryMetrics) {
    StatusPill(
        text = when {
            memory.isLowMemory -> "内存紧张"
            memory.totalBytes != null -> "运行正常"
            else -> "检测中"
        },
        color = memoryUsageColor(memory.usage, memory.isLowMemory),
    )
}

@Composable
private fun ChargePowerSummary(
    samples: List<ChargePowerSample>,
    isCharging: Boolean,
    currentPowerWatts: Double?,
) {
    val displayedPower = currentPowerWatts ?: samples.lastOrNull()?.powerWatts
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = displayedPower?.let { "${formatDecimal(it, 2)} W" } ?: "—",
            color = if (isCharging) HyperGreen else HyperOnSurfaceMuted,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        MiuixText(
            text = when {
                isCharging -> "实时功率"
                samples.isNotEmpty() -> "末次功率"
                else -> "等待充电"
            },
            color = HyperOnSurfaceMuted,
            style = MiuixTheme.textStyles.footnote2,
        )
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String,
    trailing: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = HyperOnSurfaceMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = trailing,
            color = HyperGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SourcePill(status: CpuAccessStatus) {
    val (text, color) = when (status) {
        CpuAccessStatus.SHIZUKU -> "SHIZUKU" to HyperGreen
        CpuAccessStatus.ROOT -> "ROOT" to HyperRed
        CpuAccessStatus.DIRECT -> "DIRECT" to HyperCyan
        CpuAccessStatus.CONNECTING -> "CONNECTING" to HyperAmber
        CpuAccessStatus.ROOT_CONNECTING -> "ROOT…" to HyperAmber
        else -> "LIMITED" to HyperRed
    }
    StatusPill(text = text, color = color)
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = CircleShape,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun usageColor(usage: Float): Color = when {
    usage >= 0.85f -> HyperRed
    usage >= 0.60f -> HyperAmber
    else -> HyperGreen
}

@Composable
private fun coreRoleColor(role: CpuCoreRole): Color = when (role) {
    CpuCoreRole.ULTRA -> HyperRed
    CpuCoreRole.PERFORMANCE -> HyperAmber
    CpuCoreRole.EFFICIENCY -> HyperCyan
    CpuCoreRole.GENERAL -> HyperGreen
    CpuCoreRole.UNKNOWN -> HyperOnSurfaceMuted
}

@Composable
private fun batteryLevelColor(level: Int?): Color = when {
    level == null -> HyperOnSurfaceMuted
    level <= 15 -> HyperRed
    level <= 35 -> HyperAmber
    else -> HyperGreen
}

@Composable
private fun memoryUsageColor(usage: Float?, isLowMemory: Boolean): Color = when {
    usage == null -> HyperOnSurfaceMuted
    isLowMemory || usage >= 0.90f -> HyperRed
    usage >= 0.75f -> HyperAmber
    else -> HyperGreen
}

private fun formatFrequency(frequencyKhz: Long?): String {
    frequencyKhz ?: return "—"
    return if (frequencyKhz >= 1_000_000L) {
        formatDecimal(frequencyKhz / 1_000_000.0, 2)
    } else {
        formatDecimal(frequencyKhz / 1_000.0, 0)
    }
}

private fun frequencyUnit(frequencyKhz: Long?): String =
    if (frequencyKhz != null && frequencyKhz >= 1_000_000L) "GHz" else "MHz"

private fun formatFrequencyWithUnit(frequencyKhz: Long?): String =
    "${formatFrequency(frequencyKhz)} ${frequencyUnit(frequencyKhz)}"

private fun formatCurrent(currentUa: Long?): String {
    currentUa ?: return "—"
    val currentMa = currentUa / 1_000.0
    val prefix = when {
        currentMa > 0 -> "+"
        currentMa < 0 -> "−"
        else -> ""
    }
    return prefix + formatDecimal(abs(currentMa), 0)
}

private fun formatMemoryValue(bytes: Long?): String {
    bytes ?: return "—"
    return if (bytes >= BYTES_PER_GIBIBYTE) {
        formatDecimal(bytes / BYTES_PER_GIBIBYTE.toDouble(), 2)
    } else {
        formatDecimal(bytes / BYTES_PER_MEBIBYTE.toDouble(), 0)
    }
}

private fun memoryUnit(bytes: Long?): String =
    if (bytes != null && bytes >= BYTES_PER_GIBIBYTE) "GB" else "MB"

private fun formatDuration(durationMillis: Long?): String {
    durationMillis ?: return "—"
    val totalMinutes = durationMillis / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes / 60L) % 24L
    val minutes = totalMinutes % 60L
    return when {
        days > 0 -> "${days}天 ${hours}小时"
        hours > 0 -> "${hours}小时 ${minutes}分"
        else -> "${minutes}分钟"
    }
}

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)

private fun formatUpdateTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private const val CHART_FRAME_INTERVAL_NANOS = 32_000_000L
private const val BYTES_PER_MEBIBYTE = 1_024L * 1_024L
private const val BYTES_PER_GIBIBYTE = 1_024L * BYTES_PER_MEBIBYTE

@Preview(showBackground = true, backgroundColor = 0xFF071012)
@Composable
private fun DashboardPreview() {
    val now = System.currentTimeMillis()
    HyperPowerTheme {
        DashboardScreen(
            state = DashboardState(
                cpu = CpuMetrics(
                    overallUsage = 0.43f,
                    cores = List(8) { id ->
                        CpuCoreMetric(
                            id = id,
                            usage = (id + 2) / 12f,
                            frequencyKhz = if (id < 4) 1_800_000L else 2_850_000L,
                            architecture = when {
                                id < 4 -> "Cortex-A510"
                                id == 7 -> "Cortex-X2"
                                else -> "Cortex-A710"
                            },
                            role = when {
                                id < 4 -> CpuCoreRole.EFFICIENCY
                                id == 7 -> CpuCoreRole.ULTRA
                                else -> CpuCoreRole.PERFORMANCE
                            },
                        )
                    },
                ),
                cpuLoadHistory = List(30) { index ->
                    CpuLoadSample(
                        timestampMillis = now - (29 - index) * 1_000L,
                        usage = (0.32f + (index % 7) * 0.045f).coerceAtMost(1f),
                    )
                },
                chargePowerHistory = List(60) { index ->
                    ChargePowerSample(
                        timestampMillis = now - (59 - index) * 1_000L,
                        powerWatts = 42.0 + (index % 9) * 0.85,
                    )
                },
                battery = BatteryMetrics(
                    levelPercent = 76,
                    voltageMv = 4_087,
                    currentUa = 12_200_000,
                    chargeUah = 3_640_000,
                    temperatureCelsius = 32.6f,
                    isCharging = true,
                    isPowerConnected = true,
                ),
                memory = MemoryMetrics(
                    totalBytes = 16L * BYTES_PER_GIBIBYTE,
                    availableBytes = 5L * BYTES_PER_GIBIBYTE,
                    lowMemoryThresholdBytes = 1L * BYTES_PER_GIBIBYTE,
                ),
                cpuAccessStatus = CpuAccessStatus.SHIZUKU,
            ),
            notificationPermissionGranted = true,
            onRequestNotificationPermission = {},
            onRefresh = {},
            onRequestShizuku = {},
            onRequestRoot = {},
            onOpenSettings = {},
        )
    }
}
