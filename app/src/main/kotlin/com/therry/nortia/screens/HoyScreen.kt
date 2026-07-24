package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.data.Repeat
import com.therry.nortia.ui.components.EmptyState
import com.therry.nortia.ui.components.ItemCard
import com.therry.nortia.ui.components.SectionLabel
import com.therry.nortia.ui.theme.Personal
import com.therry.nortia.util.DateTimeUtils
import com.therry.nortia.util.RecurrenceUtils
import java.util.Calendar

fun hoyGreeting(): String {
    val calendar = Calendar.getInstance()
    val dow = DateTimeUtils.DIAS[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    return dow.replaceFirstChar { it.uppercase() }
}

fun hoyTitle(): String {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = DateTimeUtils.MESES[calendar.get(Calendar.MONTH)]
    return "$day $month"
}

@Composable
fun HoyScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = DateTimeUtils.today()
    val nowHm = run {
        val c = Calendar.getInstance()
        "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    val dueTasks = items
        .filter {
            it.type == ItemType.TAREA && !it.done && it.repeat == Repeat.NINGUNO &&
                it.date != null && it.date < today
        }
        .sortedBy { it.date }

    val todayList = items
        .filter { RecurrenceUtils.occursOn(it, today) && !(it.type == ItemType.TAREA && it.done) }

    val untimed = todayList.filter { it.time == null }
    val timed = todayList.filter { it.time != null }.sortedBy { it.time }

    if (dueTasks.isEmpty() && todayList.isEmpty()) {
        EmptyState(
            emoji = "🌤️",
            title = "Día despejado",
            subtitle = "No tienes nada agendado hoy.\nToca + para agregar algo.",
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        if (dueTasks.isNotEmpty()) {
            item { SectionLabel("Atrasadas", color = Personal) }
            items(dueTasks, key = { "due-${it.id}" }) { it2 ->
                ItemCard(
                    item = it2,
                    onClick = { onItemClick(it2) },
                    onToggleDone = { onToggleDone(it2) },
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
        }

        item { SectionLabel("Hoy") }

        items(untimed, key = { "u-${it.id}" }) { it2 ->
            ItemCard(
                item = it2,
                onClick = { onItemClick(it2) },
                onToggleDone = { onToggleDone(it2) },
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }

        val firstFutureIndex = timed.indexOfFirst { (it.time ?: "") > nowHm }
        if (timed.isNotEmpty()) {
            if (firstFutureIndex == -1) {
                items(timed, key = { "t-${it.id}" }) { it2 ->
                    ItemCard(
                        item = it2,
                        onClick = { onItemClick(it2) },
                        onToggleDone = { onToggleDone(it2) },
                        modifier = Modifier.padding(bottom = 9.dp)
                    )
                }
                item { NowLine() }
            } else {
                timed.forEachIndexed { index, it2 ->
                    if (index == firstFutureIndex) {
                        item { NowLine() }
                    }
                    item(key = "t-${it2.id}") {
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
}

@Composable
private fun NowLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(Personal, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(Personal.copy(alpha = 0.55f))
        )
        Text(
            text = "AHORA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Personal
        )
    }
}
