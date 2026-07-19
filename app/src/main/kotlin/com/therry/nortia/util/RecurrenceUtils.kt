package com.therry.nortia.util

import com.therry.nortia.data.Item
import com.therry.nortia.data.Repeat
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
        }
    }

    /** Próxima fecha de ocurrencia >= [from]. Null si el item no tiene fecha asignada. */
    fun nextOccurrenceAtOrAfter(item: Item, from: Long): Long? {
        val anchor = item.date ?: return null
        if (anchor >= from) return anchor
        if (item.repeat == Repeat.NINGUNO) return null

        var candidate = anchor
        var guard = 0
        while (candidate < from && guard < 10_000) {
            candidate = advance(candidate, item.repeat)
            guard++
        }
        return candidate
    }

    private fun advance(day: Long, repeat: Repeat): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = day }
        when (repeat) {
            Repeat.DIARIO -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            Repeat.SEMANAL -> calendar.add(Calendar.DAY_OF_MONTH, 7)
            Repeat.MENSUAL -> calendar.add(Calendar.MONTH, 1)
            Repeat.ANUAL -> calendar.add(Calendar.YEAR, 1)
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
