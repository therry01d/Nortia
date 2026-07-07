package com.therry.nortia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventRepository
import com.therry.nortia.notifications.ReminderScheduler
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

    fun save(event: Event) {
        viewModelScope.launch {
            val saved = if (event.id == 0) {
                val newId = repository.addEvent(event)
                event.copy(id = newId.toInt())
            } else {
                repository.updateEvent(event)
                event
            }
            ReminderScheduler.schedule(getApplication(), saved)
        }
    }

    fun toggleDone(event: Event) {
        viewModelScope.launch {
            val updated = event.copy(done = !event.done)
            repository.updateEvent(updated)
            if (updated.done) {
                ReminderScheduler.cancel(getApplication(), updated)
            } else {
                ReminderScheduler.schedule(getApplication(), updated)
            }
        }
    }

    fun delete(event: Event) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), event)
            repository.removeEvent(event)
        }
    }
}
