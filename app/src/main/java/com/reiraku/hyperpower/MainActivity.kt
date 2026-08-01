package com.reiraku.hyperpower

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.reiraku.hyperpower.notification.MonitorNotificationPreferences
import com.reiraku.hyperpower.notification.PowerMonitorNotification
import com.reiraku.hyperpower.notification.PowerMonitorService
import com.reiraku.hyperpower.ui.DashboardRoute
import com.reiraku.hyperpower.ui.theme.HyperPowerTheme
import com.reiraku.hyperpower.ui.theme.LocalThemeState
import com.reiraku.hyperpower.ui.theme.rememberThemeState

class MainActivity : ComponentActivity() {
    private val canPostNotifications = mutableStateOf(false)
    private var notificationPermissionRequestInFlight = false
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            notificationPermissionRequestInFlight = false
            updateNotificationPermissionState()
            PowerMonitorService.sync(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PowerMonitorNotification.createChannel(this)
        updateNotificationPermissionState()
        setContent {
            val themeState = rememberThemeState()
            CompositionLocalProvider(LocalThemeState provides themeState) {
                HyperPowerTheme(state = themeState.value) {
                    DashboardRoute(
                        notificationPermissionGranted = canPostNotifications.value,
                        onRequestNotificationPermission = {
                            requestNotificationPermission()
                        },
                    )
                }
            }
        }

        if (
            !canPostNotifications.value &&
            !MonitorNotificationPreferences.wasPermissionPrompted(this)
        ) {
            window.decorView.post {
                requestNotificationPermission(forceSystemDialog = true)
            }
        } else {
            PowerMonitorService.sync(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationPermissionState()
        PowerMonitorService.sync(this)
    }

    private fun updateNotificationPermissionState() {
        canPostNotifications.value = PowerMonitorService.canPostNotifications(this)
    }

    private fun requestNotificationPermission(forceSystemDialog: Boolean = false) {
        if (notificationPermissionRequestInFlight) return

        val runtimePermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (runtimePermissionGranted) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    data = "package:$packageName".toUri()
                },
            )
            return
        }

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        if (
            forceSystemDialog ||
            !MonitorNotificationPreferences.wasPermissionPrompted(this) ||
            shouldShowRationale
        ) {
            MonitorNotificationPreferences.markPermissionPrompted(this)
            notificationPermissionRequestInFlight = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    data = "package:$packageName".toUri()
                },
            )
        }
    }
}
