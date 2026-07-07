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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.viewmodel.AgendaViewModel

@Composable
fun AgendaScreen(viewModel: AgendaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var descriptionInput by remember { mutableStateOf(TextFieldValue("")) }
    var timeInput by remember { mutableStateOf(TextFieldValue("")) }

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
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.agenda_add_event_content_description)
                )
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
                    Text(stringResource(R.string.agenda_empty))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
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
                title = { Text(stringResource(R.string.dialog_add_event_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text(stringResource(R.string.dialog_field_title)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text(stringResource(R.string.dialog_field_description)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = timeInput,
                            onValueChange = { timeInput = it },
                            label = { Text(stringResource(R.string.dialog_field_time)) },
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
                                    time = timeInput.text.ifBlank { "00:00" }
                                )
                                titleInput = TextFieldValue("")
                                descriptionInput = TextFieldValue("")
                                timeInput = TextFieldValue("")
                                showDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.dialog_save))
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
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
                    text = stringResource(R.string.agenda_event_time, event.time),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.agenda_delete_event_content_description)
                )
            }
        }
    }
}
