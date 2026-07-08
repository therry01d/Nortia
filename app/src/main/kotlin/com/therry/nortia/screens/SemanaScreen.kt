package com.therry.nortia.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventOccurrence
import com.therry.nortia.data.EventType
import com.therry.nortia.data.Holiday
import com.therry.nortia.util.addDays
import com.therry.nortia.util.dayNumberOf
import com.therry.nortia.util.occursOn
import com.therry.nortia.util.todayString
import com.therry.nortia.util.weekdayNameOf
import com.therry.nortia.util.Holidays

private sealed interface SemanaRow {
    data class DayHeader(val date: String, val label: String) : SemanaRow
    data class HolidayRow(val holiday: Holiday) : SemanaRow
    data class ItemRow(val occurrence: EventOccurrence) : SemanaRow
}

private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

private fun buildSemanaRows(events: List<Event>, today: String, manana: String): List<SemanaRow> {
    val rows = mutableListOf<SemanaRow>()
    for (offset in 0..6) {
        val day = addDays(today, offset)
        val label = when (day) {
            today -> "Hoy"
            manana -> "Mañana"
            else -> "${cap(weekdayNameOf(day))} ${dayNumberOf(day)}"
        }
        rows += SemanaRow.DayHeader(day, label)
        Holidays.forDate(day).forEach { rows += SemanaRow.HolidayRow(it) }
        events
            .filter { occursOn(it, day) }
            .filterNot { it.type == EventType.TAREA && day in it.completedDates }
            .sortedBy { it.time ?: "99:99" }
            .forEach { rows += SemanaRow.ItemRow(EventOccurrence(it, day)) }
    }
    return rows
}

@Composable
fun SemanaScreen(
    events: List<Event>,
    onOpen: (Event) -> Unit,
    onToggleDone: (EventOccurrence) -> Unit,
    onQuickAdd: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = todayString()
    val manana = addDays(today, 1)
    val rows = buildSemanaRows(events, today, manana)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows) { row ->
            when (row) {
                is SemanaRow.DayHeader -> Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (row.date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { onQuickAdd(row.date) }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_add),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is SemanaRow.HolidayRow -> HolidayCard(holiday = row.holiday)
                is SemanaRow.ItemRow -> EventCard(
                    occurrence = row.occurrence,
                    onOpen = { onOpen(row.occurrence.event) },
                    onToggleDone = { onToggleDone(row.occurrence) }
                )
            }
        }
    }
}
