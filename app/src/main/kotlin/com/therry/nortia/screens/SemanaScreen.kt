package com.therry.nortia.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.ui.components.ItemCard
import com.therry.nortia.ui.components.SectionLabel
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.util.DateTimeUtils

@Composable
fun SemanaScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = DateTimeUtils.today()
    val weekDays = (0..6).map { offset -> DateTimeUtils.addDays(today, offset) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        weekDays.forEach { day ->
            val dayItems = items
                .filter { it.date == day && !(it.type == ItemType.TAREA && it.done) }
                .sortedWith(compareBy({ it.time == null }, { it.time }))

            item(key = "label-$day") {
                SectionLabel(DateTimeUtils.formatDayHeader(day))
            }

            if (dayItems.isEmpty()) {
                item(key = "empty-$day") {
                    Text(
                        text = "Sin nada agendado",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
}
