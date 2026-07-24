package com.therry.nortia.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.therry.nortia.R

object NotificationHelper {

    // "v2": un canal ya creado en el dispositivo no puede cambiar sonido/vibración
    // por código una vez creado (solo el usuario puede editarlo desde Ajustes), así
    // que un cambio en importancia/sonido/vibración requiere un ID de canal nuevo
    // para que el sistema lo recree con la configuración correcta.
    const val CHANNEL_ID = "event_reminders_v2"

    val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400, 200, 400)

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
                setBypassDnd(false)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }
}
