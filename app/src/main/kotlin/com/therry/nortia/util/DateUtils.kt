package com.therry.nortia.util

import java.util.Calendar

private val MESES = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
private val DIAS = listOf(
    "domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"
)

private fun pad(n: Int) = n.toString().padStart(2, '0')

fun todayString(): String {
    val c = Calendar.getInstance()
    return "${c.get(Calendar.YEAR)}-${pad(c.get(Calendar.MONTH) + 1)}-${pad(c.get(Calendar.DAY_OF_MONTH))}"
}

fun nowTimeString(): String {
    val c = Calendar.getInstance()
    return "${pad(c.get(Calendar.HOUR_OF_DAY))}:${pad(c.get(Calendar.MINUTE))}"
}

fun formatDate(year: Int, month0: Int, day: Int): String = "$year-${pad(month0 + 1)}-${pad(day)}"

fun formatTime(hour: Int, minute: Int): String = "${pad(hour)}:${pad(minute)}"

fun dateToCalendar(dateStr: String): Calendar {
    val c = Calendar.getInstance()
    val (y, m, d) = dateStr.split("-").map { it.toInt() }
    c.set(y, m - 1, d, 0, 0, 0)
    return c
}

fun formatDateLong(dateStr: String): String {
    val c = dateToCalendar(dateStr)
    val dayName = DIAS[c.get(Calendar.DAY_OF_WEEK) - 1]
    val day = c.get(Calendar.DAY_OF_MONTH)
    val month = MESES[c.get(Calendar.MONTH)]
    return "$dayName, $day de $month"
}

fun monthYearLabel(year: Int, month: Int): String = "${MESES[month]} $year"

fun daysInMonth(year: Int, month0: Int): Int {
    val c = Calendar.getInstance()
    c.set(year, month0, 1)
    return c.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun firstWeekdayMondayIndex(year: Int, month0: Int): Int {
    val c = Calendar.getInstance()
    c.set(year, month0, 1)
    val sundayIndexed = c.get(Calendar.DAY_OF_WEEK) - 1
    return (sundayIndexed + 6) % 7
}

fun weekdayNameOf(dateStr: String): String =
    DIAS[dateToCalendar(dateStr).get(Calendar.DAY_OF_WEEK) - 1]

fun dayNumberOf(dateStr: String): Int =
    dateToCalendar(dateStr).get(Calendar.DAY_OF_MONTH)

fun addDays(dateStr: String, days: Int): String {
    val c = dateToCalendar(dateStr)
    c.add(Calendar.DAY_OF_MONTH, days)
    return formatDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
}

fun to12h(time: String): Pair<String, String> {
    val (h, m) = time.split(":").map { it.toInt() }
    val ampm = if (h < 12) "am" else "pm"
    val hour12 = if (h % 12 == 0) 12 else h % 12
    return "$hour12:${pad(m)}" to ampm
}
