package com.kronosync.ui.schedule.dial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.ui.schedule.ScheduleViewModel
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScheduleDialScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui = vm.state.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight: Dp = maxHeight
        val centerPad = viewportHeight / 2
        val dpPerMinute = 1.dp
        val pxPerMinute = with(LocalDensity.current) { dpPerMinute.toPx() }
        val state = rememberLazyListState()

        val segments = remember(ui.value.blocks) { buildSegments(ui.value.blocks) }
        var currentMinute by remember { mutableIntStateOf(minutesSinceLocalMidnight(ui.value.dayEpoch)) }

        // Auto-scroll to keep current time centered
        LaunchedEffect(ui.value.dayEpoch, segments) {
            while (true) {
                currentMinute = minutesSinceLocalMidnight(ui.value.dayEpoch)
                val (index, offsetPx) = indexAndOffsetForMinute(segments, currentMinute, pxPerMinute)
                // Center by using content padding equal to half viewport height
                state.scrollToItem(index, offsetPx)
                delay(1000L)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(top = centerPad, bottom = centerPad),
                modifier = Modifier.fillMaxSize()
            ) {
                items(segments) { seg ->
                    when (seg) {
                        is BlockSegment -> {
                            val centerMinute = seg.blk.startMinute + seg.blk.durationMinutes / 2
                            val dist = kotlin.math.abs(centerMinute - currentMinute)
                            val scale = scaleForDistance(dist)
                            BlockItem(seg.blk, dpPerMinute, isCurrent = isCurrent(seg.blk, currentMinute), scale = scale)
                        }
                        is GapSegment -> GapItem(seg.minutes, dpPerMinute)
                    }
                }
            }

            // Center indicator line
            Spacer(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            )
        }
    }
}

private sealed interface Segment { val minutes: Int }
private data class GapSegment(override val minutes: Int) : Segment
private data class BlockSegment(val blk: ScheduleBlock) : Segment { override val minutes: Int = blk.durationMinutes }

private fun buildSegments(blocks: List<ScheduleBlock>): List<Segment> {
    if (blocks.isEmpty()) return listOf(GapSegment(24 * 60))
    val sorted = blocks.sortedBy { it.startMinute }
    val res = mutableListOf<Segment>()
    var cursor = 0
    for (b in sorted) {
        if (b.startMinute > cursor) {
            res.add(GapSegment(b.startMinute - cursor))
        }
        res.add(BlockSegment(b))
        cursor = b.startMinute + b.durationMinutes
    }
    if (cursor < 24 * 60) res.add(GapSegment(24 * 60 - cursor))
    return res
}

private fun indexAndOffsetForMinute(segments: List<Segment>, minute: Int, pxPerMinute: Float): Pair<Int, Int> {
    var remaining = minute.coerceIn(0, 24 * 60)
    var idx = 0
    for (seg in segments) {
        if (remaining < seg.minutes) {
            val offsetPx = (remaining * pxPerMinute).toInt()
            return idx to offsetPx
        } else {
            remaining -= seg.minutes
            idx += 1
        }
    }
    // End of day
    return (segments.size - 1) to 0
}

@Composable
private fun BlockItem(block: ScheduleBlock, dpPerMinute: Dp, isCurrent: Boolean, scale: Float) {
    val height = dpPerMinute * block.durationMinutes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleStyle = MaterialTheme.typography.titleMedium
        val scaledTitle = titleStyle.copy(
            fontSize = titleStyle.fontSize * scale,
            fontWeight = if (isCurrent) FontWeight.Bold else titleStyle.fontWeight
        )
        Text(
            text = block.title,
            style = scaledTitle,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${formatMinutes(block.startMinute)} – ${formatMinutes(block.startMinute + block.durationMinutes)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize * (0.9f * scale)),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GapItem(minutes: Int, dpPerMinute: Dp) {
    val height = dpPerMinute * minutes
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    )
}

private fun minutesSinceLocalMidnight(dayEpoch: Long): Int {
    val nowMillis = System.currentTimeMillis()
    val diff = nowMillis - dayEpoch
    return (diff / 60_000L).toInt().coerceIn(0, 24 * 60)
}

private fun isCurrent(block: ScheduleBlock, nowMinute: Int): Boolean {
    return nowMinute in block.startMinute until (block.startMinute + block.durationMinutes)
}

private fun formatMinutes(m: Int): String {
    val mm = ((m % (24 * 60)) + (24 * 60)) % (24 * 60)
    val h = mm / 60
    val min = mm % 60
    return String.format("%02d:%02d", h, min)
}

private fun scaleForDistance(distanceMinutes: Int): Float {
    val maxScale = 1.25f
    val minScale = 0.85f
    val cap = 120f
    val d = distanceMinutes.toFloat().coerceAtMost(cap)
    val t = d / cap
    return maxScale - t * (maxScale - minScale)
}
