package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Item
import com.therry.nortia.ui.components.ItemCard
import com.therry.nortia.ui.components.categoryColor
import com.therry.nortia.ui.theme.Accent
import com.therry.nortia.ui.theme.Hairline
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.util.DateTimeUtils

@Composable
fun CalendarioScreen(
    items: List<Item>,
    calCursor: Long,
    onCalCursorChange: (Long) -> Unit,
    selDay: Long,
    onSelDayChange: (Long) -> Unit,
    onItemClick: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val marksByDay = remember(items) {
        items.filter { it.date != null }
            .groupBy { it.date!! }
            .mapValues { (_, list) -> list.map { it.category }.distinct() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            CalendarHeader(
                calCursor = calCursor,
                onPrev = { onCalCursorChange(DateTimeUtils.addMonths(calCursor, -1)) },
                onNext = { onCalCursorChange(DateTimeUtils.addMonths(calCursor, 1)) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            WeekDaysRow()
            Spacer(modifier = Modifier.height(3.dp))
            MonthGrid(
                calCursor = calCursor,
                selDay = selDay,
                marksByDay = marksByDay,
                onDayClick = onSelDayChange
            )
            Text(
                text = DateTimeUtils.formatLongDate(selDay).replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Muted,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
            )
        }

        val dayItems = items.filter { it.date == selDay }
        if (dayItems.isEmpty()) {
            item {
                Text(
                    text = "Nada este día. Toca + para agregar.",
                    fontSize = 13.sp,
                    color = Muted,
                    modifier = Modifier.padding(vertical = 28.dp)
                )
            }
        } else {
            items(dayItems, key = { it.id }) { it2 ->
                ItemCard(
                    item = it2,
                    onClick = { onItemClick(it2) },
                    onToggleDone = { onToggleDone(it2) },
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(calCursor: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateTimeUtils.formatMonthYear(calCursor),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalNavButton("‹", onPrev)
            CalNavButton("›", onNext)
        }
    }
}

@Composable
private fun CalNavButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, Hairline, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 16.sp)
    }
}

@Composable
private fun WeekDaysRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DateTimeUtils.DIAS_CORTOS.forEach { d ->
            Text(
                text = d,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    calCursor: Long,
    selDay: Long,
    marksByDay: Map<Long, List<com.therry.nortia.data.Category>>,
    onDayClick: (Long) -> Unit
) {
    val days = DateTimeUtils.monthGrid(calCursor)
    val today = DateTimeUtils.today()
    val weeks = days.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val isToday = day.dateMillis == today
                    val isSelected = day.dateMillis == selDay
                    val marks = marksByDay[day.dateMillis].orEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) Accent else MaterialTheme.colorScheme.background)
                            .clickable { onDayClick(day.dateMillis) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                fontSize = 14.sp,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = when {
                                    isSelected -> androidx.compose.ui.graphics.Color.White
                                    isToday -> Accent
                                    !day.inCurrentMonth -> Hairline
                                    else -> MaterialTheme.colorScheme.onBackground
                                }
                            )
                            if (marks.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    marks.take(3).forEach { category ->
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(
                                                    if (isSelected) androidx.compose.ui.graphics.Color.White
                                                    else categoryColor(category),
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
