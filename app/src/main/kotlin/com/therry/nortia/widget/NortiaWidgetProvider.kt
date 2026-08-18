package com.therry.nortia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

        // Toque directo (encabezado / estado vacío): puede ser inmutable.
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            mainActivityIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openApp)
        views.setOnClickPendingIntent(R.id.widget_empty, openApp)

        // La plantilla de la lista DEBE ser mutable: el sistema le fusiona el
        // fill-in intent de cada fila. Con FLAG_IMMUTABLE el toque en la fila no
        // hace nada. Va con requestCode propio para no colisionar con el de arriba
        // (mismo Intent + mismo requestCode = el mismo PendingIntent).
        val rowTemplate = PendingIntent.getActivity(
            context,
            REQUEST_ROW_TEMPLATE,
            mainActivityIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        views.setPendingIntentTemplate(R.id.widget_list, rowTemplate)

        return views
    }

    private fun mainActivityIntent(context: Context) =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    /**
     * FLAG_MUTABLE existe desde API 31; antes de esa versión los PendingIntent ya
     * eran mutables por defecto, así que no hace falta ninguna bandera.
     */
    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    companion object {

        private const val REQUEST_OPEN_APP = 0
        private const val REQUEST_ROW_TEMPLATE = 1

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
