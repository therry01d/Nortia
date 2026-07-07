package com.therry.nortia.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.therry.nortia.R
import com.therry.nortia.backup.BackupManager
import com.therry.nortia.data.Event
import com.therry.nortia.util.todayString
import com.therry.nortia.viewmodel.AgendaViewModel
import kotlinx.coroutines.launch

private enum class AgendaTab { HOY, CALENDARIO, TAREAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: AgendaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    var tab by remember { mutableStateOf(AgendaTab.HOY) }
    var tareaFilter by remember { mutableStateOf(TareaFilter.PENDIENTES) }
    var selectedCalendarDay by remember { mutableStateOf(todayString()) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun showMessage(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }
    val savedMsg = stringResource(R.string.toast_saved)
    val deletedMsg = stringResource(R.string.toast_deleted)

    val context = LocalContext.current
    val exportedMsg = stringResource(R.string.toast_exported)
    val exportErrorMsg = stringResource(R.string.error_export_failed)
    val importErrorMsg = stringResource(R.string.error_import_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching {
                    val json = BackupManager.exportJson(events)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.isSuccess
                showMessage(if (ok) exportedMsg else exportErrorMsg)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                        ?: error("no content")
                    BackupManager.importJson(json)
                }
                result.onSuccess { imported ->
                    viewModel.importEvents(imported)
                    showMessage(context.getString(R.string.toast_imported, imported.size))
                }.onFailure {
                    showMessage(importErrorMsg)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("nortia-backup-${todayString()}.json") }) {
                        Text("⬇️")
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Text("⬆️")
                    }
                }
            )
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            NavigationBar {
                NavigationBarItem(
                    selected = tab == AgendaTab.HOY,
                    onClick = { tab = AgendaTab.HOY },
                    icon = { Text("☀️") },
                    label = { Text(stringResource(R.string.tab_hoy)) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == AgendaTab.CALENDARIO,
                    onClick = { tab = AgendaTab.CALENDARIO },
                    icon = { Text("🗓️") },
                    label = { Text(stringResource(R.string.tab_calendario)) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == AgendaTab.TAREAS,
                    onClick = { tab = AgendaTab.TAREAS },
                    icon = { Text("✓") },
                    label = { Text(stringResource(R.string.tab_tareas)) },
                    colors = navColors
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingEvent = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                AgendaTab.HOY -> HoyScreen(
                    events = events,
                    onOpen = { editingEvent = it; showDialog = true },
                    onToggleDone = viewModel::toggleDone
                )
                AgendaTab.CALENDARIO -> CalendarioScreen(
                    events = events,
                    selectedDay = selectedCalendarDay,
                    onSelectedDayChange = { selectedCalendarDay = it },
                    onOpen = { editingEvent = it; showDialog = true },
                    onToggleDone = viewModel::toggleDone
                )
                AgendaTab.TAREAS -> TareasScreen(
                    events = events,
                    filter = tareaFilter,
                    onFilterChange = { tareaFilter = it },
                    onOpen = { editingEvent = it; showDialog = true },
                    onToggleDone = viewModel::toggleDone
                )
            }
        }
    }

    if (showDialog) {
        EventEditDialog(
            initial = editingEvent,
            defaultDate = if (tab == AgendaTab.CALENDARIO) selectedCalendarDay else todayString(),
            onDismiss = { showDialog = false },
            onSave = { event ->
                viewModel.save(event)
                showDialog = false
                showMessage(savedMsg)
            },
            onDelete = editingEvent?.let { ev ->
                {
                    viewModel.delete(ev)
                    showDialog = false
                    showMessage(deletedMsg)
                }
            },
            onValidationError = { showMessage(it) }
        )
    }
}
