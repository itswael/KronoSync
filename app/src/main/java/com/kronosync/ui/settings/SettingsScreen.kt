package com.kronosync.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ui = vm.state.collectAsState().value
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Backup")
        Text("Schedule: saves your planned blocks to a local file.")
        Switch(checked = ui.scheduleBackupEnabled, onCheckedChange = { vm.setScheduleBackup(it) })
        Button(onClick = { vm.backupNowSchedule() }) { Text("Backup schedule now") }
        Text("Progress: saves your recent check-ins to a local file.")
        Switch(checked = ui.progressBackupEnabled, onCheckedChange = { vm.setProgressBackup(it) })
        Button(onClick = { vm.backupNowProgress() }) { Text("Backup progress now") }

        Text("Appearance")
        Text("Dynamic color: match your wallpaper’s palette.")
        Switch(checked = ui.app.dynamicColorEnabled, onCheckedChange = { vm.setDynamicColor(it) })
        Text("AMOLED true black: deeper dark theme on OLED screens.")
        Switch(checked = ui.app.amoledTrueBlack, onCheckedChange = { vm.setAmoled(it) })

        Text("Check-in interval")
        Slider(
            value = ui.app.checkInBatchMinutes.toFloat(),
            onValueChange = { vm.setCheckInMinutes(it.toInt()) },
            valueRange = 15f..120f,
            steps = 7
        )
        Text("Quote frequency")
        Slider(
            value = ui.app.quoteFrequency.toFloat(),
            onValueChange = { vm.setQuoteFrequency(it.toInt()) },
            valueRange = 0f..3f,
            steps = 2
        )
    }
}
