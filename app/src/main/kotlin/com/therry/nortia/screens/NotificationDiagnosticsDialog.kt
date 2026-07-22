package com.therry.nortia.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

private data class DiagCheck(val label: String, val ok: Boolean, val applicable: Boolean = true)

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
 * Dispara la notificación exactamente por el mismo camino que un recordatorio
 * real (mismo receiver, mismo canal), pero de forma inmediata sin pasar por
 * AlarmManager. Sirve para distinguir si el problema está en cómo se arma la
 * notificación (canal/sonido) o en que el sistema no llega a disparar la alarma
 * programada (permisos, ahorro de batería del fabricante).
 */
@Composable
fun NotificationDiagnosticsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }

    val checks = remember(refreshKey) {
        listOf(
            DiagCheck("Permiso de notificaciones", hasNotificationPermission(context)),
            DiagCheck("Alarmas exactas", hasExactAlarm(context)),
            DiagCheck(
                "Pantalla completa (Android 14+)",
                hasFullScreenIntent(context),
                applicable = Build.VERSION.SDK_INT >= 34
            ),
            DiagCheck("Excluida del ahorro de batería", hasBatteryExemption(context)),
            DiagCheck("Sonido configurado en el canal", channelHasSound(context))
        )
    }
    val allOk = checks.all { !it.applicable || it.ok }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notificaciones") },
        text = {
            Column {
                Text(
                    text = if (allOk) {
                        "Todo parece en orden. Probá el botón de abajo: si suena y vibra, los recordatorios deberían funcionar."
                    } else {
                        "Hay algo pendiente. Tocá cada ítem con ❌ para resolverlo en Ajustes."
                    },
                    fontSize = 13.sp,
                    color = Muted
                )
                Spacer(modifier = Modifier.height(12.dp))
                checks.forEach { check ->
                    if (check.applicable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(check.label, fontSize = 14.sp)
                            Text(
                                text = if (check.ok) "✅" else "❌",
                                color = if (check.ok) PrioridadBaja else PrioridadAlta,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Si tocás \"Probar ahora\" y no pasa nada (ni sonido, ni vibración, " +
                        "ni aviso en pantalla), avisale a quien te dio la app — el problema está " +
                        "en la app misma. Si la prueba funciona pero los recordatorios que " +
                        "programás no suenan cuando corresponde, revisá que todo arriba tenga ✅.",
                    fontSize = 12.sp,
                    color = Muted
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    ) { Text("Ajustes", fontSize = 12.sp) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { refreshKey++ }
                    ) { Text("Actualizar", fontSize = 12.sp) }
                }
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
