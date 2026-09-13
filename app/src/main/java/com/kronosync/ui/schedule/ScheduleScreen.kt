package com.kronosync.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.data.alarm.ExactAlarmPermission
import com.kronosync.ui.schedule.grid.ScheduleGridScreen
import com.kronosync.ui.schedule.dial.ScheduleDialScreen
import kotlinx.coroutines.launch

private enum class ScheduleView(val label: String) {
    Now("Now"),
    List("List"),
    Grid("Grid")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var view by remember { mutableStateOf(ScheduleView.Now) }
    var showAddSheet by remember { mutableStateOf(false) }
    val sheetState: SheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        // Handled manually via statusBarsPadding()/the outer bottom-nav padding below —
        // otherwise Scaffold's own default inset would double up with them.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add or copy")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().statusBarsPadding()) {
            Text(
                "Schedule",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                ScheduleView.values().forEachIndexed { index, v ->
                    SegmentedButton(
                        selected = view == v,
                        onClick = { view = v },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ScheduleView.values().size)
                    ) { Text(v.label) }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (view) {
                    ScheduleView.Now -> ScheduleDialScreen(vm = vm)
                    ScheduleView.List -> ScheduleListScreen(vm = vm)
                    ScheduleView.Grid -> ScheduleGridScreen()
                }
            }
        }
    }

    if (showAddSheet) {
        AddOrCopySheet(
            onDismiss = { showAddSheet = false },
            onAddTask = { dayEpoch, startMinute, durationMinutes, title ->
                vm.addBlockFor(dayEpoch, startMinute, durationMinutes, title)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
            },
            onCopyDay = { targetDayEpochs ->
                vm.copyDayTo(targetDayEpochs)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
            },
            onCopyWeek = { targetWeekStartEpoch ->
                vm.copyWeekTo(targetWeekStartEpoch)
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false }
            },
            defaultDayEpoch = ui.dayEpoch,
            sheetState = sheetState
        )
    }

    if (ui.needsExactAlarmPermission) {
        AlertDialog(
            onDismissRequest = { vm.exactAlarmPermissionHandled() },
            title = { Text("One more step for exact timing") },
            text = { Text("KronoSync needs permission to send notifications at the exact minute your blocks start. Without it, reminders may arrive late. You can turn this on in system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.exactAlarmPermissionHandled()
                    ExactAlarmPermission.openExactAlarmSettings(context)
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { vm.exactAlarmPermissionHandled() }) { Text("Not now") }
            }
        )
    }
}
