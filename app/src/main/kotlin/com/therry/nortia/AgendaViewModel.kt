package com.therry.nortia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventDao
import com.therry.nortia.notifications.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgendaViewModel(
    application: Application,
    private val dao: EventDao
) : AndroidViewModel(application) {

    val events: StateFlow<List<Event>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(title: String, description: String, date: Long, time: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newId = dao.insert(
                Event(title = title, description = description, date = date, time = time)
            )
            val scheduled = Event(
                id = newId.toInt(),
                title = title,
                description = description,
                date = date,
                time = time
            )
            NotificationScheduler.schedule(getApplication(), scheduled)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            dao.delete(event)
            NotificationScheduler.cancel(getApplication(), event)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getInstance(application).eventDao()
                    @Suppress("UNCHECKED_CAST")
                    return AgendaViewModel(application, dao) as T
                }
            }
    }
}
