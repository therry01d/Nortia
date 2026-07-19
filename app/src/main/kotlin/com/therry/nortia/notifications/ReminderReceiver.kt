package com.therry.nortia.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.therry.nortia.R
import com.therry.nortia.data.Event

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra(NotificationScheduler.EXTRA_EVENT_ID, -1)
        if (eventId == -1) return

        when (intent.action) {
            ACTION_DISMISS -> {
                NotificationManagerCompat.from(context).cancel(eventId)
            }
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(eventId)
                val event = eventFromIntent(intent, eventId)
                NotificationScheduler.schedule(
                    context,
                    event,
                    triggerAtMillisOverride = System.currentTimeMillis() + SNOOZE_MILLIS
                )
            }
            else -> showReminder(context, intent, eventId)
        }
    }

    private fun showReminder(context: Context, intent: Intent, eventId: Int) {
        // Algunos fabricantes no despiertan el dispositivo salvo que se sostenga
        // brevemente un wake lock al recibir la alarma con la pantalla apagada.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Nortia:ReminderWakeLock"
        )
        wakeLock.acquire(10_000L)

        val title = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TITLE).orEmpty()
        val description = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_DESCRIPTION).orEmpty()
        val time = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TIME).orEmpty()

        val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(NotificationScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationScheduler.EXTRA_EVENT_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_EVENT_DESCRIPTION, description)
            putExtra(NotificationScheduler.EXTRA_EVENT_TIME, time)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            eventId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = actionPendingIntent(context, eventId, ACTION_DISMISS, title, description, time)
        val snoozePendingIntent = actionPendingIntent(context, eventId, ACTION_SNOOZE, title, description, time)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { context.getString(R.string.notification_title) })
            .setContentText(if (description.isNotBlank()) "$description · $time" else time)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .addAction(0, context.getString(R.string.action_dismiss), dismissPendingIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(eventId, notification)

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun eventFromIntent(intent: Intent, eventId: Int): Event = Event(
        id = eventId,
        title = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TITLE).orEmpty(),
        description = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_DESCRIPTION).orEmpty(),
        date = System.currentTimeMillis(),
        time = intent.getStringExtra(NotificationScheduler.EXTRA_EVENT_TIME).orEmpty()
    )

    private fun actionPendingIntent(
        context: Context,
        eventId: Int,
        action: String,
        title: String,
        description: String,
        time: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationScheduler.EXTRA_EVENT_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_EVENT_DESCRIPTION, description)
            putExtra(NotificationScheduler.EXTRA_EVENT_TIME, time)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action$eventId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_DISMISS = "com.therry.nortia.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.therry.nortia.ACTION_SNOOZE"
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L
    }
}
