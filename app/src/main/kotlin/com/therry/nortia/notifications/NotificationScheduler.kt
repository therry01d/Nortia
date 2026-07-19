package com.therry.nortia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.therry.nortia.data.Event
import com.therry.nortia.util.DateTimeUtils

object NotificationScheduler {

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_EVENT_TITLE = "extra_event_title"
    const val EXTRA_EVENT_DESCRIPTION = "extra_event_description"
    const val EXTRA_EVENT_TIME = "extra_event_time"

    fun triggerAtMillis(event: Event): Long =
        DateTimeUtils.combineDateAndTime(event.date, event.time)

    fun schedule(context: Context, event: Event, triggerAtMillisOverride: Long? = null) {
        val triggerAtMillis = triggerAtMillisOverride ?: triggerAtMillis(event)
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val pendingIntent = buildPendingIntent(context, event)

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

    fun cancel(context: Context, event: Event) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, event))
    }

    private fun buildPendingIntent(context: Context, event: Event): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_EVENT_TITLE, event.title)
            putExtra(EXTRA_EVENT_DESCRIPTION, event.description)
            putExtra(EXTRA_EVENT_TIME, event.time)
        }
        return PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
