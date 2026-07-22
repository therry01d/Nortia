package com.therry.nortia

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
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
        requestNextMissingPermission()
    }

    /**
     * Pide los permisos especiales de notificaciones de a uno por vez: si se
     * lanzan los tres juntos (alarma exacta, pantalla completa, batería) al
     * abrir la app, es fácil que el usuario solo llegue a resolver el primero
     * y los otros dos queden sin conceder sin que se note. Cada vez que la
     * app vuelve a primer plano (p. ej. al volver de Ajustes) se revisa cuál
     * falta y se pide solo esa, en orden.
     */
    private fun requestNextMissingPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                requestExactAlarmPermission()
                return
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                requestFullScreenIntentPermission()
                return
            }
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            requestIgnoreBatteryOptimizations()
        }
    }

    private fun onBellClicked() {
        if (hasNotificationPermission()) {
            // Ya está habilitado; llevar al usuario a los ajustes para que pueda desactivarlo si quiere.
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            safeStartActivity(intent)
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

    private fun requestExactAlarmPermission() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        safeStartActivity(intent)
    }

    /**
     * En Android 14+ mostrar la actividad de pantalla completa sobre el lock
     * screen requiere un permiso especial que el usuario debe habilitar a mano
     * (no hay diálogo del sistema); sin él, el recordatorio se degrada a una
     * notificación normal aunque el dispositivo esté bloqueado.
     */
    private fun requestFullScreenIntentPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:$packageName")
        }
        safeStartActivity(intent)
    }

    /**
     * Muchos fabricantes (Xiaomi, Samsung, Huawei, etc.) matan las alarmas de
     * apps en segundo plano para ahorrar batería, aunque tengan el permiso de
     * alarma exacta: por eso los recordatorios pueden andar bien al principio
     * y dejar de sonar días después. Pedir esta exclusión reduce ese riesgo.
     */
    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        safeStartActivity(intent)
    }

    /**
     * Algunas versiones de fabricantes no traen la pantalla de Ajustes que
     * corresponde a ciertas acciones especiales (ej. permiso de pantalla
     * completa en Android 14): evita que la app crashee si no existe.
     */
    private fun safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("Nortia", "No se pudo abrir la pantalla de ajustes: ${intent.action}", e)
        }
    }
}
