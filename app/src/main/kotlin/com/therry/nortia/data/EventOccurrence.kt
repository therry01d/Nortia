package com.therry.nortia.data

data class EventOccurrence(val event: Event, val occurrenceDate: String) {
    val isDone: Boolean get() = event.type == EventType.TAREA && occurrenceDate in event.completedDates
}
