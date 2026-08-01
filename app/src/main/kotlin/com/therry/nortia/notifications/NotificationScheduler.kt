package com.therry.nortia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.therry.nortia.MainActivity
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
    const val EXTRA_ITEM_REPEAT_DAYS = "extra_item_repeat_days"

    /** Cuánto se pospone un recordatorio al tocar "Posponer". Fuente única de verdad. */
    const val SNOOZE_MILLIS = 10 * 60 * 1000L

    /** Margen mínimo para un aviso "ya mismo", para no programar en el instante exacto. */
    private const val IMMEDIATE_DELAY_MILLIS = 3_000L

    /** requestCode del guardián; muy negativo para no chocar nunca con un id de item. */
    private const val KEEPER_REQUEST_CODE = -424242

    /** Cada cuánto el guardián reprograma todo. */
    private const val KEEPER_INTERVAL_MILLIS = 6 * 60 * 60 * 1000L

    /** Desplazamiento del requestCode del ícono de alarma, para no chocar con el widget. */
    private const val SHOW_INTENT_REQUEST_OFFSET = 1_000_000

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

    /** Momento del evento en sí, sin restarle el aviso previo. */
    private fun eventTimeMillis(item: Item): Long? {
        val date = if (item.repeat == Repeat.NINGUNO) {
            item.date
        } else {
            RecurrenceUtils.nextOccurrenceAtOrAfter(item, DateTimeUtils.today())
        } ?: return null
        return DateTimeUtils.combineDateAndTime(date, item.time)
    }

    fun schedule(context: Context, item: Item, triggerAtMillisOverride: Long? = null) {
        if (!item.remind || item.done) {
            cancel(context, item)
            return
        }
        val requested = triggerAtMillisOverride ?: triggerAtMillis(item) ?: return
        val now = System.currentTimeMillis()

        val triggerAtMillis = when {
            requested > now -> requested
            // El snooze siempre apunta al futuro; si no, no hay nada que hacer.
            triggerAtMillisOverride != null -> return
            else -> {
                // El aviso previo cayó en el pasado, pero el evento todavía no
                // llegó: pasa siempre que se crea algo con menos anticipación que
                // el "avisarme X antes" (ej. evento en 3 min con aviso de 10 min).
                // Antes se descartaba la alarma en silencio y el recordatorio
                // nunca sonaba. Ahora se avisa enseguida.
                val eventTime = eventTimeMillis(item) ?: return
                if (eventTime > now) now + IMMEDIATE_DELAY_MILLIS else return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, item)

        // setAlarmClock es la API para avisos que el usuario espera a una hora
        // concreta: el sistema NUNCA ajusta su entrega y sale de los modos de bajo
        // consumo para entregarla, así que no la afectan ni Doze ni las cuotas de
        // App Standby (en el bucket RARE son 1 alarma/hora y en RESTRICTED 1 al día,
        // que es a donde cae la app tras días sin abrirse).
        //
        // OJO: desde Android 12 SÍ requiere permiso de alarma exacta y lanza
        // SecurityException sin él. Por eso el manifiesto declara USE_EXACT_ALARM,
        // que se concede solo al instalar. El catch de abajo cubre el caso en que
        // aun así no esté disponible.
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent(context, item)),
                pendingIntent
            )
            return
        } catch (e: Exception) {
            Log.w("Nortia", "setAlarmClock falló, se usa el camino alternativo", e)
        }

        // Respaldo por si algún fabricante restringe setAlarmClock.
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
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
                // Sin nada más que hacer; lo recupera el guardián o el próximo arranque.
            }
        }
    }

    /**
     * Lo que se abre si el usuario toca el ícono de alarma del sistema. El
     * requestCode va desplazado porque el widget también crea PendingIntents hacia
     * MainActivity con códigos bajos y, al coincidir el Intent, se pisarían.
     */
    private fun showIntent(context: Context, item: Item): PendingIntent =
        PendingIntent.getActivity(
            context,
            SHOW_INTENT_REQUEST_OFFSET + item.id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Guardián: alarma repetitiva que cada pocas horas vuelve a programar TODOS los
     * recordatorios desde la base. Es la red de seguridad ante el otro fallo de la
     * arquitectura: la cadena de recurrencia se autoperpetúa (cada disparo programa
     * el siguiente), así que un solo disparo perdido la mataba para siempre hasta
     * que el usuario abriera la app. Al ser repetitiva, se vuelve a disparar sola
     * aunque se pierda una vuelta.
     */
    fun ensureKeeperScheduled(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_RESCHEDULE_ALL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KEEPER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + KEEPER_INTERVAL_MILLIS,
            KEEPER_INTERVAL_MILLIS,
            pendingIntent
        )
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

    /**
     * ¿El sistema tiene realmente registrada la alarma de este item? Con
     * FLAG_NO_CREATE el PendingIntent solo se devuelve si ya existe, así que
     * sirve para distinguir "nunca se programó" de "se programó pero no suena".
     */
    fun isScheduled(context: Context, item: Item): Boolean {
        val intent = Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
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
            putExtra(EXTRA_ITEM_REPEAT_DAYS, item.repeatDays)
        }
        return PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
