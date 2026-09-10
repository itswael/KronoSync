package com.kronosync.ui.schedule

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import com.kronosync.ui.schedule.grid.ScheduleGridScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
    val (isGrid, setIsGrid) = remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Schedule") },
        colors = TopAppBarDefaults.topAppBarColors(),
        actions = {
            IconButton(onClick = { setIsGrid(!isGrid) }) {
                Icon(if (isGrid) Icons.Outlined.ViewList else Icons.Outlined.GridView, contentDescription = null)
            }
            IconButton(onClick = { /* TODO copy-day action */ }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy day")
            }
        }
    )

    if (isGrid) {
        ScheduleGridScreen()
    } else {
        ScheduleListScreen()
    }
}
