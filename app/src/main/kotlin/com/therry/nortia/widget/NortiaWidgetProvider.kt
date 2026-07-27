package com.therry.nortia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.therry.nortia.MainActivity
import com.therry.nortia.R
import com.therry.nortia.util.DateTimeUtils

/**
 * Widget de pantalla de inicio: muestra lo pendiente de hoy (eventos, tareas y
 * recordatorios) y abre la app al tocarlo.
 */
class NortiaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId))
        }
    }

    private fun buildRemoteViews(context: Context, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_nortia)

        views.setTextViewText(R.id.widget_date, DateTimeUtils.formatShortDate(DateTimeUtils.today()))

        // El Intent del adapter necesita un data URI único por widget; si no,
        // Android reutiliza la misma factory para todas las instancias.
        val serviceIntent = Intent(context, NortiaWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // Abrir la app: tanto desde el encabezado como desde cualquier fila.
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openApp)
        views.setOnClickPendingIntent(R.id.widget_empty, openApp)
        views.setPendingIntentTemplate(R.id.widget_list, openApp)

        return views
    }

    companion object {

        /**
         * Refresca todos los widgets colocados. Se llama al crear/editar/borrar un
         * item y al disparar un recordatorio, para que la lista no quede vieja
         * hasta el próximo refresco periódico.
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context.applicationContext, NortiaWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            // Repinta el encabezado (la fecha cambia al pasar el día)...
            context.sendBroadcast(
                Intent(context, NortiaWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
            // ...y fuerza a la factory a releer la base.
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }
}
