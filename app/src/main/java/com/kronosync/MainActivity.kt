package com.kronosync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.ui.settings.SettingsViewModel
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint
import com.kronosync.ui.theme.KronoSyncTheme
import com.kronosync.ui.navigation.KronoNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KronoSyncTheme {
                val vm: SettingsViewModel = hiltViewModel()
                var showOptIn by remember { mutableStateOf(false) }
                

                // Lightweight: check once on launch; real usage tracked via logs.
                LaunchedEffect(Unit) {
                    if (vm.shouldPromptBackup()) {
                        showOptIn = true
                        vm.markBackupPromptShown()
                    }
                }

                val navController = KronoNavHost()

                if (showOptIn) {
                    AlertDialog(
                        onDismissRequest = { showOptIn = false },
                        title = { Text("Optional backup to Drive?") },
                        text = { Text("After a few days, some folks like a safety net. You can back up your schedule and progress to Google Drive anytime. Totally optional.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showOptIn = false
                                navController.navigate("settings")
                            }) { Text("Show me options") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                vm.dismissBackupPrompt()
                                showOptIn = false
                            }) { Text("Not now") }
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewApp() {
    KronoSyncTheme {
        Text("KronoSync")
    }
}
