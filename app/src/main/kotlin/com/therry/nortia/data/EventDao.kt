package com.therry.nortia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: Event): Long

    @Delete
    suspend fun delete(event: Event)

    @Query("SELECT * FROM events ORDER BY date ASC, time ASC")
    fun getAll(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE date >= :fromDate ORDER BY date ASC, time ASC")
    suspend fun getUpcoming(fromDate: Long): List<Event>
}
