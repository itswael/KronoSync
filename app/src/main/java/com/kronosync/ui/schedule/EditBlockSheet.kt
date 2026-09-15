package com.kronosync.ui.schedule

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.util.TimeFormat

private val DurationOptionsMinutes = listOf(15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBlockSheet(
    block: ScheduleBlock,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (startMinute: Int, durationMinutes: Int, title: String, lockIn: Boolean) -> Unit,
    onDelete: () -> Unit,
    onCopyToNow: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var title by remember { mutableStateOf(block.title) }
    var minutes by remember { mutableStateOf(block.startMinute) }
    var durationMinutes by remember { mutableStateOf(block.durationMinutes) }
    var lockIn by remember { mutableStateOf(block.lockIn) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Edit task", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            SuggestionChip(onClick = { showTimePicker = true }, label = { Text("Starts at " + TimeFormat.minutesLabel(minutes, use24Hour)) })
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lock-in", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Locks your phone to just this app, camera, and calls for the task's duration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = lockIn, onCheckedChange = { lockIn = it })
            }
            Spacer(Modifier.height(16.dp))
            Text("Duration", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onClick = { if (title.isNotBlank()) onSave(minutes, durationMinutes, title, lockIn) },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) { Text("Save changes") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCopyToNow, modifier = Modifier.fillMaxWidth()) {
                Text("Copy to current hour, today")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Delete task")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        LaunchedEffect(Unit) {
            val dialog = TimePickerDialog(context, { _, h, m ->
                minutes = h * 60 + m
            }, minutes / 60, minutes % 60, use24Hour)
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
        }
    }
}
