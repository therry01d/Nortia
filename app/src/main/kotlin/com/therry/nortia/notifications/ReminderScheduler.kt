package com.therry.nortia.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.data.RepeatRule
import com.therry.nortia.util.addDays
import com.therry.nortia.util.dateToCalendar
import com.therry.nortia.util.nextOccurrenceOnOrAfter
import com.therry.nortia.util.todayString
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val REMINDER_CHANNEL_ID = "reminders"

object ReminderScheduler {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notif_channel_description) }
        manager.createNotificationChannel(channel)
    }

    /** Programa el recordatorio para la próxima ocurrencia del evento a partir de hoy. */
    fun schedule(context: Context, event: Event) {
        if (!event.remind) {
            cancel(context, event)
            return
        }
        val targetDate = nextOccurrenceOnOrAfter(event, todayString())
        if (targetDate == null) {
            cancel(context, event)
            return
        }
        scheduleForDate(context, event, targetDate)
    }

    /** Programa el recordatorio para la ocurrencia siguiente a la que recién sonó. */
    fun scheduleNextAfterFiring(context: Context, event: Event, firedDate: String) {
        if (!event.remind || event.repeat == RepeatRule.NINGUNO) {
            cancel(context, event)
            return
        }
        val next = nextOccurrenceOnOrAfter(event, addDays(firedDate, 1))
        if (next == null) {
            cancel(context, event)
            return
        }
        scheduleForDate(context, event, next)
    }

    private fun scheduleForDate(context: Context, event: Event, occurrenceDate: String) {
        cancel(context, event)
        val fireAt = fireTimeMillis(event, occurrenceDate) ?: return
        val delay = fireAt - System.currentTimeMillis()
        if (delay <= 0) return

        val data = workDataOf(
            ReminderWorker.KEY_EVENT_ID to event.id,
            ReminderWorker.KEY_OCCURRENCE_DATE to occurrenceDate,
            ReminderWorker.KEY_TITLE to event.title,
            ReminderWorker.KEY_BODY to buildBody(event)
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName(event.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, event: Event) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(event.id))
    }

    private fun uniqueName(id: Int) = "reminder_$id"

    private fun fireTimeMillis(event: Event, occurrenceDate: String): Long? {
        val time = event.time ?: "09:00"
        val (hour, minute) = time.split(":").map { it.toInt() }
        val cal = dateToCalendar(occurrenceDate)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.add(Calendar.MINUTE, -event.remindBeforeMinutes)
        return cal.timeInMillis
    }

    private fun buildBody(event: Event): String =
        listOfNotNull(event.time, event.note.ifBlank { null }).joinToString(" · ")
}
