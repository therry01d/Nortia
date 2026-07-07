package com.therry.nortia.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val events: Flow<List<Event>> = eventDao.getAll()

    suspend fun addEvent(event: Event) = eventDao.insert(event)

    suspend fun removeEvent(event: Event) = eventDao.delete(event)
}
