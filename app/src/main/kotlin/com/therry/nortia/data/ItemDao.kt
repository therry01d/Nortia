package com.therry.nortia.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM items ORDER BY date ASC, time ASC")
    fun getAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE remind = 1 AND done = 0")
    suspend fun getRemindable(): List<Item>

    /**
     * Versión bloqueante para el widget: RemoteViewsFactory corre en un hilo de
     * binder (nunca en el principal) y no admite corrutinas, así que necesita
     * una consulta síncrona.
     */
    @Query("SELECT * FROM items WHERE done = 0")
    fun getPendingBlocking(): List<Item>
}
