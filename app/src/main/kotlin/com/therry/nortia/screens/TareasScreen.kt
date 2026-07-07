package com.therry.nortia.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventOccurrence
import com.therry.nortia.data.EventType
import com.therry.nortia.data.Priority
import com.therry.nortia.data.RepeatRule
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.util.nextOccurrenceOnOrAfter
import com.therry.nortia.util.todayString

enum class TareaFilter { PENDIENTES, HOY, HECHAS }

private enum class TareaGroup { ATRASADAS, SIN_FECHA, HOY, PROXIMAS, COMPLETADAS }

private const val NO_DATE_SENTINEL = ""
private const val SORT_FALLBACK = "9999-99-99"

private fun priorityRank(priority: Priority?): Int = when (priority) {
    Priority.ALTA -> 0
    Priority.MEDIA -> 1
    Priority.BAJA -> 2
    null -> 3
}

private fun representativeOccurrence(event: Event, today: String): EventOccurrence {
    val date = event.date
    val occurrenceDate = when {
        date == null -> NO_DATE_SENTINEL
        event.repeat == RepeatRule.NINGUNO -> date
        else -> nextOccurrenceOnOrAfter(event, today) ?: date
    }
    return EventOccurrence(event, occurrenceDate)
}

private fun groupOf(occurrence: EventOccurrence, today: String): TareaGroup = when {
    occurrence.isDone -> TareaGroup.COMPLETADAS
    occurrence.event.date == null -> TareaGroup.SIN_FECHA
    occurrence.occurrenceDate < today -> TareaGroup.ATRASADAS
    occurrence.occurrenceDate == today -> TareaGroup.HOY
    else -> TareaGroup.PROXIMAS
}

private sealed interface TareaRow {
    data class SectionLabel(val group: TareaGroup) : TareaRow
    data class ItemRow(val occurrence: EventOccurrence) : TareaRow
}

private fun buildTareaRows(events: List<Event>, filter: TareaFilter, today: String): List<TareaRow> {
    var occurrences = events
        .filter { it.type == EventType.TAREA }
        .map { representativeOccurrence(it, today) }
    occurrences = when (filter) {
        TareaFilter.PENDIENTES -> occurrences.filter { !it.isDone }
        TareaFilter.HOY -> occurrences.filter { !it.isDone && it.occurrenceDate == today }
        TareaFilter.HECHAS -> occurrences.filter { it.isDone }
    }
    val sorted = occurrences.sortedWith(
        compareBy(
            { it.occurrenceDate.ifBlank { SORT_FALLBACK } },
            { priorityRank(it.event.priority) }
        )
    )
    val rows = mutableListOf<TareaRow>()
    var lastGroup: TareaGroup? = null
    sorted.forEach { occurrence ->
        val group = groupOf(occurrence, today)
        if (group != lastGroup) {
            rows += TareaRow.SectionLabel(group)
            lastGroup = group
        }
        rows += TareaRow.ItemRow(occurrence)
    }
    return rows
}

@Composable
private fun groupLabel(group: TareaGroup): String = when (group) {
    TareaGroup.ATRASADAS -> stringResource(R.string.section_atrasadas)
    TareaGroup.SIN_FECHA -> stringResource(R.string.section_sin_fecha)
    TareaGroup.HOY -> stringResource(R.string.section_hoy)
    TareaGroup.PROXIMAS -> stringResource(R.string.section_proximas)
    TareaGroup.COMPLETADAS -> stringResource(R.string.section_completadas)
}

@Composable
fun TareasScreen(
    events: List<Event>,
    filter: TareaFilter,
    onFilterChange: (TareaFilter) -> Unit,
    onOpen: (Event) -> Unit,
    onToggleDone: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = todayString()
    val rows = buildTareaRows(events, filter, today)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp, 16.dp, 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton(stringResource(R.string.filter_pendientes), filter == TareaFilter.PENDIENTES, Modifier.weight(1f)) {
                onFilterChange(TareaFilter.PENDIENTES)
            }
            FilterButton(stringResource(R.string.filter_hoy), filter == TareaFilter.HOY, Modifier.weight(1f)) {
                onFilterChange(TareaFilter.HOY)
            }
            FilterButton(stringResource(R.string.filter_hechas), filter == TareaFilter.HECHAS, Modifier.weight(1f)) {
                onFilterChange(TareaFilter.HECHAS)
            }
        }

        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (filter == TareaFilter.HECHAS) "📋" else "🎯", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(if (filter == TareaFilter.HECHAS) R.string.tareas_empty_hechas_title else R.string.tareas_empty_pend_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(if (filter == TareaFilter.HECHAS) R.string.tareas_empty_hechas_subtitle else R.string.tareas_empty_pend_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows) { row ->
                    when (row) {
                        is TareaRow.SectionLabel -> Text(
                            groupLabel(row.group),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (row.group == TareaGroup.ATRASADAS) PrioridadAlta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        is TareaRow.ItemRow -> EventCard(
                            occurrence = row.occurrence,
                            onOpen = { onOpen(row.occurrence.event) },
                            onToggleDone = { onToggleDone(row.occurrence) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
    ) {
        Text(text)
    }
}
