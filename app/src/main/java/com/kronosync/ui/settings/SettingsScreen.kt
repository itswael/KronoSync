package com.kronosync.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ui = vm.state.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        SectionHeader("Backup")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                headlineContent = { Text("Back up schedule") },
                supportingContent = { Text("Saves your planned blocks so you can restore them later.") },
                trailingContent = { Switch(checked = ui.scheduleBackupEnabled, onCheckedChange = { vm.setScheduleBackup(it) }) }
            )
            ListItem(
                leadingContent = { Icon(Icons.Outlined.CloudUpload, contentDescription = null) },
                headlineContent = { Text("Back up progress") },
                supportingContent = { Text("Saves your last 3 months of check-ins.") },
                trailingContent = { Switch(checked = ui.progressBackupEnabled, onCheckedChange = { vm.setProgressBackup(it) }) }
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { vm.backupNowSchedule() }, modifier = Modifier.fillMaxWidth()) { Text("Back up schedule now") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.backupNowProgress() }, modifier = Modifier.fillMaxWidth()) { Text("Back up progress now") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Appearance")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                headlineContent = { Text("Dynamic color") },
                supportingContent = { Text("Match your wallpaper's palette (Android 12+).") },
                trailingContent = { Switch(checked = ui.app.dynamicColorEnabled, onCheckedChange = { vm.setDynamicColor(it) }) }
            )
            ListItem(
                leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                headlineContent = { Text("AMOLED true black") },
                supportingContent = { Text("Deeper dark theme that saves battery on OLED screens.") },
                trailingContent = { Switch(checked = ui.app.amoledTrueBlack, onCheckedChange = { vm.setAmoled(it) }) }
            )
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                headlineContent = { Text("24-hour time") },
                supportingContent = { Text(if (ui.app.use24HourClock) "14:30" else "2:30 PM") },
                trailingContent = { Switch(checked = ui.app.use24HourClock, onCheckedChange = { vm.setUse24HourClock(it) }) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("Notifications")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                headlineContent = { Text("Check-in interval") },
                supportingContent = { Text("How often to batch check-in reminders: ${ui.app.checkInBatchMinutes} min") }
            )
            Slider(
                value = ui.app.checkInBatchMinutes.toFloat(),
                onValueChange = { vm.setCheckInMinutes(it.toInt()) },
                valueRange = 15f..120f,
                steps = 7,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ListItem(
                headlineContent = { Text("Quote frequency") },
                supportingContent = { Text(quoteFrequencyLabel(ui.app.quoteFrequency)) }
            )
            Slider(
                value = ui.app.quoteFrequency.toFloat(),
                onValueChange = { vm.setQuoteFrequency(it.toInt()) },
                valueRange = 0f..3f,
                steps = 2,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun quoteFrequencyLabel(freq: Int) = when (freq) {
    0 -> "Off"
    1 -> "Low"
    2 -> "Medium"
    else -> "High"
}
