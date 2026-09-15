package com.kronosync.ui.lockin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.domain.lockin.LockInPermissions
import com.kronosync.domain.lockin.LockInPhase

private val AdHocDurationOptions = listOf(15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockInSheet(
    onDismiss: () -> Unit,
    vm: LockInViewModel = hiltViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state.phase) {
                LockInPhase.IDLE -> IdleContent(vm, context)
                LockInPhase.COUNTDOWN -> CountdownContent(state.countdownSecondsRemaining, vm, context)
                LockInPhase.ACTIVE -> ActiveContent(state, vm, context)
                LockInPhase.ON_BREAK -> BreakContent(state)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IdleContent(vm: LockInViewModel, context: android.content.Context) {
    var showDurationPicker by remember { mutableStateOf(false) }
    var showPermissionAsk by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableStateOf(30) }

    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Lock-In Mode", style = MaterialTheme.typography.titleLarge)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "While locked in, your phone is limited to KronoSync, the camera, and phone calls — everything else is blocked until the session ends. Incoming and emergency calls always get through regardless.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "You can also turn this on for a specific task when adding or editing it, so it starts automatically at that task's scheduled time.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))

    if (!showDurationPicker) {
        Button(
            onClick = {
                if (LockInPermissions.hasAllPermissions(context)) showDurationPicker = true
                else showPermissionAsk = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start a focus session") }
    } else {
        Text("How long?", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AdHocDurationOptions) { option ->
                FilterChip(
                    selected = selectedMinutes == option,
                    onClick = { selectedMinutes = option },
                    label = { Text(if (option < 60) "${option}m" else "${option / 60}h${if (option % 60 != 0) " ${option % 60}m" else ""}") }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Capped at 2 hours for ad-hoc sessions. You'll get a 10-second countdown to cancel before it engages.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.startAdHoc(context, selectedMinutes) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Begin countdown") }
    }

    if (showPermissionAsk) {
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Lock-In needs two special permissions to see what app is in front and to show the block screen over it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(12.dp))
                if (!LockInPermissions.hasUsageAccess(context)) {
                    OutlinedButton(onClick = { LockInPermissions.openUsageAccessSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant Usage Access")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (!LockInPermissions.hasOverlayPermission(context)) {
                    OutlinedButton(onClick = { LockInPermissions.openOverlaySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant display-over-other-apps")
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownContent(secondsRemaining: Int, vm: LockInViewModel, context: android.content.Context) {
    Text("Locking in in ${secondsRemaining}s", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        "Cancel now if this wasn't intentional.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = { vm.cancelCountdown(context) }, modifier = Modifier.fillMaxWidth()) {
        Text("Cancel")
    }
}

@Composable
private fun ActiveContent(state: com.kronosync.domain.lockin.LockInUiState, vm: LockInViewModel, context: android.content.Context) {
    Text("Locked in", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    state.taskTitle?.let {
        Text("Focusing on: $it", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
    }
    Text(
        "Planned: ${state.plannedMinutes} min",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))
    if (state.breakAvailable) {
        Button(onClick = { vm.requestBreak(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Take a 20-minute break")
        }
        Spacer(Modifier.height(8.dp))
    }
    TextButton(onClick = { vm.stopSession(context) }, modifier = Modifier.fillMaxWidth()) {
        Text("End session early")
    }
}

@Composable
private fun BreakContent(state: com.kronosync.domain.lockin.LockInUiState) {
    Text("On a break", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        "Lock-in resumes automatically when the break ends — no need to do anything.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
