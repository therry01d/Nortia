package com.therry.nortia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.therry.nortia.data.Item
import com.therry.nortia.data.Repeat
import com.therry.nortia.util.DateTimeUtils
import com.therry.nortia.util.RecurrenceUtils

object NotificationScheduler {

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TYPE = "extra_item_type"
    const val EXTRA_ITEM_TITLE = "extra_item_title"
    const val EXTRA_ITEM_NOTE = "extra_item_note"
    const val EXTRA_ITEM_TIME = "extra_item_time"
    const val EXTRA_ITEM_DATE = "extra_item_date"
    const val EXTRA_ITEM_REPEAT = "extra_item_repeat"
    const val EXTRA_ITEM_REMIND_BEFORE = "extra_item_remind_before"

    /**
     * Momento exacto del disparo: fecha+hora de la próxima ocurrencia menos el
     * aviso previo. Null si no hay ocurrencia futura.
     *
     * Para items recurrentes NO basta con tomar la ocurrencia >= [searchFrom]:
     * la de hoy puede tener su hora (menos el aviso) ya en el pasado. En ese caso
     * hay que seguir avanzando a la ocurrencia siguiente hasta encontrar una cuyo
     * disparo caiga realmente en el futuro; si no, schedule() la descartaba y el
     * recordatorio recurrente quedaba sin reprogramarse (dejaba de sonar).
     */
    fun triggerAtMillis(item: Item, searchFrom: Long = DateTimeUtils.today()): Long? {
        if (item.repeat == Repeat.NINGUNO) {
            val date = item.date ?: return null
            val base = DateTimeUtils.combineDateAndTime(date, item.time)
            return base - item.remindBeforeMinutes * 60_000L
        }

        val now = System.currentTimeMillis()
        var from = searchFrom
        var guard = 0
        while (guard < 3660) { // ~10 años de ocurrencias diarias, tope de seguridad
            val occurrenceDate = RecurrenceUtils.nextOccurrenceAtOrAfter(item, from) ?: return null
            val base = DateTimeUtils.combineDateAndTime(occurrenceDate, item.time)
            val trigger = base - item.remindBeforeMinutes * 60_000L
            if (trigger > now) return trigger
            from = DateTimeUtils.addDays(occurrenceDate, 1)
            guard++
        }
        return null
    }

    fun schedule(context: Context, item: Item, triggerAtMillisOverride: Long? = null) {
        if (!item.remind || item.done) {
            cancel(context, item)
            return
        }
        val triggerAtMillis = triggerAtMillisOverride ?: triggerAtMillis(item)
        if (triggerAtMillis == null || triggerAtMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, item)

        // En Android 12+ las alarmas exactas requieren un permiso especial que
        // suele venir denegado. Si no lo tenemos, NO abandonamos: caemos a una
        // alarma inexacta (setAndAllowWhileIdle) que igual se dispara aunque el
        // dispositivo esté en Doze, con un margen de pocos minutos. Antes esta
        // función hacía return y el recordatorio no se programaba en absoluto,
        // por eso las notificaciones no aparecían cuando debían.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // El permiso de alarmas exactas se revocó entre el chequeo y la
            // llamada: reintentamos con la variante inexacta, que no lo necesita.
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
                // Sin nada más que hacer; se reprogramará en el próximo arranque.
            }
        }
    }

    /** Programa la ocurrencia siguiente de un item recurrente, después de que ya sonó la de hoy. */
    fun scheduleNextRecurrence(context: Context, item: Item) {
        if (item.repeat == Repeat.NINGUNO) return
        val tomorrow = DateTimeUtils.addDays(DateTimeUtils.today(), 1)
        val triggerAtMillis = triggerAtMillis(item, searchFrom = tomorrow) ?: return
        schedule(context, item, triggerAtMillisOverride = triggerAtMillis)
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
            item.date?.let { putExtra(EXTRA_ITEM_DATE, it) }
            putExtra(EXTRA_ITEM_REPEAT, item.repeat.name)
            putExtra(EXTRA_ITEM_REMIND_BEFORE, item.remindBeforeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
