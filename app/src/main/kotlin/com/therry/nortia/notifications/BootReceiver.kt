package com.therry.nortia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * El sistema borra las alarmas exactas de AlarmManager al reiniciar el dispositivo,
 * así que hay que volver a programarlas para todos los eventos futuros.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).eventDao()
                val todayStart = DateTimeUtils.startOfDay(System.currentTimeMillis())
                val upcoming = dao.getUpcoming(todayStart)
                upcoming.forEach { event -> NotificationScheduler.schedule(appContext, event) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
