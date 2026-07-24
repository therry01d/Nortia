package com.therry.nortia.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.therry.nortia.data.ItemType
import com.therry.nortia.data.Repeat
import com.therry.nortia.notifications.NotificationHelper
import com.therry.nortia.notifications.NotificationScheduler
import com.therry.nortia.notifications.ReminderReceiver
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.ui.theme.PrioridadBaja
import com.therry.nortia.util.DateTimeUtils

private data class DiagCheck(
    val label: String,
    val ok: Boolean,
    val applicable: Boolean = true,
    /** Pantalla de Ajustes que resuelve este ítem, si aplica. */
    val fixIntent: Intent? = null
)

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasExactAlarm(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return alarmManager.canScheduleExactAlarms()
}

private fun hasFullScreenIntent(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return true
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    return notificationManager.canUseFullScreenIntent()
}

private fun hasBatteryExemption(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun channelHasSound(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
    return channel != null &&
        channel.sound != null &&
        channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
}

private fun buildChecks(context: Context): List<DiagCheck> = listOf(
    DiagCheck(
        label = "Permiso de notificaciones",
        ok = hasNotificationPermission(context),
        fixIntent = appSettingsIntent(context)
    ),
    DiagCheck(
        label = "Alarmas exactas",
        ok = hasExactAlarm(context),
        applicable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        fixIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else null
    ),
    DiagCheck(
        label = "Pantalla completa (Android 14+)",
        ok = hasFullScreenIntent(context),
        applicable = Build.VERSION.SDK_INT >= 34,
        fixIntent = if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else null
    ),
    DiagCheck(
        label = "Excluida del ahorro de batería",
        ok = hasBatteryExemption(context),
        fixIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    ),
    DiagCheck(
        label = "Sonido configurado en el canal",
        ok = channelHasSound(context),
        fixIntent = appSettingsIntent(context)
    )
)

private fun sendTestNotification(context: Context) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(NotificationScheduler.EXTRA_ITEM_ID, 999999)
        putExtra(NotificationScheduler.EXTRA_ITEM_TYPE, ItemType.RECORDATORIO.name)
        putExtra(NotificationScheduler.EXTRA_ITEM_TITLE, "Prueba de Nortia")
        putExtra(NotificationScheduler.EXTRA_ITEM_NOTE, "Si ves y escuchás esto, las notificaciones funcionan bien.")
        putExtra(NotificationScheduler.EXTRA_ITEM_TIME, null as String?)
        putExtra(NotificationScheduler.EXTRA_ITEM_DATE, DateTimeUtils.today())
        putExtra(NotificationScheduler.EXTRA_ITEM_REPEAT, Repeat.NINGUNO.name)
        putExtra(NotificationScheduler.EXTRA_ITEM_REMIND_BEFORE, 0)
    }
    context.sendBroadcast(intent)
}

/**
 * Herramienta de autodiagnóstico accesible desde la campana: muestra el estado
 * real de cada requisito de las notificaciones (con ✅/❌), permite tocar cada
 * ítem pendiente para abrir su pantalla de Ajustes, y dispara una notificación
 * de prueba por el mismo camino que un recordatorio real (mismo receiver, mismo
 * canal) pero de inmediato, sin pasar por AlarmManager. Así se distingue si el
 * problema está en cómo se arma la notificación o en la programación de la alarma.
 */
@Composable
fun NotificationDiagnosticsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val checks = remember(refreshKey) { buildChecks(context) }
    val allOk = checks.all { !it.applicable || it.ok }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notificaciones") },
        text = {
            Column {
                Text(
                    text = if (allOk) {
                        "Todo está en orden. Probá el botón de abajo: si suena y vibra, los recordatorios deberían funcionar."
                    } else {
                        "Hay algo pendiente. Tocá cada ítem con ❌ para resolverlo en Ajustes."
                    },
                    fontSize = 13.sp,
                    color = Muted
                )
                Spacer(modifier = Modifier.height(12.dp))
                checks.forEach { check ->
                    if (check.applicable) {
                        val clickable = !check.ok && check.fixIntent != null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (clickable) Modifier.clickable {
                                        runCatching { context.startActivity(check.fixIntent) }
                                    } else Modifier
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(check.label, fontSize = 14.sp)
                            Text(
                                text = if (check.ok) "✅" else "❌",
                                color = if (check.ok) PrioridadBaja else PrioridadAlta,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Si tocás \"Probar ahora\" y no pasa nada (ni sonido, ni vibración, " +
                        "ni aviso en pantalla), el problema está en la app. Si la prueba funciona " +
                        "pero los recordatorios que programás no suenan a su hora, revisá que todo " +
                        "arriba tenga ✅.",
                    fontSize = 12.sp,
                    color = Muted
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { refreshKey++ }
                ) { Text("Actualizar estado", fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(onClick = { sendTestNotification(context) }) {
                Text("Probar ahora")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
