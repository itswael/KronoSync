package com.kronosync.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.Schedule
import com.kronosync.ui.schedule.grid.ScheduleGridScreen
import com.kronosync.ui.schedule.dial.ScheduleDialScreen
import com.kronosync.ui.schedule.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
    var isGrid by remember { mutableStateOf(false) }
    var isDial by remember { mutableStateOf(true) }
    var showQuickAdd by remember { mutableStateOf(false) }
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val vm: ScheduleViewModel = hiltViewModel()
    TopAppBar(
        title = { Text("Schedule") },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = { if (isDial) { isDial = false; isGrid = true } else { isGrid = !isGrid } }) {
                Icon(
                    if (isGrid) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = if (isGrid) "Switch to list" else "Switch to grid"
                )
            }
            IconButton(onClick = { isDial = !isDial }) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = if (isDial) "Switch to list" else "Switch to dial"
                )
            }
            IconButton(onClick = { /* TODO copy-day action */ }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy this day")
            }
        }
    )

    Box(Modifier.fillMaxSize()) {
        when {
            isDial -> ScheduleDialScreen()
            isGrid -> ScheduleGridScreen()
            else -> ScheduleListScreen()
        }
        FloatingActionButton(
            onClick = { showQuickAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Quick add")
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(
            onDismiss = { showQuickAdd = false },
            onAdd = { dayEpoch, minutes, title ->
                vm.addBlockFor(dayEpoch ?: vm.state.value.dayEpoch, minutes, title)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showQuickAdd = false }
            },
            sheetState = sheetState,
            defaultDayEpoch = vm.state.value.dayEpoch
        )
    }
}
