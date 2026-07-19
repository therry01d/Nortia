package com.therry.nortia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.therry.nortia.data.Item
import com.therry.nortia.util.DateTimeUtils

object NotificationScheduler {

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TYPE = "extra_item_type"
    const val EXTRA_ITEM_TITLE = "extra_item_title"
    const val EXTRA_ITEM_NOTE = "extra_item_note"
    const val EXTRA_ITEM_TIME = "extra_item_time"

    /** Momento exacto del disparo: fecha+hora del item menos el aviso previo. Null si no tiene fecha. */
    fun triggerAtMillis(item: Item): Long? {
        val date = item.date ?: return null
        val base = DateTimeUtils.combineDateAndTime(date, item.time)
        return base - item.remindBeforeMinutes * 60_000L
    }

    fun schedule(context: Context, item: Item, triggerAtMillisOverride: Long? = null) {
        if (!item.remind || item.done) {
            cancel(context, item)
            return
        }
        val triggerAtMillis = triggerAtMillisOverride ?: triggerAtMillis(item)
        if (triggerAtMillis == null || triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val pendingIntent = buildPendingIntent(context, item)

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            // El usuario revocó el permiso de alarmas exactas entre el chequeo y la llamada.
        }
    }

    fun cancel(context: Context, item: Item) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, item))
    }

    private fun buildPendingIntent(context: Context, item: Item): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_ITEM_TYPE, item.type.name)
            putExtra(EXTRA_ITEM_TITLE, item.title)
            putExtra(EXTRA_ITEM_NOTE, item.note)
            putExtra(EXTRA_ITEM_TIME, item.time)
        }
        return PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
