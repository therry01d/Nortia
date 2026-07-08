package com.therry.nortia.util

import com.therry.nortia.data.Holiday

/**
 * Feriados de México, generados por año sin backend.
 * Incluye fechas fijas y las religiosas móviles calculadas a partir del
 * Domingo de Pascua (algoritmo de computus gregoriano).
 */
object Holidays {

    private val cache = HashMap<Int, Map<String, List<Holiday>>>()

    // (mes 1-12, día, nombre)
    private val fixed = listOf(
        Triple(1, 1, "Año Nuevo"),
        Triple(2, 5, "Día de la Constitución"),
        Triple(3, 21, "Natalicio de Benito Juárez"),
        Triple(5, 1, "Día del Trabajo"),
        Triple(9, 16, "Día de la Independencia"),
        Triple(11, 2, "Día de Muertos"),
        Triple(11, 20, "Revolución Mexicana"),
        Triple(12, 12, "Día de la Virgen de Guadalupe"),
        Triple(12, 25, "Navidad")
    )

    // (desplazamiento en días respecto al Domingo de Pascua, nombre)
    private val movable = listOf(
        -46 to "Miércoles de Ceniza",
        -7 to "Domingo de Ramos",
        -3 to "Jueves Santo",
        -2 to "Viernes Santo",
        0 to "Domingo de Pascua"
    )

    fun forYear(year: Int): Map<String, List<Holiday>> = cache.getOrPut(year) {
        val map = HashMap<String, MutableList<Holiday>>()
        fun add(date: String, name: String) {
            map.getOrPut(date) { mutableListOf() }.add(Holiday(date, name))
        }
        fixed.forEach { (m, d, name) -> add(formatDate(year, m - 1, d), name) }
        val easter = easterSunday(year)
        movable.forEach { (offset, name) -> add(addDays(easter, offset), name) }
        map
    }

    fun forDate(dateStr: String): List<Holiday> {
        val year = dateStr.substringBefore("-").toIntOrNull() ?: return emptyList()
        return forYear(year)[dateStr] ?: emptyList()
    }

    /** Algoritmo anónimo gregoriano (computus): devuelve la fecha del Domingo de Pascua. */
    private fun easterSunday(year: Int): String {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return formatDate(year, month - 1, day)
    }
}
