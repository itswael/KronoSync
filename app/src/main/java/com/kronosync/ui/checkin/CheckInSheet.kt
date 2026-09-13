package com.kronosync.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kronosync.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How did it go?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "No wrong answer here — this just helps your trends.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onAction("Done") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) StatusColors.doneContainerDark else StatusColors.doneContainerLight,
                    contentColor = if (isDark) StatusColors.doneDark else StatusColors.doneLight
                )
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Done")
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onAction("Partial") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) StatusColors.partialContainerDark else StatusColors.partialContainerLight,
                    contentColor = if (isDark) StatusColors.partialDark else StatusColors.partialLight
                )
            ) {
                Icon(Icons.Outlined.PauseCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Partly done")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { onAction("Skipped") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skipped for now")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
