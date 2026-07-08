package com.therry.nortia.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.therry.nortia.data.RepeatRule
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.util.Holidays
import com.therry.nortia.util.nowTimeString
import com.therry.nortia.util.occursOn
import com.therry.nortia.util.todayString

private enum class HoySection { ATRASADAS, HOY }

private sealed interface HoyRow {
    data class SectionLabel(val section: HoySection) : HoyRow
    data object NowDivider : HoyRow
    data class ItemRow(val occurrence: EventOccurrence) : HoyRow
}

private fun buildHoyRows(events: List<Event>, today: String, nowTime: String): List<HoyRow> {
    val todays = events
        .filter { occursOn(it, today) }
        .filterNot { it.type == EventType.TAREA && today in it.completedDates }
        .map { EventOccurrence(it, today) }
    val overdue = events
        .filter {
            it.type == EventType.TAREA && it.repeat == RepeatRule.NINGUNO &&
                it.date != null && it.date < today && it.date !in it.completedDates
        }
        .sortedBy { it.date }
        .map { EventOccurrence(it, it.date!!) }

    if (todays.isEmpty() && overdue.isEmpty()) return emptyList()

    val rows = mutableListOf<HoyRow>()
    if (overdue.isNotEmpty()) {
        rows += HoyRow.SectionLabel(HoySection.ATRASADAS)
        overdue.forEach { rows += HoyRow.ItemRow(it) }
    }
    val timed = todays.filter { it.event.time != null }.sortedBy { it.event.time }
    val untimed = todays.filter { it.event.time == null }

    rows += HoyRow.SectionLabel(HoySection.HOY)
    untimed.forEach { rows += HoyRow.ItemRow(it) }

    var placed = false
    timed.forEach {
        if (!placed && it.event.time!! > nowTime) {
            rows += HoyRow.NowDivider
            placed = true
        }
        rows += HoyRow.ItemRow(it)
    }
    if (!placed && timed.isNotEmpty()) {
        rows += HoyRow.NowDivider
    }
    return rows
}

@Composable
fun HoyScreen(
    events: List<Event>,
    onOpen: (Event) -> Unit,
    onToggleDone: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = todayString()
    val nowTime = nowTimeString()
    val rows = buildHoyRows(events, today, nowTime)
    val holidays = Holidays.forDate(today)

    if (rows.isEmpty() && holidays.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌤️", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.hoy_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.hoy_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(holidays) { holiday -> HolidayCard(holiday = holiday) }
        items(rows) { row ->
            when (row) {
                is HoyRow.SectionLabel -> Text(
                    stringResource(
                        if (row.section == HoySection.ATRASADAS) R.string.section_atrasadas else R.string.section_hoy
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (row.section == HoySection.ATRASADAS) PrioridadAlta else MaterialTheme.colorScheme.onSurfaceVariant
                )
                HoyRow.NowDivider -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.section_ahora),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is HoyRow.ItemRow -> EventCard(
                    occurrence = row.occurrence,
                    onOpen = { onOpen(row.occurrence.event) },
                    onToggleDone = { onToggleDone(row.occurrence) }
                )
            }
        }
    }
}
