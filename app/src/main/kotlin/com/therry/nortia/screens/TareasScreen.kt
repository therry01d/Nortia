package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.util.DateTimeUtils
import com.therry.nortia.util.RecurrenceUtils

private enum class DoneFilter(val label: String) {
    PENDIENTES("Pendientes"),
    HECHAS("Hechas")
}

private val typeTabs = listOf(
    ItemType.EVENTO to "Eventos",
    ItemType.TAREA to "Tareas",
    ItemType.RECORDATORIO to "Recordatorios"
)

private val priorityRank = mapOf(Priority.ALTA to 0, Priority.MEDIA to 1, Priority.BAJA to 2)

private sealed class ListaRow {
    data class SectionHeader(val label: String, val color: Color) : ListaRow()
    data class EntryRow(val item: Item) : ListaRow()
}

/** Fecha "efectiva" para agrupar/ordenar: la próxima ocurrencia si es recurrente. */
private fun effectiveDate(item: Item, today: Long): Long? =
    if (item.repeat == Repeat.NINGUNO) item.date
    else RecurrenceUtils.nextOccurrenceAtOrAfter(item, today) ?: item.date

/**
 * Arma la lista plana de filas (encabezados + items) de una sola pasada. Se
 * precalcula para poder emitirla con un único itemsIndexed: mezclar item() dentro
 * de un forEach con una variable mutable fue lo que rompía el compositor.
 */
private fun buildRows(items: List<Item>, today: Long, hechas: Boolean): List<ListaRow> {
    if (hechas) return items.map { ListaRow.EntryRow(it) }

    val rows = mutableListOf<ListaRow>()
    var lastGroup = ""
    for (item in items) {
        val date = effectiveDate(item, today)
        val group = when {
            date == null -> "Sin fecha"
            date < today -> "Atrasadas"
            date == today -> "Hoy"
            else -> "Próximas"
        }
        if (group != lastGroup) {
            rows.add(ListaRow.SectionHeader(group, if (group == "Atrasadas") Personal else Muted))
            lastGroup = group
        }
        rows.add(ListaRow.EntryRow(item))
    }
    return rows
}

private fun emptyStateFor(type: ItemType, hechas: Boolean): Triple<String, String, String> = when {
    hechas -> Triple("📋", "Aún nada completado", "Lo que marques como hecho aparece acá.")
    type == ItemType.EVENTO -> Triple("📅", "Sin eventos", "Toca + para crear un evento.")
    type == ItemType.TAREA -> Triple("🎯", "Sin tareas", "Toca + para crear una tarea.")
    else -> Triple("⏰", "Sin recordatorios", "Toca + para crear un recordatorio.")
}

@Composable
fun TareasScreen(
    items: List<Item>,
    alertedTypes: Set<ItemType>,
    onItemClick: (Item) -> Unit,
    onToggleDone: (Item) -> Unit,
    onTypeSeen: (ItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by rememberSaveable { mutableStateOf(ItemType.TAREA) }
    var filter by rememberSaveable { mutableStateOf(DoneFilter.PENDIENTES) }
    val today = DateTimeUtils.today()

    // Si el usuario está viendo la pestaña que tenía aviso, se da por revisada.
    LaunchedEffect(selectedType, alertedTypes) {
        if (selectedType in alertedTypes) onTypeSeen(selectedType)
    }

    val hechas = filter == DoneFilter.HECHAS
    val filtered = items
        .filter { it.type == selectedType && it.done == hechas }
        .sortedWith(
            compareBy(
                { effectiveDate(it, today) ?: Long.MAX_VALUE },
                { priorityRank[it.priority] ?: 3 }
            )
        )
    val rows = buildRows(filtered, today, hechas)

    Column(modifier = modifier.fillMaxSize()) {
        TypeTabs(
            selected = selectedType,
            alertedTypes = alertedTypes,
            onSelect = { selectedType = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        DoneFilterControl(
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (rows.isEmpty()) {
            val (emoji, title, subtitle) = emptyStateFor(selectedType, hechas)
            EmptyState(emoji, title, subtitle, modifier = Modifier.fillMaxWidth())
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
                            is ListaRow.SectionHeader -> "header-$index"
                            is ListaRow.EntryRow -> row.item.id
                        }
                    }
                ) { _, row ->
                    when (row) {
                        is ListaRow.SectionHeader -> SectionLabel(row.label, color = row.color)
                        is ListaRow.EntryRow -> ItemCard(
                            item = row.item,
                            onClick = { onItemClick(row.item) },
                            onToggleDone = { onToggleDone(row.item) },
                            modifier = Modifier.padding(bottom = 9.dp),
                            showCheckbox = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeTabs(
    selected: ItemType,
    alertedTypes: Set<ItemType>,
    onSelect: (ItemType) -> Unit,
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
        typeTabs.forEach { (type, label) ->
            val isSel = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSel) Accent else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = if (isSel) Color.White else Muted
                )
                // Punto de aviso: hay un recordatorio de este tipo que ya sonó
                // y todavía no se revisó.
                if (type in alertedTypes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 4.dp)
                            .size(8.dp)
                            .background(PrioridadAlta, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneFilterControl(
    selected: DoneFilter,
    onSelect: (DoneFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DoneFilter.entries.forEach { f ->
            val isSel = f == selected
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSel) Accent.copy(alpha = 0.12f) else Color.Transparent)
                    .border(1.dp, if (isSel) Accent else Hairline, shape)
                    .clickable { onSelect(f) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = f.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSel) Accent else Muted
                )
            }
        }
    }
}
