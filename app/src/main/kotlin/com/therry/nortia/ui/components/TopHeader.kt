package com.therry.nortia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.ui.theme.Accent
import com.therry.nortia.ui.theme.Card
import com.therry.nortia.ui.theme.Hairline
import com.therry.nortia.ui.theme.Muted

@Composable
fun TopHeader(
    greet: String,
    title: String,
    notificationsEnabled: Boolean,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = greet, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Muted)
            Text(text = title, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        val shape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(if (notificationsEnabled) Accent else Card, shape)
                .border(1.dp, if (notificationsEnabled) Accent else Hairline, shape)
                .clickable { onBellClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔔", fontSize = 18.sp)
        }
    }
}
