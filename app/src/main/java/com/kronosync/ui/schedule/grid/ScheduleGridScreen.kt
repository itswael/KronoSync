package com.kronosync.ui.schedule.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.ui.schedule.EditBlockSheet
import com.kronosync.ui.schedule.ScheduleViewModel
import com.kronosync.ui.settings.SettingsViewModel
import com.kronosync.ui.theme.StatusColors
import com.kronosync.util.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val PX_PER_MINUTE = 0.7f
private val DAY_COLUMN_WIDTH = 108.dp
private val HOUR_RULER_WIDTH = 44.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGridScreen(
    gridVm: WeekGridViewModel = hiltViewModel(),
    scheduleVm: ScheduleViewModel = hiltViewModel(),
    settingsVm: SettingsViewModel = hiltViewModel()
) {
    val ui by gridVm.state.collectAsState()
    val use24Hour = settingsVm.state.collectAsState().value.app.use24HourClock
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var editing by remember { mutableStateOf<ScheduleBlock?>(null) }
    val editSheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize().padding(start = 8.dp)) {
        // Day header row (stays aligned with the scrollable columns below)
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(HOUR_RULER_WIDTH))
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                ui.days.forEach { day ->
                    val isToday = day.date == today
                    Column(
                        modifier = Modifier.width(DAY_COLUMN_WIDTH).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            day.date.format(DateTimeFormatter.ofPattern("d")),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        HorizontalDivider()

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val todayIndex = remember(ui.days) { ui.days.indexOfFirst { it.date == today } }
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val viewportWidthPx = with(density) { (maxWidth - HOUR_RULER_WIDTH).toPx() }

            LaunchedEffect(todayIndex) {
                if (todayIndex < 0) return@LaunchedEffect
                val columnWidthPx = with(density) { DAY_COLUMN_WIDTH.toPx() }
                val targetX = (todayIndex * columnWidthPx - (viewportWidthPx - columnWidthPx) / 2f)
                    .toInt().coerceAtLeast(0)
                horizontalScroll.animateScrollTo(targetX)

                val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
                val pxPerMinute = with(density) { PX_PER_MINUTE.dp.toPx() }
                val targetY = (nowMinute * pxPerMinute - viewportHeightPx / 2f).toInt().coerceAtLeast(0)
                verticalScroll.animateScrollTo(targetY)
            }

            Row(modifier = Modifier.fillMaxSize().verticalScroll(verticalScroll)) {
                HourRuler(use24Hour)
                Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                    ui.days.forEach { day ->
                        DayColumn(
                            day = day,
                            isDark = isDark,
                            onTap = { block -> editing = block }
                        )
                    }
                }
            }
        }
    }

    editing?.let { block ->
        EditBlockSheet(
            block = block,
            use24Hour = use24Hour,
            onDismiss = { editing = null },
            onSave = { startMinute, durationMinutes, title ->
                scheduleVm.updateBlock(block, startMinute, durationMinutes, title)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editing = null }
            },
            onDelete = {
                scheduleVm.deleteBlock(block)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editing = null }
            },
            onCopyToNow = {
                scheduleVm.copyBlockToNow(block)
                scope.launch { editSheetState.hide() }.invokeOnCompletion { editing = null }
            },
            sheetState = editSheetState
        )
    }
}

@Composable
private fun HourRuler(use24Hour: Boolean) {
    Column(modifier = Modifier.width(HOUR_RULER_WIDTH)) {
        for (hour in 0 until 24) {
            Box(modifier = Modifier.height((60 * PX_PER_MINUTE).dp)) {
                Text(
                    TimeFormat.hourLabel(hour, use24Hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp).align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun DayColumn(day: WeekGridViewModel.DayColumn, isDark: Boolean, onTap: (ScheduleBlock) -> Unit) {
    val columnHeight = (24 * 60 * PX_PER_MINUTE).dp
    Box(
        modifier = Modifier
            .width(DAY_COLUMN_WIDTH)
            .height(columnHeight)
    ) {
        // Hour gridlines
        Column(modifier = Modifier.fillMaxSize()) {
            for (hour in 0 until 24) {
                Box(modifier = Modifier.height((60 * PX_PER_MINUTE).dp).fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.align(Alignment.TopStart), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
        day.blocks.forEach { blockStatus ->
            val (container, content) = tileColors(blockStatus, isDark)
            val blockEndEpochMillis = blockStatus.block.dayEpoch +
                (blockStatus.block.startMinute + blockStatus.block.durationMinutes) * 60_000L
            val editable = blockEndEpochMillis > System.currentTimeMillis()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .offset(y = (blockStatus.block.startMinute * PX_PER_MINUTE).dp)
                    .height((blockStatus.block.durationMinutes * PX_PER_MINUTE).dp.let { if (it < 20.dp) 20.dp else it })
                    .clip(RoundedCornerShape(6.dp))
                    .background(container)
                    .clickable(enabled = editable) { onTap(blockStatus.block) }
                    .padding(4.dp)
            ) {
                Text(
                    blockStatus.block.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun tileColors(blockStatus: WeekGridViewModel.BlockStatus, isDark: Boolean): Pair<Color, Color> {
    return when (blockStatus.status) {
        "Done" -> (if (isDark) StatusColors.doneContainerDark else StatusColors.doneContainerLight) to (if (isDark) StatusColors.doneDark else StatusColors.doneLight)
        "Partial" -> (if (isDark) StatusColors.partialContainerDark else StatusColors.partialContainerLight) to (if (isDark) StatusColors.partialDark else StatusColors.partialLight)
        "Skipped" -> (if (isDark) StatusColors.skippedContainerDark else StatusColors.skippedContainerLight) to (if (isDark) StatusColors.skippedDark else StatusColors.skippedLight)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
}
