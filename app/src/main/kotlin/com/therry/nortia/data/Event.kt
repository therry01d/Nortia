package com.therry.nortia.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: EventType,
    val title: String,
    val date: String? = null,
    val time: String? = null,
    val category: Category,
    val priority: Priority? = null,
    val note: String = "",
    val done: Boolean = false,
    val remind: Boolean = false,
    val remindBeforeMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)
