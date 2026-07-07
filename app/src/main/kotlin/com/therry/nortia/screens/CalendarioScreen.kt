package com.therry.nortia.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therry.nortia.R

@Composable
fun CalendarioScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗓️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.calendario_placeholder_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.calendario_placeholder_subtitle), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
