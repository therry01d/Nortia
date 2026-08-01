package com.therry.nortia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.widget.NortiaWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reprograma los recordatorios ante los eventos del sistema que los invalidan o
 * que dan una oportunidad barata de repararlos.
 *
 * El enfoque está tomado de Tasks.org, la app de recordatorios open source más
 * madura del ecosistema: además del reinicio, escucha el desbloqueo del teléfono
 * y el cambio del permiso de alarmas exactas.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        // USER_PRESENT llega en cada desbloqueo: sin freno se releería la base
        // decenas de veces por día sin motivo. Los eventos importantes (reinicio,
        // actualización, cambio de permiso) se atienden siempre.
        val throttled = action == Intent.ACTION_USER_PRESENT
        if (throttled && !shouldRunThrottled(context)) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).itemDao()
                dao.getRemindable().forEach { item -> NotificationScheduler.schedule(appContext, item) }
                // El reinicio también borra la alarma repetitiva del guardián.
                NotificationScheduler.ensureKeeperScheduled(appContext)
                NortiaWidgetProvider.refresh(appContext)
            } catch (e: Throwable) {
                Log.e("Nortia", "Error reprogramando recordatorios ($action)", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Deja pasar como mucho una reprogramación por desbloqueo cada [THROTTLE_MILLIS]. */
    private fun shouldRunThrottled(context: Context): Boolean {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_RUN, 0L)
        val now = System.currentTimeMillis()
        if (now - last < THROTTLE_MILLIS) return false
        prefs.edit().putLong(KEY_LAST_RUN, now).apply()
        return true
    }

    private companion object {
        const val PREFS_NAME = "nortia_reschedule"
        const val KEY_LAST_RUN = "last_run"
        const val THROTTLE_MILLIS = 30 * 60 * 1000L

        /** ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED es API 31; el literal
         *  evita tener que compilar contra la constante en dispositivos anteriores. */
        const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED
        )
    }
}
