package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.data.Priority
import com.therry.nortia.data.Repeat
import com.therry.nortia.ui.components.EmptyState
import com.therry.nortia.ui.components.ItemCard
import com.therry.nortia.ui.components.SectionLabel
import com.therry.nortia.ui.theme.Accent
import com.therry.nortia.ui.theme.Hairline
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.ui.theme.Personal
import com.therry.nortia.util.DateTimeUtils
import com.therry.nortia.util.RecurrenceUtils

private enum class TareaFilter(val label: String) {
    PENDIENTES("Pendientes"),
    HOY("Hoy"),
    HECHAS("Hechas")
}

private val priorityRank = mapOf(Priority.ALTA to 0, Priority.MEDIA to 1, Priority.BAJA to 2)

private sealed class TareaRow {
    data class SectionHeader(val label: String, val color: Color) : TareaRow()
    data class TaskRow(val item: Item) : TareaRow()
}

/** Fecha "efectiva" para agrupar/ordenar: la próxima ocurrencia si es recurrente. */
private fun effectiveDate(item: Item, today: Long): Long? =
    if (item.repeat == Repeat.NINGUNO) item.date
    else RecurrenceUtils.nextOccurrenceAtOrAfter(item, today) ?: item.date

private fun buildRows(tareas: List<Item>, today: Long): List<TareaRow> {
    val rows = mutableListOf<TareaRow>()
    var lastGroup = ""
    for (tarea in tareas) {
        val date = effectiveDate(tarea, today)
        val group = when {
            tarea.done -> "Completadas"
            date == null -> "Sin fecha"
            date < today -> "Atrasadas"
            date == today -> "Hoy"
            else -> "Próximas"
        }
        if (group != lastGroup) {
            rows.add(TareaRow.SectionHeader(group, if (group == "Atrasadas") Personal else Muted))
            lastGroup = group
        }
        rows.add(TareaRow.TaskRow(tarea))
    }
    return rows
}

@Composable
fun TareasScreen(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(TareaFilter.PENDIENTES) }
    val today = DateTimeUtils.today()

    var tareas = items.filter { it.type == ItemType.TAREA }
    tareas = when (filter) {
        TareaFilter.PENDIENTES -> tareas.filter { !it.done }
        TareaFilter.HOY -> tareas.filter { !it.done && effectiveDate(it, today) == today }
        TareaFilter.HECHAS -> tareas.filter { it.done }
    }
    tareas = tareas.sortedWith(
        compareBy(
            { effectiveDate(it, today) ?: Long.MAX_VALUE },
            { priorityRank[it.priority] ?: 3 }
        )
    )

    val rows = buildRows(tareas, today)

    Column(modifier = modifier.fillMaxSize()) {
        SegmentedControl(
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (rows.isEmpty()) {
            if (filter == TareaFilter.HECHAS) {
                EmptyState("📋", "Aún nada completado", "Las tareas terminadas aparecen aquí.")
            } else {
                EmptyState("🎯", "Sin tareas", "Toca + para crear una tarea.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                itemsIndexed(
                    items = rows,
                    key = { index, row ->
                        when (row) {
                            is TareaRow.SectionHeader -> "header-$index"
                            is TareaRow.TaskRow -> row.item.id
                        }
                    }
                ) { _, row ->
                    when (row) {
                        is TareaRow.SectionHeader -> SectionLabel(row.label, color = row.color)
                        is TareaRow.TaskRow -> ItemCard(
                            item = row.item,
                            onClick = { onItemClick(row.item) },
                            onToggleDone = { onToggleDone(row.item) },
                            modifier = Modifier.padding(bottom = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedControl(
    selected: TareaFilter,
    onSelect: (TareaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, Hairline, shape)
            .padding(3.dp)
    ) {
        TareaFilter.entries.forEach { f ->
            val isSel = f == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSel) Accent else Color.Transparent)
                    .clickable { onSelect(f) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = f.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSel) Color.White else Muted
                )
            }
        }
    }
}
