package com.therry.nortia.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.therry.nortia.AgendaViewModel
import com.therry.nortia.data.Item
import com.therry.nortia.ui.components.AgendaBottomNav
import com.therry.nortia.ui.components.AppTab
import com.therry.nortia.ui.components.ScreenErrorBoundary
import com.therry.nortia.ui.components.TopHeader
import com.therry.nortia.util.DateTimeUtils

@Composable
fun AgendaScreen(
    viewModel: AgendaViewModel,
    onRequestNotifications: () -> Unit
) {
    val items by viewModel.items.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOY) }
    var calCursor by rememberSaveable { mutableStateOf(DateTimeUtils.today()) }
    var selDay by rememberSaveable { mutableStateOf(DateTimeUtils.today()) }

    var editingItem by remember { mutableStateOf<Item?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val defaultDateForNew = if (currentTab == AppTab.CALENDARIO) selDay else DateTimeUtils.today()

    val (greet, title) = when (currentTab) {
        AppTab.HOY -> hoyGreeting() to hoyTitle()
        AppTab.SEMANA -> "Próximos 7 días" to "Semana"
        AppTab.CALENDARIO -> "Calendario" to "Agenda"
        AppTab.TAREAS -> "Pendientes" to "Tareas"
    }

    Scaffold(
        topBar = {
            TopHeader(
                greet = greet,
                title = title,
                notificationsEnabled = notificationsEnabled,
                onBellClick = onRequestNotifications
            )
        },
        bottomBar = {
            AgendaBottomNav(current = currentTab, onSelect = { currentTab = it })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showEditor = true
                }
            ) {
                Text("+", fontSize = 26.sp)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ScreenErrorBoundary(key = currentTab) {
                when (currentTab) {
                    AppTab.HOY -> HoyScreen(
                        items = items,
                        onItemClick = { editingItem = it; showEditor = true },
                        onToggleDone = { viewModel.toggleDone(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    AppTab.SEMANA -> SemanaScreen(
                        items = items,
                        onItemClick = { editingItem = it; showEditor = true },
                        onToggleDone = { viewModel.toggleDone(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    AppTab.CALENDARIO -> CalendarioScreen(
                        items = items,
                        calCursor = calCursor,
                        onCalCursorChange = { calCursor = it },
                        selDay = selDay,
                        onSelDayChange = { selDay = it },
                        onItemClick = { editingItem = it; showEditor = true },
                        onToggleDone = { viewModel.toggleDone(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    AppTab.TAREAS -> TareasScreen(
                        items = items,
                        onItemClick = { editingItem = it; showEditor = true },
                        onToggleDone = { viewModel.toggleDone(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showEditor) {
        val current = editingItem
        ItemEditorSheet(
            editing = current,
            defaultDate = defaultDateForNew,
            onDismiss = { showEditor = false },
            onSave = { item ->
                if (current != null) viewModel.updateItem(item) else viewModel.addItem(item)
                showEditor = false
            },
            onDelete = current?.let {
                {
                    viewModel.deleteItem(it)
                    showEditor = false
                }
            }
        )
    }
}
