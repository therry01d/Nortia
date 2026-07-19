package com.therry.nortia.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.therry.nortia.data.Category
import com.therry.nortia.data.Item
import com.therry.nortia.data.ItemType
import com.therry.nortia.data.Priority
import com.therry.nortia.ui.theme.Accent
import com.therry.nortia.ui.theme.AccentSoft
import com.therry.nortia.ui.theme.Hairline
import com.therry.nortia.ui.theme.Muted
import com.therry.nortia.ui.theme.Personal
import com.therry.nortia.ui.theme.PersonalSoft
import com.therry.nortia.ui.theme.PrioridadAlta
import com.therry.nortia.util.DateTimeUtils

private val remindOptions = listOf(
    0 to "A la hora exacta",
    5 to "5 min antes",
    10 to "10 min antes",
    30 to "30 min antes",
    60 to "1 hora antes",
    1440 to "1 día antes"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(
    editing: Item?,
    defaultDate: Long,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (() -> Unit)?
) {
    var type by remember { mutableStateOf(editing?.type ?: ItemType.EVENTO) }
    var title by remember { mutableStateOf(TextFieldValue(editing?.title ?: "")) }
    var noDate by remember { mutableStateOf(editing != null && editing.date == null) }
    var dateMillis by remember { mutableStateOf(editing?.date ?: defaultDate) }
    var hasTime by remember { mutableStateOf(editing?.time != null) }
    var timeStr by remember { mutableStateOf(editing?.time ?: "09:00") }
    var category by remember { mutableStateOf(editing?.category ?: Category.TRABAJO) }
    var priority by remember { mutableStateOf(editing?.priority ?: Priority.MEDIA) }
    var note by remember { mutableStateOf(TextFieldValue(editing?.note ?: "")) }
    var remind by remember { mutableStateOf(editing?.remind ?: true) }
    var remindBefore by remember { mutableStateOf(editing?.remindBeforeMinutes ?: 10) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (editing != null) "Editar" else "Nuevo",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            FieldLabel("Tipo")
            PickerRow(
                options = listOf(
                    ItemType.EVENTO to "📅 Evento",
                    ItemType.TAREA to "✓ Tarea",
                    ItemType.RECORDATORIO to "⏰ Recordatorio"
                ),
                selected = type,
                onSelect = { type = it }
            )

            FieldLabel("Título")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("¿Qué es?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("Fecha")
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        enabled = !noDate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (noDate) "Sin fecha" else DateTimeUtils.formatShortDate(dateMillis))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("Hora")
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = !noDate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasTime) timeStr else "Todo el día")
                    }
                }
            }

            if (type == ItemType.TAREA) {
                ToggleRow(
                    label = "Sin fecha asignada",
                    checked = noDate,
                    onToggle = { noDate = it }
                )
            }
            if (!noDate) {
                ToggleRow(
                    label = "Con hora",
                    checked = hasTime,
                    onToggle = { hasTime = it }
                )
            }

            FieldLabel("Categoría")
            PickerRow(
                options = listOf(Category.TRABAJO to "Trabajo", Category.PERSONAL to "Personal"),
                selected = category,
                onSelect = { category = it },
                selectedColor = if (category == Category.PERSONAL) Personal else Accent,
                selectedSoft = if (category == Category.PERSONAL) PersonalSoft else AccentSoft
            )

            if (type == ItemType.TAREA) {
                FieldLabel("Prioridad")
                PickerRow(
                    options = listOf(Priority.ALTA to "Alta", Priority.MEDIA to "Media", Priority.BAJA to "Baja"),
                    selected = priority,
                    onSelect = { priority = it }
                )
            }

            FieldLabel("Nota (opcional)")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Detalles...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            SwitchRow(label = "Recordarme", checked = remind, onToggle = { remind = it })

            if (remind) {
                FieldLabel("Avisarme")
                RemindBeforeDropdown(selected = remindBefore, onSelect = { remindBefore = it })
            }

            errorMessage?.let {
                Text(text = it, color = PrioridadAlta, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }

            Row(modifier = Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete) { Text("🗑") }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    onClick = {
                        val trimmedTitle = title.text.trim()
                        if (trimmedTitle.isEmpty()) {
                            errorMessage = "Ponle un título"
                            return@Button
                        }
                        if (type == ItemType.RECORDATORIO && (noDate || !hasTime)) {
                            errorMessage = "El recordatorio necesita fecha y hora"
                            return@Button
                        }
                        onSave(
                            Item(
                                id = editing?.id ?: 0,
                                type = type,
                                title = trimmedTitle,
                                date = if (noDate) null else dateMillis,
                                time = if (!noDate && hasTime) timeStr else null,
                                category = category,
                                priority = if (type == ItemType.TAREA) priority else null,
                                note = note.text.trim(),
                                done = editing?.done ?: false,
                                remind = remind && !noDate,
                                remindBeforeMinutes = remindBefore
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = DateTimeUtils.startOfDay(it) }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val parts = timeStr.split(":")
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    timeStr = "%02d:%02d".format(timeState.hour, timeState.minute)
                    hasTime = true
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Muted,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 2.dp)
    )
}

@Composable
private fun <T> PickerRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    selectedColor: Color = Accent,
    selectedSoft: Color = AccentSoft
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSel) selectedSoft else Color(0xFFFAFBFD), shape)
                    .border(1.dp, if (isSel) selectedColor else Hairline, shape)
                    .clickable { onSelect(value) }
                    .padding(vertical = 11.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSel) selectedColor else Muted
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Switch(checked = checked, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = Muted)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFBFD), RoundedCornerShape(12.dp))
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindBeforeDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = remindOptions.firstOrNull { it.first == selected }?.second ?: "10 min antes"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            remindOptions.forEach { (minutes, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}
