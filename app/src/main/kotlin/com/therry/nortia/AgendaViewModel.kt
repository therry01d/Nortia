package com.therry.nortia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemDao
import com.therry.nortia.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgendaViewModel(
    application: Application,
    private val dao: ItemDao
) : AndroidViewModel(application) {

    val items: StateFlow<List<Item>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        if (enabled) rescheduleAll()
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            val newId = dao.insert(item)
            NotificationScheduler.schedule(getApplication(), item.copy(id = newId.toInt()))
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            NotificationScheduler.cancel(getApplication(), item)
            dao.update(item)
            NotificationScheduler.schedule(getApplication(), item)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            dao.delete(item)
            NotificationScheduler.cancel(getApplication(), item)
        }
    }

    fun toggleDone(item: Item) {
        val updated = item.copy(done = !item.done)
        viewModelScope.launch {
            dao.update(updated)
            if (updated.done) {
                NotificationScheduler.cancel(getApplication(), updated)
            } else {
                NotificationScheduler.schedule(getApplication(), updated)
            }
        }
    }

    /**
     * Lee directo de Room (no de [items].value) porque ese StateFlow recién arranca
     * a coleccionar cuando alguien lo observa desde la UI; si esto corre antes
     * (p. ej. en el arranque de MainActivity), [items].value todavía sería la
     * lista vacía inicial y no se reprogramaría ningún recordatorio.
     */
    private fun rescheduleAll() {
        viewModelScope.launch {
            dao.getAll().first().forEach { NotificationScheduler.schedule(getApplication(), it) }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getInstance(application).itemDao()
                    @Suppress("UNCHECKED_CAST")
                    return AgendaViewModel(application, dao) as T
                }
            }
    }
}
