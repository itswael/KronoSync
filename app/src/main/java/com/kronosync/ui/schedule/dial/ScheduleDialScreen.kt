package com.kronosync.ui.schedule.dial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.domain.quotes.QuoteBank
import com.kronosync.ui.schedule.ScheduleViewModel
import com.kronosync.ui.schedule.formatMinutes
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScheduleDialScreen(vm: ScheduleViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    var nowMinute by remember { mutableIntStateOf(minutesSinceLocalMidnight(ui.dayEpoch)) }

    LaunchedEffect(ui.dayEpoch) {
        while (true) {
            nowMinute = minutesSinceLocalMidnight(ui.dayEpoch)
            delay(15_000L)
        }
    }

    val sorted = remember(ui.blocks) { ui.blocks.sortedBy { it.startMinute } }
    val current = sorted.firstOrNull { it.startMinute <= nowMinute && nowMinute < it.startMinute + it.durationMinutes }
    val next = sorted.firstOrNull { it.startMinute > nowMinute }
    val quote = remember { QuoteBank().pick(0, 0, 0) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(28.dp))
        FocusRing(current = current, next = next, nowMinute = nowMinute)
        Spacer(Modifier.height(36.dp))
        QuoteCard(quote)
    }
}

@Composable
private fun QuoteCard(quote: String) {
    Card(
        modifier = Modifier.padding(horizontal = 28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                quote,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun FocusRing(current: ScheduleBlock?, next: ScheduleBlock?, nowMinute: Int) {
    val diameter = 268.dp
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        val fraction = if (current != null) {
            ((nowMinute - current.startMinute).toFloat() / current.durationMinutes).coerceIn(0f, 1f)
        } else 0f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = Size(size.width - stroke.width, size.height - stroke.width),
                style = stroke
            )
            if (current != null) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = Size(size.width - stroke.width, size.height - stroke.width),
                    style = stroke
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(36.dp)) {
            if (current != null) {
                val minutesLeft = (current.startMinute + current.durationMinutes - nowMinute).coerceAtLeast(0)
                Text(
                    current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatMinutes(current.startMinute)} – ${formatMinutes(current.startMinute + current.durationMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$minutesLeft min left",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (next != null) {
                val minutesUntil = next.startMinute - nowMinute
                Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(10.dp))
                Text("Free time", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${next.title} in ${formatDuration(minutesUntil)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(10.dp))
                Text("All clear", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Nothing left scheduled today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

private fun minutesSinceLocalMidnight(dayEpoch: Long): Int {
    val diff = System.currentTimeMillis() - dayEpoch
    return (diff / 60_000L).toInt().coerceIn(0, 24 * 60)
}
