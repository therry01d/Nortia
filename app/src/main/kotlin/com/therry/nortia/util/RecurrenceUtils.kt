package com.therry.nortia.util

import com.therry.nortia.data.Item
import com.therry.nortia.data.Repeat
import com.therry.nortia.data.WeekDays
import java.util.Calendar
import kotlin.math.roundToLong

object RecurrenceUtils {

    /** ¿El item ocurre en [day] (inicio de día en millis)? */
    fun occursOn(item: Item, day: Long): Boolean {
        val anchor = item.date ?: return false
        if (day < anchor) return false
        return when (item.repeat) {
            Repeat.NINGUNO -> day == anchor
            Repeat.DIARIO -> true
            Repeat.SEMANAL -> daysBetween(anchor, day) % 7 == 0L
            Repeat.MENSUAL -> sameDayOfMonth(anchor, day)
            Repeat.ANUAL -> sameMonthAndDay(anchor, day)
            Repeat.DIAS_SEMANA -> matchesWeekDay(item.repeatDays, day)
        }
    }

    /** ¿[day] cae en uno de los días marcados en la máscara? */
    private fun matchesWeekDay(mask: Int, day: Long): Boolean {
        if (mask == WeekDays.NONE) return false
        val calendar = Calendar.getInstance().apply { timeInMillis = day }
        return WeekDays.isSelected(mask, WeekDays.bitIndexOf(calendar.get(Calendar.DAY_OF_WEEK)))
    }

    /** Próxima fecha de ocurrencia >= [from]. Null si el item no tiene fecha asignada. */
    fun nextOccurrenceAtOrAfter(item: Item, from: Long): Long? {
        val anchor = item.date ?: return null

        // DIAS_SEMANA va aparte porque, a diferencia del resto, el ancla NO es
        // necesariamente una ocurrencia: el item puede crearse un martes con
        // lunes/miércoles/viernes elegidos. Se avanza día a día hasta el primero
        // que coincida (como mucho 7 intentos).
        if (item.repeat == Repeat.DIAS_SEMANA) {
            if (item.repeatDays == WeekDays.NONE) return null
            var candidate = if (anchor >= from) anchor else from
            repeat(7) {
                if (matchesWeekDay(item.repeatDays, candidate)) return candidate
                candidate = DateTimeUtils.addDays(candidate, 1)
            }
            return null
        }

        if (anchor >= from) return anchor
        if (item.repeat == Repeat.NINGUNO) return null

        val anchorCal = Calendar.getInstance().apply { timeInMillis = anchor }
        val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH)
        val anchorMonth = anchorCal.get(Calendar.MONTH)

        var candidate = anchor
        var guard = 0
        while (candidate < from && guard < 12_000) {
            candidate = advance(candidate, item.repeat, anchorDay, anchorMonth)
            guard++
        }
        return if (candidate >= from) candidate else null
    }

    /**
     * Avanza a la ocurrencia siguiente. Para MENSUAL/ANUAL parte del día del
     * ancla (no del último candidato) para evitar la deriva de Calendar.add:
     * ej. ancla el 31 → sumar mes cae en 28/29 de febrero y quedaría "pegado"
     * al 28 para siempre. Acá se conserva el día del ancla y se saltan los meses
     * (o años, para el 29/02) que no lo contienen, consistente con [occursOn].
     */
    private fun advance(day: Long, repeat: Repeat, anchorDay: Int, anchorMonth: Int): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = day }
        when (repeat) {
            Repeat.DIARIO -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            Repeat.SEMANAL -> calendar.add(Calendar.DAY_OF_MONTH, 7)
            Repeat.MENSUAL -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                do {
                    calendar.add(Calendar.MONTH, 1)
                } while (anchorDay > calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, anchorDay)
            }
            Repeat.ANUAL -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.MONTH, anchorMonth)
                do {
                    calendar.add(Calendar.YEAR, 1)
                } while (anchorDay > calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, anchorDay)
            }
            // DIAS_SEMANA se resuelve antes de llegar acá; avanzar un día es la
            // interpretación segura por si alguna vez llegara.
            Repeat.DIAS_SEMANA -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            Repeat.NINGUNO -> Unit
        }
        return DateTimeUtils.startOfDay(calendar.timeInMillis)
    }

    private fun daysBetween(a: Long, b: Long): Long =
        ((b - a) / 86_400_000.0).roundToLong()

    private fun sameDayOfMonth(anchor: Long, day: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = anchor }
        val cd = Calendar.getInstance().apply { timeInMillis = day }
        return ca.get(Calendar.DAY_OF_MONTH) == cd.get(Calendar.DAY_OF_MONTH)
    }

    private fun sameMonthAndDay(anchor: Long, day: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = anchor }
        val cd = Calendar.getInstance().apply { timeInMillis = day }
        return ca.get(Calendar.MONTH) == cd.get(Calendar.MONTH) &&
            ca.get(Calendar.DAY_OF_MONTH) == cd.get(Calendar.DAY_OF_MONTH)
    }
}
