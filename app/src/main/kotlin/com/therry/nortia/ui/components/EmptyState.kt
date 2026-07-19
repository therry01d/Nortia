package com.therry.nortia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.ui.theme.Ink
import com.therry.nortia.ui.theme.Muted

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionLabel(text: String, color: androidx.compose.ui.graphics.Color = Muted, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}
