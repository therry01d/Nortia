package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.therry.nortia.R
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventOccurrence
import com.therry.nortia.ui.theme.Feriado
import com.therry.nortia.util.Holidays
import com.therry.nortia.util.dateToCalendar
import com.therry.nortia.util.daysInMonth
import com.therry.nortia.util.firstWeekdayMondayIndex
import com.therry.nortia.util.formatDate
import com.therry.nortia.util.formatDateLong
import com.therry.nortia.util.monthYearLabel
import com.therry.nortia.util.occursOn
import com.therry.nortia.util.todayString
import java.util.Calendar

private data class CalCell(
    val day: Int,
    val dateKey: String?,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dotColors: List<Color>
)

private fun buildCalendarCells(
    year: Int,
    month0: Int,
    today: String,
    selected: String,
    events: List<Event>
): List<CalCell> {
    val start = firstWeekdayMondayIndex(year, month0)
    val daysInM = daysInMonth(year, month0)
    val prevYear = if (month0 == 0) year - 1 else year
    val prevMonth0 = if (month0 == 0) 11 else month0 - 1
    val prevDays = daysInMonth(prevYear, prevMonth0)

    val cells = mutableListOf<CalCell>()
    for (i in 0 until start) {
        cells += CalCell(prevDays - start + 1 + i, null, false, false, emptyList())
    }
    for (d in 1..daysInM) {
        val key = formatDate(year, month0, d)
        val dotColors = buildList {
            if (Holidays.forDate(key).isNotEmpty()) add(Feriado)
            addAll(events.filter { occursOn(it, key) }.map { categoryColor(it.category) })
        }.distinct()
        cells += CalCell(
            day = d,
            dateKey = key,
            isToday = key == today,
            isSelected = key == selected,
            dotColors = dotColors
        )
    }
    val total = start + daysInM
    val trail = (7 - total % 7) % 7
    for (i in 1..trail) {
        cells += CalCell(i, null, false, false, emptyList())
    }
    return cells
}

@Composable
fun CalendarioScreen(
    events: List<Event>,
    selectedDay: String,
    onSelectedDayChange: (String) -> Unit,
    onOpen: (Event) -> Unit,
    onToggleDone: (EventOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = todayString()
    val todayCal = remember(today) { dateToCalendar(today) }
    var year by remember { mutableStateOf(todayCal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(todayCal.get(Calendar.MONTH)) }

    val cells = remember(year, month, events, selectedDay, today) {
        buildCalendarCells(year, month, today, selectedDay, events)
    }
    val dayItems = events
        .filter { occursOn(it, selectedDay) }
        .sortedBy { it.time ?: "99:99" }
        .map { EventOccurrence(it, selectedDay) }
    val dayHolidays = Holidays.forDate(selectedDay)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 16.dp, 16.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(monthYearLabel(year, month), style = MaterialTheme.typography.titleLarge)
            Row {
                IconButton(onClick = {
                    if (month == 0) { month = 11; year -= 1 } else month -= 1
                }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
                IconButton(onClick = {
                    if (month == 11) { month = 0; year += 1 } else month += 1
                }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            stringArrayResource(R.array.dow_labels).forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        CalendarCell(
                            cell = cell,
                            onClick = { cell.dateKey?.let(onSelectedDayChange) },
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }

        Text(
            formatDateLong(selectedDay),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)
        )

        if (dayItems.isEmpty() && dayHolidays.isEmpty()) {
            Text(
                stringResource(R.string.calendario_day_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayHolidays) { holiday -> HolidayCard(holiday = holiday) }
                items(dayItems) { occurrence ->
                    EventCard(
                        occurrence = occurrence,
                        onOpen = { onOpen(occurrence.event) },
                        onToggleDone = { onToggleDone(occurrence) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(cell: CalCell, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (cell.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(enabled = cell.dateKey != null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                cell.day.toString(),
                color = when {
                    cell.isSelected -> MaterialTheme.colorScheme.onPrimary
                    cell.dateKey == null -> MaterialTheme.colorScheme.outlineVariant
                    cell.isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (cell.dotColors.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    cell.dotColors.take(3).forEach { color ->
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (cell.isSelected) MaterialTheme.colorScheme.onPrimary else color)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
