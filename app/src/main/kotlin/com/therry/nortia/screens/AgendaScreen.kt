package com.therry.nortia.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.therry.nortia.AgendaViewModel
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: AgendaViewModel) {
    val events by viewModel.events.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var descriptionInput by remember { mutableStateOf(TextFieldValue("")) }
    var timeInput by remember { mutableStateOf(TextFieldValue("")) }
    var selectedDateMillis by remember { mutableStateOf(DateTimeUtils.startOfDay(System.currentTimeMillis())) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.agenda_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text(stringResource(R.string.empty_agenda))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onDelete = { viewModel.deleteEvent(event) }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.add_event_dialog_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text(stringResource(R.string.label_title)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text(stringResource(R.string.label_description)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${stringResource(R.string.label_date)}: ${DateTimeUtils.formatDate(selectedDateMillis)}")
                        }
                        TextField(
                            value = timeInput,
                            onValueChange = { timeInput = it },
                            label = { Text(stringResource(R.string.label_time)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (titleInput.text.isNotBlank()) {
                                viewModel.addEvent(
                                    title = titleInput.text,
                                    description = descriptionInput.text,
                                    date = selectedDateMillis,
                                    time = timeInput.text.ifBlank { "00:00" }
                                )
                                titleInput = TextFieldValue("")
                                descriptionInput = TextFieldValue("")
                                timeInput = TextFieldValue("")
                                selectedDateMillis = DateTimeUtils.startOfDay(System.currentTimeMillis())
                                showDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = DateTimeUtils.startOfDay(it)
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = "${DateTimeUtils.formatDate(event.date)} - ${event.time}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_event))
            }
        }
    }
}
