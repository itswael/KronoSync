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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui = vm.state
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
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
            // Placeholder for copy-day UI trigger (Step 7), wires later
        }
    }

    if (showSheet) {
        QuickAddSheet(
            onDismiss = { showSheet = false },
            onAdd = { minutes, title ->
                vm.addBlock(minutes, title)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false }
            },
            sheetState = sheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    onDismiss: () -> Unit,
    onAdd: (Int, String) -> Unit,
    sheetState: SheetState
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf(480) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task name") })
            Spacer(Modifier.height(8.dp))
            Row { Text("Time (minutes from midnight): ") }
            OutlinedTextField(value = minutes.toString(), onValueChange = { v ->
                minutes = v.toIntOrNull() ?: minutes
            }, label = { Text("Start minute") })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { if (title.isNotBlank()) onAdd(minutes, title) }) { Text("Add") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatMinutes(m: Int): String {
    val h = m / 60
    val min = m % 60
    return String.format("%02d:%02d", h, min)
}
