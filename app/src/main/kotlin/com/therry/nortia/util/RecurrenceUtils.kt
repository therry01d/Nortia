package com.therry.nortia.util

import com.therry.nortia.data.Event
import com.therry.nortia.data.RepeatRule
import java.util.Calendar

fun occursOn(event: Event, dateStr: String): Boolean {
    val anchor = event.date ?: return false
    if (dateStr < anchor) return false
    if (dateStr == anchor) return true
    return when (event.repeat) {
        RepeatRule.NINGUNO -> false
        RepeatRule.DIARIO -> true
        RepeatRule.SEMANAL -> dateToCalendar(dateStr).get(Calendar.DAY_OF_WEEK) ==
            dateToCalendar(anchor).get(Calendar.DAY_OF_WEEK)
        RepeatRule.MENSUAL -> matchesMonthly(anchor, dateStr)
        RepeatRule.ANUAL -> matchesYearly(anchor, dateStr)
    }
}

private fun matchesMonthly(anchor: String, dateStr: String): Boolean {
    val anchorCal = dateToCalendar(anchor)
    val dateCal = dateToCalendar(dateStr)
    val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH)
    val lastDayOfDateMonth = dateCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return dateCal.get(Calendar.DAY_OF_MONTH) == minOf(anchorDay, lastDayOfDateMonth)
}

private fun matchesYearly(anchor: String, dateStr: String): Boolean {
    val anchorCal = dateToCalendar(anchor)
    val dateCal = dateToCalendar(dateStr)
    if (anchorCal.get(Calendar.MONTH) != dateCal.get(Calendar.MONTH)) return false
    val anchorDay = anchorCal.get(Calendar.DAY_OF_MONTH)
    val lastDay = dateCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return dateCal.get(Calendar.DAY_OF_MONTH) == minOf(anchorDay, lastDay)
}

/** Próxima fecha (inclusive) en la que ocurre el evento, a partir de [fromDate]. */
fun nextOccurrenceOnOrAfter(event: Event, fromDate: String): String? {
    val anchor = event.date ?: return null
    var cursor = if (anchor > fromDate) anchor else fromDate
    repeat(400) {
        if (occursOn(event, cursor)) return cursor
        cursor = addDays(cursor, 1)
    }
    return null
}
