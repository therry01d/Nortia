package com.therry.nortia.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.therry.nortia.R
import com.therry.nortia.data.Category
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.util.DateTimeUtils

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(NotificationScheduler.EXTRA_ITEM_ID, -1)
        if (itemId == -1) return

        when (intent.action) {
            ACTION_DISMISS -> {
                NotificationManagerCompat.from(context).cancel(itemId)
            }
            ACTION_SNOOZE -> {
                NotificationManagerCompat.from(context).cancel(itemId)
                val item = itemFromIntent(intent, itemId)
                NotificationScheduler.schedule(
                    context,
                    item,
                    triggerAtMillisOverride = System.currentTimeMillis() + SNOOZE_MILLIS
                )
            }
            else -> showReminder(context, intent, itemId)
        }
    }

    private fun typeLabel(type: ItemType): String = when (type) {
        ItemType.EVENTO -> "Evento"
        ItemType.TAREA -> "Tarea"
        ItemType.RECORDATORIO -> "Recordatorio"
    }

    private fun showReminder(context: Context, intent: Intent, itemId: Int) {
        // Algunos fabricantes no despiertan el dispositivo salvo que se sostenga
        // brevemente un wake lock al recibir la alarma con la pantalla apagada.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Nortia:ReminderWakeLock"
        )
        wakeLock.acquire(10_000L)

        val type = ItemType.valueOf(intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TYPE) ?: ItemType.RECORDATORIO.name)
        val title = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TITLE).orEmpty()
        val note = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_NOTE).orEmpty()
        val time = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TIME)

        val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(NotificationScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(NotificationScheduler.EXTRA_ITEM_TYPE, type.name)
            putExtra(NotificationScheduler.EXTRA_ITEM_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_ITEM_NOTE, note)
            putExtra(NotificationScheduler.EXTRA_ITEM_TIME, time)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            itemId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = actionPendingIntent(context, itemId, ACTION_DISMISS, type, title, note, time)
        val snoozePendingIntent = actionPendingIntent(context, itemId, ACTION_SNOOZE, type, title, note, time)

        val bodyPrefix = if (!time.isNullOrBlank()) "$time · " else ""
        val body = bodyPrefix + note.ifBlank { typeLabel(type) }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.ifBlank { context.getString(R.string.notification_title) })
            .setContentText(body)
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

        NotificationManagerCompat.from(context).notify(itemId, notification)

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun itemFromIntent(intent: Intent, itemId: Int): Item = Item(
        id = itemId,
        type = ItemType.valueOf(intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TYPE) ?: ItemType.RECORDATORIO.name),
        title = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TITLE).orEmpty(),
        date = DateTimeUtils.today(),
        time = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TIME),
        category = Category.PERSONAL,
        priority = null,
        note = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_NOTE).orEmpty(),
        done = false,
        remind = true
    )

    private fun actionPendingIntent(
        context: Context,
        itemId: Int,
        action: String,
        type: ItemType,
        title: String,
        note: String,
        time: String?
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationScheduler.EXTRA_ITEM_ID, itemId)
            putExtra(NotificationScheduler.EXTRA_ITEM_TYPE, type.name)
            putExtra(NotificationScheduler.EXTRA_ITEM_TITLE, title)
            putExtra(NotificationScheduler.EXTRA_ITEM_NOTE, note)
            putExtra(NotificationScheduler.EXTRA_ITEM_TIME, time)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action$itemId".hashCode(),
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
