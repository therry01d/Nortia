package com.therry.nortia.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.therry.nortia.ui.theme.Accent
import com.therry.nortia.ui.theme.Muted

enum class AppTab(val label: String, val emoji: String) {
    HOY("Hoy", "☀️"),
    SEMANA("Semana", "📆"),
    CALENDARIO("Calendario", "🗓️"),
    TAREAS("Tareas", "✓")
}

@Composable
fun AgendaBottomNav(current: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { onSelect(tab) },
                icon = { Text(tab.emoji) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
