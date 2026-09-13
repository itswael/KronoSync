package com.kronosync.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.ui.checkin.CheckInSheet
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    var checkInBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    var editBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
    val checkInSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val nowMinute = minutesSinceLocalMidnight(ui.dayEpoch)

    if (ui.blocks.isEmpty()) {
        EmptySchedule()
    } else {
        val currentIndex = remember(ui.blocks) {
            ui.blocks.indexOfFirst { it.startMinute <= nowMinute && nowMinute < it.startMinute + it.durationMinutes }
        }
        LaunchedEffect(ui.blocks) {
            val target = if (currentIndex >= 0) currentIndex else ui.blocks.indexOfFirst { it.startMinute >= nowMinute }
            if (target >= 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(ui.blocks, key = { _, b -> b.id }) { index, block ->
                val isCurrent = index == currentIndex
                val started = block.startMinute <= nowMinute
                val isPast = block.startMinute + block.durationMinutes <= nowMinute
                ScheduleBlockCard(
                    block = block,
                    isCurrent = isCurrent,
                    eligibleForCheckIn = started,
                    editable = !isPast,
                    onCheckIn = { checkInBlock = block },
                    onEdit = { editBlock = block }
                )
            }
        }
    }

    checkInBlock?.let { block ->
        CheckInSheet(
            onDismiss = { checkInBlock = null },
            onAction = { status ->
                vm.checkIn(block.id, status)
                scope.launch { checkInSheetState.hide() }.invokeOnCompletion { checkInBlock = null }
            },
            sheetState = checkInSheetState
        )
    }

    editBlock?.let { block ->
        EditBlockSheet(
            block = block,
            onDismiss = { editBlock = null },
            onSave = { startMinute, durationMinutes, title ->
                vm.updateBlock(block, startMinute, durationMinutes, title)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editBlock = null }
            },
            onDelete = {
                vm.deleteBlock(block)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editBlock = null }
            },
            onCopyToNow = {
                vm.copyBlockToNow(block)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editBlock = null }
            },
            sheetState = editSheetState
        )
    }
}

@Composable
private fun ScheduleBlockCard(
    block: ScheduleBlock,
    isCurrent: Boolean,
    eligibleForCheckIn: Boolean,
    editable: Boolean,
    onCheckIn: () -> Unit,
    onEdit: () -> Unit
) {
    val containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = eligibleForCheckIn, onClick = onCheckIn),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.size(width = 56.dp, height = 40.dp)) {
                Text(formatMinutes(block.startMinute), style = MaterialTheme.typography.titleSmall, color = contentColor)
                Text(
                    formatMinutes(block.startMinute + block.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (isCurrent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Now", style = MaterialTheme.typography.labelSmall, color = contentColor)
                    }
                }
                Text(block.title, style = MaterialTheme.typography.titleMedium, color = contentColor)
                block.tag?.let { tag ->
                    Spacer(Modifier.height(4.dp))
                    SuggestionChip(onClick = {}, label = { Text(tag) })
                }
            }
            if (eligibleForCheckIn) {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "Tap to check in", tint = contentColor)
            }
            if (editable) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit task", tint = contentColor)
                }
            }
        }
    }
}

@Composable
private fun EmptySchedule() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text("Nothing planned yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Let's block out your day — tap + to add the first task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun minutesSinceLocalMidnight(dayEpoch: Long): Int {
    val diff = System.currentTimeMillis() - dayEpoch
    return (diff / 60_000L).toInt()
}

internal fun formatMinutes(m: Int): String {
    val mm = ((m % (24 * 60)) + (24 * 60)) % (24 * 60)
    val h = mm / 60
    val min = mm % 60
    return String.format("%02d:%02d", h, min)
}
