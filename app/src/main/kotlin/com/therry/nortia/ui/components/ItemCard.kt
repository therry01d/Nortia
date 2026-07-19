package com.therry.nortia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.ui.theme.Hairline
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.ui.theme.PrioridadBaja
import com.therry.nortia.util.DateTimeUtils

private fun typeIcon(type: ItemType): String = when (type) {
    ItemType.EVENTO -> "📅"
    ItemType.TAREA -> "✓"
    ItemType.RECORDATORIO -> "⏰"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = categoryColor(item.category)
    val done = item.type == ItemType.TAREA && item.done

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(barColor)
            )
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (item.type == ItemType.TAREA) {
                    TaskCheckbox(done = done, onToggle = onToggleDone)
                } else {
                    TimeColumn(time = item.time)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${typeIcon(item.type)} ${item.title}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (done) Muted else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (done) TextDecoration.LineThrough else null
                    )
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.type == ItemType.TAREA && !item.time.isNullOrBlank()) {
                            val (hour, ampm) = DateTimeUtils.to12Hour(item.time)
                            SimpleChip("$hour $ampm")
                        }
                        CategoryChip(item.category)
                        item.priority?.let { PriorityChip(it) }
                        if (item.remind) SimpleChip("🔔")
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            fontSize = 13.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeColumn(time: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(52.dp), contentAlignment = Alignment.TopCenter) {
        if (time != null) {
            val (hour, ampm) = DateTimeUtils.to12Hour(time)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = hour, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = ampm, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Muted)
            }
        } else {
            Text(
                text = "Todo el día",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun TaskCheckbox(done: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(24.dp)
            .background(color = if (done) PrioridadBaja else Color.Transparent, shape = shape)
            .border(width = 2.dp, color = if (done) PrioridadBaja else Hairline, shape = shape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
