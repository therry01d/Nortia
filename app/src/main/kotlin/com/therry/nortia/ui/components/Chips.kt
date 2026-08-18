package com.therry.nortia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Category
import com.therry.nortia.data.Priority
import com.therry.nortia.ui.theme.AccentSoft
import com.therry.nortia.ui.theme.Background
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.ui.theme.Personal
import com.therry.nortia.ui.theme.PersonalSoft
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.ui.theme.PrioridadAltaSoft
import com.therry.nortia.ui.theme.PrioridadBaja
import com.therry.nortia.ui.theme.PrioridadBajaSoft
import com.therry.nortia.ui.theme.PrioridadMedia
import com.therry.nortia.ui.theme.PrioridadMediaSoft
import com.therry.nortia.ui.theme.Trabajo

@Composable
fun AgendaChip(
    text: String,
    containerColor: Color = Background,
    contentColor: Color = Muted,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = modifier
            .background(containerColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

fun categoryColor(category: Category): Color = if (category == Category.PERSONAL) Personal else Trabajo

@Composable
fun CategoryChip(category: Category, modifier: Modifier = Modifier) {
    val label = if (category == Category.PERSONAL) "Personal" else "Trabajo"
    val (container, content) = if (category == Category.PERSONAL) {
        PersonalSoft to Personal
    } else {
        AccentSoft to Trabajo
    }
    AgendaChip(label, container, content, modifier)
}

@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val (label, container, content) = when (priority) {
        Priority.ALTA -> Triple("Alta", PrioridadAltaSoft, PrioridadAlta)
        Priority.MEDIA -> Triple("Media", PrioridadMediaSoft, PrioridadMedia)
        Priority.BAJA -> Triple("Baja", PrioridadBajaSoft, PrioridadBaja)
    }
    AgendaChip(label, container, content, modifier)
}

@Composable
fun SimpleChip(text: String, modifier: Modifier = Modifier) {
    AgendaChip(text, Background, Muted, modifier)
}
