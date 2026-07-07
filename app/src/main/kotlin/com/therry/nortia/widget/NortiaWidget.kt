package com.therry.nortia.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.therry.nortia.MainActivity
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.EventType
import com.therry.nortia.ui.theme.InkLight
import com.therry.nortia.ui.theme.MutedLight
import com.therry.nortia.ui.theme.SurfaceLight
import com.therry.nortia.util.occursOn
import com.therry.nortia.util.to12h
import com.therry.nortia.util.todayString
import kotlinx.coroutines.flow.first

class NortiaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = todayString()
        val events = AppDatabase.getInstance(context).eventDao().getAll().first()
        val items = events
            .filter { occursOn(it, today) }
            .filterNot { it.type == EventType.TAREA && today in it.completedDates }
            .sortedBy { it.time ?: "23:59" }
            .take(6)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(SurfaceLight)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(
                    text = "Hoy",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorProvider(InkLight))
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                if (items.isEmpty()) {
                    Text(
                        text = "Sin eventos hoy",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(MutedLight))
                    )
                } else {
                    items.forEach { event ->
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                text = event.time?.let { to12h(it).let { (t, ap) -> "$t$ap" } } ?: "•",
                                modifier = GlanceModifier.width(46.dp),
                                style = TextStyle(fontSize = 12.sp, color = ColorProvider(MutedLight))
                            )
                            Text(
                                text = event.title,
                                style = TextStyle(fontSize = 13.sp, color = ColorProvider(InkLight))
                            )
                        }
                    }
                }
            }
        }
    }
}
