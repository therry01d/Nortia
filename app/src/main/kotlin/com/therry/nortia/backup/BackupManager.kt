package com.therry.nortia.backup

import com.google.gson.Gson
import com.therry.nortia.data.Event

private data class BackupPayload(val version: Int = 1, val events: List<Event> = emptyList())

object BackupManager {
    private val gson = Gson()

    fun exportJson(events: List<Event>): String = gson.toJson(BackupPayload(events = events))

    fun importJson(json: String): List<Event> =
        gson.fromJson(json, BackupPayload::class.java)?.events ?: emptyList()
}
