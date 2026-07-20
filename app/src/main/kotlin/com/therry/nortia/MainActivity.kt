package com.therry.nortia

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.therry.nortia.notifications.NotificationHelper
import com.therry.nortia.screens.AgendaScreen
import com.therry.nortia.ui.theme.NortiaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        AgendaViewModel.factory(application)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setNotificationsEnabled(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)
        viewModel.setNotificationsEnabled(hasNotificationPermission())
        requestExactAlarmPermissionIfNeeded()
        requestFullScreenIntentPermissionIfNeeded()

        setContent {
            NortiaTheme {
                AgendaScreen(
                    viewModel = viewModel,
                    onRequestNotifications = { onBellClicked() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setNotificationsEnabled(hasNotificationPermission())
    }

    private fun onBellClicked() {
        if (hasNotificationPermission()) {
            // Ya está habilitado; llevar al usuario a los ajustes para que pueda desactivarlo si quiere.
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotificationsEnabled(true)
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    /**
     * En Android 14+ mostrar la actividad de pantalla completa sobre el lock
     * screen requiere un permiso especial que el usuario debe habilitar a mano
     * (no hay diálogo del sistema); sin él, el recordatorio se degrada a una
     * notificación normal aunque el dispositivo esté bloqueado.
     */
    private fun requestFullScreenIntentPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 34) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
