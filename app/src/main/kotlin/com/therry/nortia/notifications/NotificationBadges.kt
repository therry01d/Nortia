package com.therry.nortia.notifications

import android.content.Context
import android.content.SharedPreferences
import com.therry.nortia.data.ItemType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Registra qué tipos de item tienen un recordatorio que sonó y el usuario
 * todavía no revisó, para marcar con un punto la pestaña correspondiente.
 *
 * Se persiste en SharedPreferences porque la notificación puede dispararse con
 * la app cerrada (el proceso lo levanta el BroadcastReceiver): guardarlo solo en
 * memoria perdería la marca justo en el caso más común.
 */
object NotificationBadges {

    private const val PREFS_NAME = "nortia_badges"
    private const val KEY_TYPES = "types_with_alerts"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Marca el tipo como "tiene un aviso sin revisar". Se llama al postear la notificación. */
    fun mark(context: Context, type: ItemType) {
        val prefs = prefs(context)
        val current = prefs.getStringSet(KEY_TYPES, emptySet()).orEmpty()
        if (type.name in current) return
        // Se escribe un Set nuevo: mutar el que devuelve getStringSet no es seguro.
        prefs.edit().putStringSet(KEY_TYPES, current + type.name).apply()
    }

    /** Limpia la marca de un tipo, cuando el usuario abre esa pestaña. */
    fun clear(context: Context, type: ItemType) {
        val prefs = prefs(context)
        val current = prefs.getStringSet(KEY_TYPES, emptySet()).orEmpty()
        if (type.name !in current) return
        prefs.edit().putStringSet(KEY_TYPES, current - type.name).apply()
    }

    /** Emite el conjunto actual y cada cambio posterior, para que la UI reaccione en vivo. */
    fun observe(context: Context): Flow<Set<ItemType>> = callbackFlow {
        val prefs = prefs(context)
        trySend(read(prefs))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == null || key == KEY_TYPES) trySend(read(changed))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun read(prefs: SharedPreferences): Set<ItemType> =
        prefs.getStringSet(KEY_TYPES, emptySet()).orEmpty()
            .mapNotNull { name -> runCatching { ItemType.valueOf(name) }.getOrNull() }
            .toSet()
}
