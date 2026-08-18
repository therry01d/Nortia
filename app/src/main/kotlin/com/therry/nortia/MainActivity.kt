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

    private val prefs by lazy { getSharedPreferences("nortia_perms", Context.MODE_PRIVATE) }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setNotificationsEnabled(granted)
            // Concedido (o no) el permiso base, seguimos con los especiales.
            promptSpecialPermissionsOnce()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannel(this)
        requestNotificationPermissionThenSpecials()

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
        // Solo refrescamos el estado y reprogramamos: NO relanzamos pantallas de
        // Ajustes en cada resume (eso provocaba un bucle molesto). Los permisos
        // especiales se piden una única vez; después se gestionan desde la campana.
        viewModel.setNotificationsEnabled(hasNotificationPermission())
    }

    /**
     * Paso 1: el permiso de notificaciones (Android 13+) sí tiene diálogo del
     * sistema. Lo pedimos primero; cuando el usuario responde, encadenamos los
     * permisos especiales. En versiones viejas no hace falta y vamos directo.
     */
    private fun requestNotificationPermissionThenSpecials() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setNotificationsEnabled(hasNotificationPermission())
            promptSpecialPermissionsOnce()
        }
    }

    /**
     * Paso 2: los permisos especiales (alarma exacta, pantalla completa,
     * exclusión de batería) no tienen diálogo: hay que mandar al usuario a una
     * pantalla de Ajustes. Para no relanzarlas en bucle, cada una se ofrece una
     * sola vez en la vida de la instalación; si el usuario las ignora, puede
     * volver a abrirlas cuando quiera desde el botón de la campana. Se piden en
     * orden, una por vez, para no encimar tres pantallas de golpe.
     */
    private fun promptSpecialPermissionsOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms() && markPromptedIfNeeded(KEY_EXACT_ALARM)) {
                requestExactAlarmPermission()
                return
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent() && markPromptedIfNeeded(KEY_FULL_SCREEN)) {
                requestFullScreenIntentPermission()
                return
            }
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName) &&
            markPromptedIfNeeded(KEY_BATTERY)
        ) {
            requestIgnoreBatteryOptimizations()
        }
    }

    /** Devuelve true (y marca como ofrecido) solo la primera vez para cada permiso. */
    private fun markPromptedIfNeeded(key: String): Boolean {
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    private fun onBellClicked() {
        // La campana abre el diagnóstico si ya hay permiso de notificaciones;
        // si no, lo pide. El diagnóstico permite reabrir cualquier pantalla de
        // Ajustes que el usuario haya salteado la primera vez.
        if (hasNotificationPermission()) {
            viewModel.setNotificationsEnabled(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    companion object {
        private const val KEY_EXACT_ALARM = "prompted_exact_alarm"
        private const val KEY_FULL_SCREEN = "prompted_full_screen"
        private const val KEY_BATTERY = "prompted_battery"
    }
}
