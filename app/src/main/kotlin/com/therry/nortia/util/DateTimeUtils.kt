package com.therry.nortia.util

import java.util.Calendar

data class CalendarDay(
    val dateMillis: Long,
    val dayOfMonth: Int,
    val inCurrentMonth: Boolean
)

object DateTimeUtils {

    private const val DEFAULT_TIME = "09:00"

    val MESES = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    val DIAS = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
    val DIAS_CORTOS = listOf("L", "M", "X", "J", "V", "S", "D")

    fun today(): Long = startOfDay(System.currentTimeMillis())

    fun startOfDay(millis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    /** Combina un [dateMillis] (inicio del día) con una hora "HH:mm"; usa 09:00 si [time] es null. */
    fun combineDateAndTime(dateMillis: Long, time: String?): Long {
        val effectiveTime = time ?: DEFAULT_TIME
        val parts = effectiveTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfDay(dateMillis)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return calendar.timeInMillis
    }

    fun addMonths(monthAnchorMillis: Long, delta: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = monthAnchorMillis
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        return calendar.timeInMillis
    }

    fun formatMonthYear(monthAnchorMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = monthAnchorMillis }
        val month = MESES[calendar.get(Calendar.MONTH)]
        return "$month ${calendar.get(Calendar.YEAR)}"
    }

    fun formatLongDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val dow = DIAS[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = MESES[calendar.get(Calendar.MONTH)]
        return "$dow, $day de $month"
    }

    fun formatShortDate(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = MESES[calendar.get(Calendar.MONTH)]
        return "$day $month"
    }

    /** Convierte "HH:mm" (24h) a un par (hora 12h, "am"/"pm"). */
    fun to12Hour(time: String): Pair<String, String> {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val ampm = if (hour < 12) "am" else "pm"
        var hour12 = hour % 12
        if (hour12 == 0) hour12 = 12
        return "$hour12:${minute.toString().padStart(2, '0')}" to ampm
    }

    /** Grilla de 7xN empezando en lunes, incluyendo días del mes anterior/siguiente para completar semanas. */
    fun monthGrid(monthAnchorMillis: Long): List<CalendarDay> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = monthAnchorMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Calendar.DAY_OF_WEEK: domingo=1 ... sábado=7. Queremos lunes=0.
        val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val monthStart = calendar.timeInMillis

        val days = mutableListOf<CalendarDay>()

        for (i in firstDayOfWeek downTo 1) {
            val c = Calendar.getInstance().apply {
                timeInMillis = monthStart
                add(Calendar.DAY_OF_MONTH, -i)
            }
            days += CalendarDay(c.timeInMillis, c.get(Calendar.DAY_OF_MONTH), inCurrentMonth = false)
        }

        for (dayNum in 1..daysInMonth) {
            val c = Calendar.getInstance().apply {
                timeInMillis = monthStart
                set(Calendar.DAY_OF_MONTH, dayNum)
            }
            days += CalendarDay(c.timeInMillis, dayNum, inCurrentMonth = true)
        }

        val trailing = (7 - days.size % 7) % 7
        for (i in 1..trailing) {
            val c = Calendar.getInstance().apply {
                timeInMillis = monthStart
                set(Calendar.DAY_OF_MONTH, daysInMonth)
                add(Calendar.DAY_OF_MONTH, i)
            }
            days += CalendarDay(c.timeInMillis, c.get(Calendar.DAY_OF_MONTH), inCurrentMonth = false)
        }

        return days
    }
}
