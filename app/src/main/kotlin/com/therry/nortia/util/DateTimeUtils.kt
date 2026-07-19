package com.therry.nortia.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeUtils {

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

    /** Combines a start-of-day [dateMillis] with an "HH:mm" [time] string into an exact timestamp. */
    fun combineDateAndTime(dateMillis: Long, time: String): Long {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfDay(dateMillis)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return calendar.timeInMillis
    }

    fun formatDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
        return formatter.format(millis)
    }
}
