package com.reiraku.hyperpower.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.reiraku.hyperpower.data.DashboardMonitor
import com.reiraku.hyperpower.data.DashboardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PowerMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var monitor: DashboardMonitor

    @Volatile
    private var lastState = DashboardState()

    override fun onCreate() {
        super.onCreate()
        PowerMonitorNotification.createChannel(this)
        startForeground(
            PowerMonitorNotification.NOTIFICATION_ID,
            PowerMonitorNotification.build(this, lastState),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        monitor = DashboardMonitor(applicationContext)
        serviceScope.launch {
            monitor.state.collectLatest { state ->
                lastState = state
                updateNotification()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> rebuildForegroundNotification()
            ACTION_STOP -> {
                MonitorNotificationPreferences.setEnabled(this, false)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun updateNotification() {
        if (!canPostNotifications(this)) {
            stopSelf()
            return
        }
        getSystemService(NotificationManager::class.java).notify(
            PowerMonitorNotification.NOTIFICATION_ID,
            PowerMonitorNotification.build(this, lastState),
        )
    }

    private fun rebuildForegroundNotification() {
        if (!canPostNotifications(this)) {
            stopSelf()
            return
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        startForeground(
            PowerMonitorNotification.NOTIFICATION_ID,
            PowerMonitorNotification.build(this, lastState),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (::monitor.isInitialized) monitor.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_REFRESH =
            "com.reiraku.hyperpower.action.REFRESH_POWER_NOTIFICATION"
        private const val ACTION_STOP =
            "com.reiraku.hyperpower.action.STOP_POWER_NOTIFICATION"

        fun sync(context: Context) {
            if (
                MonitorNotificationPreferences.isEnabled(context) &&
                canPostNotifications(context)
            ) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, PowerMonitorService::class.java),
                )
            } else {
                stop(context)
            }
        }

        fun refresh(context: Context) {
            if (
                !MonitorNotificationPreferences.isEnabled(context) ||
                !canPostNotifications(context)
            ) {
                return
            }
            ContextCompat.startForegroundService(
                context,
                Intent(context, PowerMonitorService::class.java).apply {
                    action = ACTION_REFRESH
                },
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PowerMonitorService::class.java))
        }

        fun createStopPendingIntent(context: Context): PendingIntent =
            PendingIntent.getService(
                context,
                PowerMonitorNotification.NOTIFICATION_ID + 1,
                Intent(context, PowerMonitorService::class.java).apply {
                    action = ACTION_STOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun canPostNotifications(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED &&
                context.getSystemService(NotificationManager::class.java)
                    .areNotificationsEnabled()
    }
}
