package com.therry.nortia.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.therry.nortia.data.AppDatabase

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY).orEmpty()
        val eventId = inputData.getInt(KEY_EVENT_ID, -1)
        val occurrenceDate = inputData.getString(KEY_OCCURRENCE_DATE)

        ReminderScheduler.ensureChannel(applicationContext)

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(eventId, notification)
        }

        if (eventId != -1 && occurrenceDate != null) {
            val event = AppDatabase.getInstance(applicationContext).eventDao().getById(eventId)
            if (event != null) {
                ReminderScheduler.scheduleNextAfterFiring(applicationContext, event, occurrenceDate)
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_OCCURRENCE_DATE = "occurrence_date"
    }
}
