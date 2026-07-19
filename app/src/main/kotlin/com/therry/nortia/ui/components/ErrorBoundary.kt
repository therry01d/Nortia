package com.therry.nortia.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Atrapa excepciones lanzadas durante la composición de [content] y muestra el
 * error en pantalla en vez de tirar abajo toda la app. Es temporal, para poder
 * diagnosticar un crash sin acceso a logcat.
 */
@Composable
fun ScreenErrorBoundary(key: Any?, content: @Composable () -> Unit) {
    var error by remember(key) { mutableStateOf<Throwable?>(null) }
    val current = error
    if (current != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Error al mostrar esta pantalla",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = current.toString(), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            current.stackTrace.take(12).forEach {
                Text(text = it.toString(), fontSize = 10.sp)
            }
        }
    } else {
        try {
            content()
        } catch (t: Throwable) {
            error = t
        }
    }
}
