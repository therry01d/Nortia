package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.therry.nortia.R
import com.therry.nortia.data.Category
import com.therry.nortia.data.EventOccurrence
import com.therry.nortia.data.EventType
import com.therry.nortia.data.Priority
import com.therry.nortia.data.RepeatRule
import com.therry.nortia.ui.theme.CategoriaPersonal
import com.therry.nortia.ui.theme.CategoriaTrabajo
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.ui.theme.PrioridadBaja
import com.therry.nortia.ui.theme.PrioridadMedia
import com.therry.nortia.util.to12h

fun categoryColor(category: Category): Color = when (category) {
    Category.TRABAJO -> CategoriaTrabajo
    Category.PERSONAL -> CategoriaPersonal
}

fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.ALTA -> PrioridadAlta
    Priority.MEDIA -> PrioridadMedia
    Priority.BAJA -> PrioridadBaja
}

fun typeIcon(type: EventType): String = when (type) {
    EventType.EVENTO -> "📅"
    EventType.TAREA -> "✓"
    EventType.RECORDATORIO -> "⏰"
}

@Composable
private fun LabelChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(
    occurrence: EventOccurrence,
    onOpen: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = occurrence.event
    val done = occurrence.isDone
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(categoryColor(event.category), RoundedCornerShape(2.dp))
            )

            if (event.type == EventType.TAREA) {
                Checkbox(checked = done, onCheckedChange = { onToggleDone() })
            } else {
                val time = event.time
                Column(
                    modifier = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (time != null) {
                        val (t, ampm) = to12h(time)
                        Text(t, style = MaterialTheme.typography.titleSmall)
                        Text(ampm, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text(
                            stringResource(R.string.allday_generic),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${typeIcon(event.type)} ${event.title}",
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (event.type == EventType.TAREA && event.time != null) {
                        val (t, ampm) = to12h(event.time)
                        LabelChip("$t $ampm", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LabelChip(
                        text = if (event.category == Category.TRABAJO) stringResource(R.string.category_trabajo) else stringResource(R.string.category_personal),
                        color = categoryColor(event.category)
                    )
                    event.priority?.let {
                        val label = when (it) {
                            Priority.ALTA -> stringResource(R.string.priority_alta)
                            Priority.MEDIA -> stringResource(R.string.priority_media)
                            Priority.BAJA -> stringResource(R.string.priority_baja)
                        }
                        LabelChip(label, priorityColor(it))
                    }
                    if (event.remind) {
                        Text("🔔", style = MaterialTheme.typography.labelSmall)
                    }
                    if (event.repeat != RepeatRule.NINGUNO) {
                        Text("🔁", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (event.note.isNotBlank()) {
                    Text(
                        event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
