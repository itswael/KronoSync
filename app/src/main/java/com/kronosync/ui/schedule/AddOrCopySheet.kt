package com.kronosync.ui.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kronosync.util.DayEpoch
import com.kronosync.util.WeekStart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class SheetMode(val label: String) { New("New"), CopyDay("Copy day"), CopyWeek("Copy week") }

private val DurationOptionsMinutes = listOf(15, 30, 45, 60, 90, 120)

/** millis returned by a Compose DatePickerState are always UTC-midnight of the picked date — extract the LocalDate this way, never re-derive a dayEpoch from it directly. */
private fun pickedLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrCopySheet(
    onDismiss: () -> Unit,
    onAddTask: (dayEpoch: Long, startMinute: Int, durationMinutes: Int, title: String) -> Unit,
    onCopyDay: (List<Long>) -> Unit,
    onCopyWeek: (targetWeekStartEpoch: Long) -> Unit,
    defaultDayEpoch: Long,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var mode by remember { mutableStateOf(SheetMode.New) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SheetMode.values().forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = SheetMode.values().size)
                    ) { Text(m.label) }
                }
            }
            Spacer(Modifier.height(16.dp))

            when (mode) {
                SheetMode.New -> NewTaskForm(defaultDayEpoch = defaultDayEpoch, onAdd = onAddTask)
                SheetMode.CopyDay -> CopyDayForm(onCopy = onCopyDay)
                SheetMode.CopyWeek -> CopyWeekForm(onCopy = onCopyWeek)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTaskForm(
    defaultDayEpoch: Long,
    onAdd: (dayEpoch: Long, startMinute: Int, durationMinutes: Int, title: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(java.time.LocalTime.now().let { it.hour * 60 + (it.minute / 15) * 15 }) }
    var durationMinutes by remember { mutableStateOf(60) }
    var selectedDayEpoch by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    Column {
        Text("Add to your day", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            SuggestionChip(onClick = { showDatePicker = true }, label = { Text(if (selectedDayEpoch != null) "Change date" else "Today") })
            SuggestionChip(onClick = { showTimePicker = true }, label = { Text(String.format("%02d:%02d", minutes / 60, minutes % 60)) })
        }
        Spacer(Modifier.height(16.dp))
        Text("Duration", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(DurationOptionsMinutes) { option ->
                FilterChip(
                    selected = durationMinutes == option,
                    onClick = { durationMinutes = option },
                    label = { Text(if (option < 60) "${option}m" else if (option % 60 == 0) "${option / 60}h" else "${option / 60}h ${option % 60}m") }
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (title.isNotBlank()) onAdd(selectedDayEpoch ?: defaultDayEpoch, minutes, durationMinutes, title) },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank()
        ) { Text("Add") }
        Spacer(Modifier.height(16.dp))
    }

    if (showTimePicker) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val dialog = TimePickerDialog(context, { _, h, m ->
                minutes = h * 60 + m
            }, minutes / 60, minutes % 60, true)
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    dateState.selectedDateMillis?.let { selectedDayEpoch = DayEpoch.of(pickedLocalDate(it)) }
                    showDatePicker = false
                }) { Text("Use date") }
            },
            dismissButton = { Button(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyDayForm(onCopy: (List<Long>) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateOf(listOf<Long>()) }
    val dateState = rememberDatePickerState()
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }

    Column {
        Text("Copy today's schedule to", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Each day opens for you to adjust afterward — the original stays untouched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (selectedDays.value.isNotEmpty()) {
            LazyRow {
                items(selectedDays.value) { epoch ->
                    val label = DayEpoch.toLocalDate(epoch).format(formatter)
                    AssistChip(
                        onClick = { selectedDays.value = selectedDays.value - epoch },
                        label = { Text(label) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $label", modifier = Modifier.wrapContentWidth()) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(onClick = { showDatePicker = true }) { Text("Add a date") }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onCopy(selectedDays.value) },
            enabled = selectedDays.value.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Copy to ${selectedDays.value.size} day${if (selectedDays.value.size == 1) "" else "s"}") }
        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val dayEpoch = DayEpoch.of(pickedLocalDate(millis))
                        if (dayEpoch !in selectedDays.value) selectedDays.value = selectedDays.value + dayEpoch
                    }
                    showDatePicker = false
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyWeekForm(onCopy: (Long) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var targetWeekStart by remember { mutableStateOf<LocalDate?>(null) }
    val dateState = rememberDatePickerState()
    val formatter = remember { DateTimeFormatter.ofPattern("MMM d") }

    Column {
        Text("Copy this week to another week", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pick any day in the target week — the whole Mon–Sun schedule copies over, then you can adjust it freely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        targetWeekStart?.let { weekStart ->
            AssistChip(
                onClick = { showDatePicker = true },
                label = { Text("Week of ${weekStart.format(formatter)}") }
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = { showDatePicker = true }) {
            Text(if (targetWeekStart == null) "Pick a target week" else "Change target week")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { targetWeekStart?.let { onCopy(DayEpoch.of(it)) } },
            enabled = targetWeekStart != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Copy week") }
        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        targetWeekStart = WeekStart.of(pickedLocalDate(millis))
                    }
                    showDatePicker = false
                }) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}
