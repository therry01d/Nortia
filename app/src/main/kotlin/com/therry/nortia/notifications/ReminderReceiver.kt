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
import com.therry.nortia.data.Repeat
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

        val item = itemFromIntent(intent, itemId)
        val time = item.time

        val fullScreenIntent = Intent(context, ReminderFullScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putItemExtras(item)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            itemId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = actionPendingIntent(context, item, ACTION_DISMISS)
        val snoozePendingIntent = actionPendingIntent(context, item, ACTION_SNOOZE)

        val bodyPrefix = if (!time.isNullOrBlank()) "$time · " else ""
        val body = bodyPrefix + item.note.ifBlank { typeLabel(item.type) }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.title.ifBlank { context.getString(R.string.notification_title) })
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

        if (item.repeat != Repeat.NINGUNO) {
            NotificationScheduler.scheduleNextRecurrence(context, item)
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun itemFromIntent(intent: Intent, itemId: Int): Item {
        val date = if (intent.hasExtra(NotificationScheduler.EXTRA_ITEM_DATE)) {
            intent.getLongExtra(NotificationScheduler.EXTRA_ITEM_DATE, DateTimeUtils.today())
        } else {
            DateTimeUtils.today()
        }
        return Item(
            id = itemId,
            type = ItemType.valueOf(
                intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TYPE) ?: ItemType.RECORDATORIO.name
            ),
            title = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TITLE).orEmpty(),
            date = date,
            time = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_TIME),
            category = Category.PERSONAL,
            priority = null,
            note = intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_NOTE).orEmpty(),
            done = false,
            remind = true,
            remindBeforeMinutes = intent.getIntExtra(NotificationScheduler.EXTRA_ITEM_REMIND_BEFORE, 10),
            repeat = Repeat.valueOf(
                intent.getStringExtra(NotificationScheduler.EXTRA_ITEM_REPEAT) ?: Repeat.NINGUNO.name
            )
        )
    }

    private fun Intent.putItemExtras(item: Item) {
        putExtra(NotificationScheduler.EXTRA_ITEM_ID, item.id)
        putExtra(NotificationScheduler.EXTRA_ITEM_TYPE, item.type.name)
        putExtra(NotificationScheduler.EXTRA_ITEM_TITLE, item.title)
        putExtra(NotificationScheduler.EXTRA_ITEM_NOTE, item.note)
        putExtra(NotificationScheduler.EXTRA_ITEM_TIME, item.time)
        item.date?.let { putExtra(NotificationScheduler.EXTRA_ITEM_DATE, it) }
        putExtra(NotificationScheduler.EXTRA_ITEM_REPEAT, item.repeat.name)
        putExtra(NotificationScheduler.EXTRA_ITEM_REMIND_BEFORE, item.remindBeforeMinutes)
    }

    private fun actionPendingIntent(context: Context, item: Item, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putItemExtras(item)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action${item.id}".hashCode(),
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
