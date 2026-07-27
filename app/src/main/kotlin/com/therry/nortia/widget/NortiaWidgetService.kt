package com.therry.nortia.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.therry.nortia.R
import com.therry.nortia.data.AppDatabase
import com.therry.nortia.data.Category
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.util.DateTimeUtils
import com.therry.nortia.util.RecurrenceUtils

class NortiaWidgetService : RemoteViewsService() {
    // El tipo va calificado: Kotlin no trae al scope los tipos anidados de la
    // superclase como sí hace Java, así que "RemoteViewsFactory" a secas no resuelve.
    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory =
        NortiaWidgetFactory(applicationContext)
}

private const val MAX_ITEMS = 25

private class NortiaWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    // Escrito en onDataSetChanged y leído en getCount/getViewAt, que pueden ser
    // hilos de binder distintos.
    @Volatile
    private var items: List<Item> = emptyList()

    override fun onCreate() = Unit

    /**
     * Corre en un hilo de binder (nunca en el principal), así que se puede
     * consultar Room de forma bloqueante.
     */
    override fun onDataSetChanged() {
        items = try {
            val today = DateTimeUtils.today()
            AppDatabase.getInstance(context).itemDao()
                .getPendingBlocking()
                .filter { RecurrenceUtils.occursOn(it, today) }
                .sortedWith(compareBy({ it.time == null }, { it.time }))
                .take(MAX_ITEMS)
        } catch (e: Throwable) {
            // Si la base falla, el widget muestra la lista vacía en vez de romperse.
            Log.e("Nortia", "Error cargando items del widget", e)
            emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        val item = items.getOrNull(position) ?: return views

        views.setTextViewText(R.id.widget_item_title, "${typeIcon(item.type)} ${item.title}")
        views.setTextViewText(R.id.widget_item_meta, meta(item))
        views.setInt(R.id.widget_item_bar, "setBackgroundColor", categoryColor(item.category))

        // Completa la plantilla de PendingIntent del ListView: toda la fila abre la app.
        views.setOnClickFillInIntent(R.id.widget_item_root, Intent())

        return views
    }

    private fun meta(item: Item): String {
        val time = item.time
        val hora = if (time.isNullOrBlank()) {
            context.getString(R.string.widget_all_day)
        } else {
            val (hour, ampm) = DateTimeUtils.to12Hour(time)
            "$hour $ampm"
        }
        val categoria = if (item.category == Category.PERSONAL) "Personal" else "Trabajo"
        return "$hora · $categoria"
    }

    private fun typeIcon(type: ItemType): String = when (type) {
        ItemType.EVENTO -> "📅"
        ItemType.TAREA -> "✓"
        ItemType.RECORDATORIO -> "⏰"
    }

    private fun categoryColor(category: Category): Int =
        if (category == Category.PERSONAL) 0xFFE8663D.toInt() else 0xFF3B5BDB.toInt()

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
