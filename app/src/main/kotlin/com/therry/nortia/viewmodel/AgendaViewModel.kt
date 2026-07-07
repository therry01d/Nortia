package com.therry.nortia.viewmodel

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventOccurrence
import com.therry.nortia.data.EventRepository
import com.therry.nortia.notifications.ReminderScheduler
import com.therry.nortia.widget.NortiaWidget
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

    private suspend fun refreshWidget() {
        NortiaWidget().updateAll(getApplication())
    }

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
            refreshWidget()
        }
    }

    fun toggleDone(occurrence: EventOccurrence) {
        viewModelScope.launch {
            val event = occurrence.event
            val date = occurrence.occurrenceDate
            val nowDone = date !in event.completedDates
            val newCompletedDates = if (nowDone) {
                event.completedDates + date
            } else {
                event.completedDates - date
            }
            val updated = event.copy(completedDates = newCompletedDates)
            repository.updateEvent(updated)
            if (nowDone) {
                ReminderScheduler.scheduleNextAfterFiring(getApplication(), updated, date)
            } else {
                ReminderScheduler.schedule(getApplication(), updated)
            }
            refreshWidget()
        }
    }

    fun delete(event: Event) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), event)
            repository.removeEvent(event)
            refreshWidget()
        }
    }

    /** Agrega los eventos importados como filas nuevas (no reemplaza los existentes). */
    fun importEvents(imported: List<Event>) {
        viewModelScope.launch {
            imported.forEach { event ->
                val newId = repository.addEvent(event.copy(id = 0))
                val saved = event.copy(id = newId.toInt())
                ReminderScheduler.schedule(getApplication(), saved)
            }
            refreshWidget()
        }
    }
}
