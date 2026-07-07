package com.therry.nortia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date ASC")
    fun getAll(): Flow<List<Event>>

    @Insert
    suspend fun insert(event: Event)

    @Delete
    suspend fun delete(event: Event)
}
