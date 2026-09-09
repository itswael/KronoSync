package com.kronosync.ui.checkin

import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable
fun CheckInSheet(
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How did it go?")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onAction("Done") }) { Text("Done") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onAction("Partial") }) { Text("Partial") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { onAction("Skipped") }) { Text("Skipped") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
