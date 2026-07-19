package com.therry.nortia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ItemType { EVENTO, TAREA, RECORDATORIO }
enum class Category { TRABAJO, PERSONAL }
enum class Priority { ALTA, MEDIA, BAJA }

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
    val remindBeforeMinutes: Int = 10
)
