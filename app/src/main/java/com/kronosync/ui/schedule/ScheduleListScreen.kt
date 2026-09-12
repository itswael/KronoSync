package com.kronosync.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.kronosync.domain.CopyDayUseCase
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.ui.schedule.parseTargetDaysCsv
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import android.app.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui = vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var showCopy by remember { mutableStateOf(false) }
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(ui.value.blocks) { block ->
                ListItem(
                    headlineContent = { Text(block.title) },
                    supportingContent = { Text("Starts at "+ formatMinutes(block.startMinute)) }
                )
            }
        }
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { showSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Quick add")
            }
            IconButton(onClick = { showCopy = true }) {
                Icon(Icons.Default.Add, contentDescription = "Copy day")
            }
        }
    }

    if (showSheet) {
        QuickAddSheet(
            onDismiss = { showSheet = false },
            onAdd = { dayEpoch, minutes, title ->
                vm.addBlockFor(dayEpoch ?: ui.value.dayEpoch, minutes, title)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
            },
            sheetState = sheetState,
            defaultDayEpoch = ui.value.dayEpoch
        )
    }

    if (showCopy) {
        CopyDaySheet(
            onDismiss = { showCopy = false },
            onCopy = { csv ->
                val targets = parseTargetDaysCsv(csv)
                scope.launch { vm.copyTo(targets) }
                showCopy = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onAdd: (Long?, Int, String) -> Unit,
    sheetState: SheetState,
    defaultDayEpoch: Long
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(480) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDayEpoch by remember { mutableStateOf<Long?>(null) }
    val dateState = rememberDatePickerState()
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showDatePicker = true }) { Text(if (selectedDayEpoch != null) "Change date" else "Pick date") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { showTimePicker = true }) { Text("Pick time") }
            Spacer(Modifier.height(8.dp))
            val selectedTime = String.format("%02d:%02d", minutes/60, minutes%60)
            Row { Text("Selected time: $selectedTime") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { if (title.isNotBlank()) onAdd(selectedDayEpoch ?: defaultDayEpoch, minutes, title) }) { Text("Add") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showTimePicker) {
        LaunchedEffect(Unit) {
            val dialog = TimePickerDialog(context, { _, h, m ->
                minutes = h * 60 + m
            }, minutes/60, minutes%60, true)
            dialog.setOnDismissListener { showTimePicker = false }
            dialog.show()
        }
    }

    if (showDatePicker) {
            DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        // Normalize to UTC midnight for dayEpoch
                        selectedDayEpoch = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                            .atStartOfDay()
                            .toInstant(java.time.ZoneOffset.UTC)
                            .toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("Use date") }
            },
            dismissButton = { Button(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}
private fun formatMinutes(m: Int): String {
    val h = m / 60
    val min = m % 60
    return String.format("%02d:%02d", h, min)
}
