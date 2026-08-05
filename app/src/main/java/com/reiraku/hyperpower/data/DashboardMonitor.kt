package com.reiraku.hyperpower.data

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import androidx.core.content.edit
import com.reiraku.hyperpower.privileged.IPrivilegedMonitor
import com.reiraku.hyperpower.privileged.PowerUserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

class DashboardMonitor(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val batteryReader = BatteryReader(appContext)
    private val memoryReader = MemoryReader(appContext)
    private val cpuSampler = CpuStatSampler()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val resetSampler = AtomicBoolean(false)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val preferences = appContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val initialAccessMode = preferences.getString(ACCESS_MODE_KEY, null)
        ?.let { saved -> CpuAccessMode.entries.firstOrNull { it.name == saved } }
        ?: CpuAccessMode.SHIZUKU

    private val _state = MutableStateFlow(
        DashboardState(
            cpuAccessMode = initialAccessMode,
            cpuAccessStatus = if (initialAccessMode == CpuAccessMode.ROOT) {
                CpuAccessStatus.ROOT_REQUIRED
            } else {
                CpuAccessStatus.CHECKING
            },
        ),
    )
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    @Volatile
    private var privilegedService: IPrivilegedMonitor? = null

    @Volatile
    private var rootGranted = false

    @Volatile
    private var identityCache: CpuIdentityCache? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(appContext, PowerUserService::class.java),
    ).processNameSuffix("monitor")

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (_state.value.cpuAccessMode != CpuAccessMode.SHIZUKU) {
                runCatching {
                    Shizuku.unbindUserService(userServiceArgs, this, false)
                }
                return
            }
            privilegedService = IPrivilegedMonitor.Stub.asInterface(binder)
            identityCache = null
            resetSampler.set(true)
            _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.SHIZUKU) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            privilegedService = null
            identityCache = null
            resetSampler.set(true)
            if (_state.value.cpuAccessMode == CpuAccessMode.SHIZUKU) {
                _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.SHIZUKU_STOPPED) }
            }
        }
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != SHIZUKU_PERMISSION_REQUEST) return@OnRequestPermissionResultListener
            if (_state.value.cpuAccessMode != CpuAccessMode.SHIZUKU) {
                return@OnRequestPermissionResultListener
            }
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindShizukuService()
            } else {
                _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.DENIED) }
            }
        }

    init {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        scope.launch { monitorLoop() }
        if (initialAccessMode == CpuAccessMode.ROOT) {
            requestRootAccess()
        } else {
            tryAutoConnectShizuku()
        }
    }

    fun setCpuAccessMode(mode: CpuAccessMode) {
        if (_state.value.cpuAccessMode == mode) return
        preferences.edit { putString(ACCESS_MODE_KEY, mode.name) }
        identityCache = null
        resetSampler.set(true)

        if (mode == CpuAccessMode.ROOT) {
            disconnectShizuku()
            rootGranted = false
            _state.update {
                it.copy(
                    cpuAccessMode = CpuAccessMode.ROOT,
                    cpuAccessStatus = CpuAccessStatus.ROOT_REQUIRED,
                )
            }
            requestRootAccess()
        } else {
            rootGranted = false
            _state.update {
                it.copy(
                    cpuAccessMode = CpuAccessMode.SHIZUKU,
                    cpuAccessStatus = CpuAccessStatus.CHECKING,
                )
            }
            tryAutoConnectShizuku()
        }
    }

    fun requestShizukuAccess() {
        if (_state.value.cpuAccessMode != CpuAccessMode.SHIZUKU) {
            setCpuAccessMode(CpuAccessMode.SHIZUKU)
        }
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.SHIZUKU_STOPPED) }
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindShizukuService()
        } else {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST)
        }
    }

    fun requestRootAccess() {
        if (_state.value.cpuAccessMode != CpuAccessMode.ROOT) {
            setCpuAccessMode(CpuAccessMode.ROOT)
            return
        }
        if (_state.value.cpuAccessStatus == CpuAccessStatus.ROOT_CONNECTING) return

        _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.ROOT_CONNECTING) }
        scope.launch {
            val result = RootCpuReader.probe()
            if (_state.value.cpuAccessMode != CpuAccessMode.ROOT) return@launch
            rootGranted = result == RootProbeResult.GRANTED
            identityCache = null
            resetSampler.set(true)
            _state.update {
                it.copy(
                    cpuAccessStatus = when (result) {
                        RootProbeResult.GRANTED -> CpuAccessStatus.ROOT
                        RootProbeResult.DENIED -> CpuAccessStatus.ROOT_DENIED
                        RootProbeResult.UNAVAILABLE -> CpuAccessStatus.ROOT_UNAVAILABLE
                    },
                )
            }
        }
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    private fun tryAutoConnectShizuku() {
        if (_state.value.cpuAccessMode != CpuAccessMode.SHIZUKU) return
        val isRunning = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (isRunning &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        ) {
            bindShizukuService()
        }
    }

    private fun bindShizukuService() {
        if (_state.value.cpuAccessMode != CpuAccessMode.SHIZUKU) return
        if (privilegedService != null ||
            _state.value.cpuAccessStatus == CpuAccessStatus.CONNECTING
        ) {
            return
        }

        _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.CONNECTING) }
        runCatching {
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        }.onFailure {
            _state.update { state ->
                state.copy(cpuAccessStatus = CpuAccessStatus.SHIZUKU_STOPPED)
            }
        }
    }

    private suspend fun monitorLoop() {
        while (scope.isActive) {
            if (resetSampler.getAndSet(false)) cpuSampler.reset()

            val accessMode = _state.value.cpuAccessMode
            val privileged = privilegedService
            val cpuSnapshot = when {
                accessMode == CpuAccessMode.ROOT && rootGranted -> readRootCpu()
                accessMode == CpuAccessMode.ROOT -> readDirectCpu()
                privileged != null -> readPrivilegedCpu(privileged)
                else -> readDirectCpu()
            }

            val cpu = cpuSampler.sample(
                rawStat = cpuSnapshot.rawStat,
                frequenciesKhz = cpuSnapshot.frequenciesKhz,
                fallbackCoreCount = cpuSnapshot.coreCount,
                identities = resolveCpuIdentities(cpuSnapshot),
            )
            val battery = batteryReader.read()
            val memory = memoryReader.read()
            val sampledAtMillis = System.currentTimeMillis()

            _state.update { previous ->
                previous.copy(
                    cpu = cpu,
                    cpuLoadHistory = updateCpuLoadHistory(
                        history = previous.cpuLoadHistory,
                        usage = cpu.overallUsage,
                        nowMillis = sampledAtMillis,
                    ),
                    chargePowerHistory = updateChargePowerHistory(
                        history = previous.chargePowerHistory,
                        wasCharging = previous.battery.isCharging,
                        isCharging = battery.isCharging,
                        powerWatts = battery.estimatedPowerWatts,
                        nowMillis = sampledAtMillis,
                    ),
                    battery = battery,
                    memory = memory,
                    cpuAccessStatus = if (accessMode == CpuAccessMode.ROOT) {
                        if (cpuSnapshot.source == CpuIdentitySource.ROOT) {
                            CpuAccessStatus.ROOT
                        } else {
                            previous.cpuAccessStatus
                        }
                    } else {
                        resolveAccessStatus(
                            current = previous.cpuAccessStatus,
                            hasDirectUsage = cpuSnapshot.source == CpuIdentitySource.DIRECT &&
                                cpuSnapshot.rawStat.isNotBlank(),
                            hasPrivilegedService =
                                cpuSnapshot.source == CpuIdentitySource.SHIZUKU,
                        )
                    },
                    updatedAtMillis = sampledAtMillis,
                )
            }
            withTimeoutOrNull(SAMPLE_INTERVAL_MILLIS) {
                refreshRequests.receive()
            }
        }
    }

    private fun readRootCpu(): CpuSnapshot {
        val readout = RootCpuReader.readSnapshot()
        if (readout == null) {
            rootGranted = false
            identityCache = null
            resetSampler.set(true)
            _state.update { it.copy(cpuAccessStatus = CpuAccessStatus.ROOT_DENIED) }
            return readDirectCpu()
        }
        return CpuSnapshot(
            rawStat = readout.rawStat,
            coreCount = readout.coreCount,
            frequenciesKhz = readout.frequenciesKhz,
            source = CpuIdentitySource.ROOT,
        )
    }

    private fun readDirectCpu(): CpuSnapshot {
        val coreCount = LinuxCpuReader.readCoreCount().coerceIn(1, MAX_CORES)
        return CpuSnapshot(
            rawStat = LinuxCpuReader.readCpuStat(),
            coreCount = coreCount,
            frequenciesKhz = LinuxCpuReader.readCoreFrequencies(coreCount),
            source = CpuIdentitySource.DIRECT,
        )
    }

    private fun readPrivilegedCpu(service: IPrivilegedMonitor): CpuSnapshot {
        return try {
            val coreCount = service.readCoreCount().coerceIn(1, MAX_CORES)
            CpuSnapshot(
                rawStat = service.readCpuStat().orEmpty(),
                coreCount = coreCount,
                frequenciesKhz = service.readCoreFrequencies(coreCount),
                source = CpuIdentitySource.SHIZUKU,
            )
        } catch (_: RemoteException) {
            privilegedService = null
            identityCache = null
            resetSampler.set(true)
            readDirectCpu()
        }
    }

    private fun resolveCpuIdentities(snapshot: CpuSnapshot): List<CpuIdentity> {
        identityCache?.takeIf {
            it.source == snapshot.source && it.coreCount == snapshot.coreCount
        }?.let { return it.identities }

        val identities = if (snapshot.source == CpuIdentitySource.ROOT) {
            RootCpuReader.readProfile(snapshot.coreCount)?.let { profile ->
                CpuArchitectureDetector.detect(
                    coreCount = snapshot.coreCount,
                    midrs = profile.midrs,
                    cpuInfo = profile.cpuInfo,
                    capacities = profile.capacities,
                    maxFrequenciesKhz = profile.maxFrequenciesKhz,
                )
            } ?: unknownCpuIdentities(snapshot.coreCount)
        } else {
            unknownCpuIdentities(snapshot.coreCount)
        }

        identityCache = CpuIdentityCache(
            source = snapshot.source,
            coreCount = snapshot.coreCount,
            identities = identities,
        )
        return identities
    }

    private fun unknownCpuIdentities(coreCount: Int): List<CpuIdentity> =
        List(coreCount) {
            CpuIdentity(
                architecture = null,
                role = CpuCoreRole.UNKNOWN,
            )
        }

    private fun resolveAccessStatus(
        current: CpuAccessStatus,
        hasDirectUsage: Boolean,
        hasPrivilegedService: Boolean,
    ): CpuAccessStatus {
        if (hasPrivilegedService) return CpuAccessStatus.SHIZUKU
        if (hasDirectUsage) return CpuAccessStatus.DIRECT
        return when (current) {
            CpuAccessStatus.CONNECTING,
            CpuAccessStatus.SHIZUKU_STOPPED,
            CpuAccessStatus.DENIED,
            -> current

            else -> CpuAccessStatus.NEEDS_SHIZUKU
        }
    }

    override fun close() {
        scope.cancel()
        refreshRequests.close()
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        disconnectShizuku(removeService = true)
        rootGranted = false
        identityCache = null
    }

    private fun disconnectShizuku(removeService: Boolean = false) {
        if (privilegedService != null) {
            runCatching {
                Shizuku.unbindUserService(
                    userServiceArgs,
                    userServiceConnection,
                    removeService,
                )
            }
        }
        privilegedService = null
    }

    private data class CpuSnapshot(
        val rawStat: String,
        val coreCount: Int,
        val frequenciesKhz: LongArray,
        val source: CpuIdentitySource,
    )

    private data class CpuIdentityCache(
        val source: CpuIdentitySource,
        val coreCount: Int,
        val identities: List<CpuIdentity>,
    )

    private enum class CpuIdentitySource {
        DIRECT,
        SHIZUKU,
        ROOT,
    }

    private companion object {
        const val SHIZUKU_PERMISSION_REQUEST = 2026
        const val SAMPLE_INTERVAL_MILLIS = 1_000L
        const val MAX_CORES = 64
        const val PREFERENCES_NAME = "monitor_preferences"
        const val ACCESS_MODE_KEY = "cpu_access_mode"
    }
}
