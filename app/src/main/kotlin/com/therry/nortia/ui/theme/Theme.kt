package com.therry.nortia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NortiaColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Card,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    secondary = Personal,
    onSecondary = Card,
    background = Background,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Background,
    onSurfaceVariant = Muted,
    outline = Hairline,
    error = PrioridadAlta,
    onError = Card,
    errorContainer = PrioridadAltaSoft,
    onErrorContainer = PrioridadAlta
)

@Composable
fun NortiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NortiaColorScheme,
        typography = Typography,
        content = content
    )
}
