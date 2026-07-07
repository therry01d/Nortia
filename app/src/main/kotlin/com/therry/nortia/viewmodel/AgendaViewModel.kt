package com.therry.nortia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgendaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EventRepository(AppDatabase.getInstance(application).eventDao())

    val events: StateFlow<List<Event>> = repository.events.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addEvent(title: String, description: String, time: String) {
        viewModelScope.launch {
            repository.addEvent(
                Event(
                    title = title,
                    description = description,
                    date = System.currentTimeMillis(),
                    time = time
                )
            )
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.removeEvent(event)
        }
    }
}
