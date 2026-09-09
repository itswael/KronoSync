package com.kronosync.ui.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyDaySheet(
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var targets = remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Copy this day to (comma-separated YYYY-MM-DD)")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = targets.value, onValueChange = { targets.value = it }, label = { Text("Targets") })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onCopy(targets.value) }) { Text("Copy") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
