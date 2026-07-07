package com.therry.nortia.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.therry.nortia.R
import com.therry.nortia.data.Category
import com.therry.nortia.data.Event
import com.therry.nortia.data.EventType
import com.therry.nortia.data.Priority
import com.therry.nortia.data.RepeatRule
import com.therry.nortia.util.dateToCalendar
import com.therry.nortia.util.formatDate
import com.therry.nortia.util.formatTime
import java.util.Calendar

@Composable
fun EventEditDialog(
    initial: Event?,
    defaultDate: String,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (() -> Unit)?,
    onValidationError: (String) -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: EventType.EVENTO) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var time by remember { mutableStateOf(initial?.time ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Category.TRABAJO) }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIA) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: RepeatRule.NINGUNO) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var remind by remember { mutableStateOf(initial?.remind ?: true) }
    var remindBefore by remember { mutableStateOf(initial?.remindBeforeMinutes ?: 10) }

    val context = LocalContext.current

    fun pickDate() {
        val cal = if (date.isNotBlank()) dateToCalendar(date) else Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d -> date = formatDate(y, m, d) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickTime() {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR_OF_DAY)
        var minute = cal.get(Calendar.MINUTE)
        if (time.isNotBlank()) {
            val parts = time.split(":").map { it.toInt() }
            hour = parts[0]; minute = parts[1]
        }
        TimePickerDialog(
            context,
            { _, h, m -> time = formatTime(h, m) },
            hour, minute, true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) stringResource(R.string.dialog_new_title) else stringResource(R.string.dialog_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FieldLabel(stringResource(R.string.field_type))
                OptionPicker(
                    options = listOf(
                        EventType.EVENTO to stringResource(R.string.type_evento),
                        EventType.TAREA to stringResource(R.string.type_tarea),
                        EventType.RECORDATORIO to stringResource(R.string.type_recordatorio)
                    ),
                    selected = type,
                    onSelect = { type = it }
                )

                FieldLabel(stringResource(R.string.field_title))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.field_title_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.field_date))
                        PickerField(value = date, placeholder = "AAAA-MM-DD", onClick = ::pickDate)
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.field_time))
                        PickerField(value = time, placeholder = "HH:mm", onClick = ::pickTime)
                    }
                }

                if (date.isNotBlank()) {
                    FieldLabel(stringResource(R.string.field_repeat))
                    OptionPicker(
                        options = listOf(
                            RepeatRule.NINGUNO to stringResource(R.string.repeat_none),
                            RepeatRule.DIARIO to stringResource(R.string.repeat_daily),
                            RepeatRule.SEMANAL to stringResource(R.string.repeat_weekly),
                            RepeatRule.MENSUAL to stringResource(R.string.repeat_monthly),
                            RepeatRule.ANUAL to stringResource(R.string.repeat_yearly)
                        ),
                        selected = repeat,
                        onSelect = { repeat = it }
                    )
                }

                FieldLabel(stringResource(R.string.field_category))
                OptionPicker(
                    options = listOf(
                        Category.TRABAJO to stringResource(R.string.category_trabajo),
                        Category.PERSONAL to stringResource(R.string.category_personal)
                    ),
                    selected = category,
                    onSelect = { category = it }
                )

                if (type == EventType.TAREA) {
                    FieldLabel(stringResource(R.string.field_priority))
                    OptionPicker(
                        options = listOf(
                            Priority.ALTA to stringResource(R.string.priority_alta),
                            Priority.MEDIA to stringResource(R.string.priority_media),
                            Priority.BAJA to stringResource(R.string.priority_baja)
                        ),
                        selected = priority,
                        onSelect = { priority = it }
                    )
                }

                FieldLabel(stringResource(R.string.field_note))
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.field_note_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.field_remind), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = remind, onCheckedChange = { remind = it })
                }

                if (remind) {
                    FieldLabel(stringResource(R.string.field_remind_before))
                    OptionPicker(
                        options = listOf(
                            0 to stringResource(R.string.remind_at_time),
                            5 to stringResource(R.string.remind_5),
                            10 to stringResource(R.string.remind_10),
                            30 to stringResource(R.string.remind_30),
                            60 to stringResource(R.string.remind_60),
                            1440 to stringResource(R.string.remind_1440)
                        ),
                        selected = remindBefore,
                        onSelect = { remindBefore = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = save@{
                val trimmedTitle = title.trim()
                if (trimmedTitle.isBlank()) {
                    onValidationError(context.getString(R.string.error_missing_title))
                    return@save
                }
                if (type == EventType.RECORDATORIO && (date.isBlank() || time.isBlank())) {
                    onValidationError(context.getString(R.string.error_reminder_needs_datetime))
                    return@save
                }
                if (repeat != RepeatRule.NINGUNO && date.isBlank()) {
                    onValidationError(context.getString(R.string.error_repeat_needs_date))
                    return@save
                }
                onSave(
                    (initial ?: Event(type = EventType.EVENTO, title = "", category = Category.TRABAJO)).copy(
                        type = type,
                        title = trimmedTitle,
                        date = date.ifBlank { null },
                        time = time.ifBlank { null },
                        category = category,
                        priority = if (type == EventType.TAREA) priority else null,
                        note = note.trim(),
                        repeat = if (date.isNotBlank()) repeat else RepeatRule.NINGUNO,
                        remind = remind && date.isNotBlank(),
                        remindBeforeMinutes = remindBefore
                    )
                )
            }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.dialog_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    )
}

@Composable
private fun PickerField(value: String, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = value.ifBlank { placeholder },
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 14.dp),
            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}
