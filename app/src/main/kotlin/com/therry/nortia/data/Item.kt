package com.therry.nortia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType { EVENTO, TAREA, RECORDATORIO }
enum class Category { TRABAJO, PERSONAL }
enum class Priority { ALTA, MEDIA, BAJA }
enum class Repeat { NINGUNO, DIARIO, SEMANAL, MENSUAL, ANUAL, DIAS_SEMANA }

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: ItemType,
    val title: String,
    /** Inicio del día en millis, o null si la tarea no tiene fecha asignada. */
    val date: Long?,
    /** "HH:mm", o null si no tiene hora (todo el día / sin hora). */
    val time: String?,
    val category: Category,
    /** Solo aplica a TAREA. */
    val priority: Priority?,
    val note: String = "",
    val done: Boolean = false,
    val remind: Boolean = false,
    val remindBeforeMinutes: Int = 10,
    val repeat: Repeat = Repeat.NINGUNO,
    /**
     * Solo aplica a [Repeat.DIAS_SEMANA]: máscara de bits con los días elegidos,
     * bit 0 = lunes … bit 6 = domingo. Se guarda como entero para no necesitar
     * un TypeConverter ni una tabla aparte.
     */
    val repeatDays: Int = 0
)

/** Utilidades para la máscara de días de [Item.repeatDays]. */
object WeekDays {
    const val NONE = 0

    /** Índice de bit (0 = lunes … 6 = domingo) de un Calendar.DAY_OF_WEEK. */
    fun bitIndexOf(calendarDayOfWeek: Int): Int = (calendarDayOfWeek + 5) % 7

    fun isSelected(mask: Int, bitIndex: Int): Boolean = (mask shr bitIndex) and 1 == 1

    fun toggle(mask: Int, bitIndex: Int): Int = mask xor (1 shl bitIndex)
}
