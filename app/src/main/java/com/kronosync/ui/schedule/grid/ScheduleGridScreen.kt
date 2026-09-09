package com.kronosync.ui.schedule.grid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.ui.schedule.ScheduleViewModel

@Composable
fun ScheduleGridScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui = vm.state.collectAsState()
    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
        items(ui.value.blocks) { block ->
            ListItem(headlineContent = { Text(block.title) }, supportingContent = { Text("@" + block.startMinute) })
        }
    }
}
