package com.kronosync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
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
        enableEdgeToEdge()
        setContent {
            val vm: SettingsViewModel = hiltViewModel()
            val settings = vm.state.collectAsState().value

            KronoSyncTheme(
                dynamicColor = settings.app.dynamicColorEnabled,
                amoledTrueBlack = settings.app.amoledTrueBlack
            ) {
                var showOptIn by remember { mutableStateOf(false) }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* no-op: user's choice either way, nothing to react to here */ }

                LaunchedEffect(Unit) {
                    if (vm.shouldPromptBackup()) {
                        showOptIn = true
                        vm.markBackupPromptShown()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
