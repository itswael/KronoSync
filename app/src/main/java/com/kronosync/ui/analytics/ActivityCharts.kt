package com.kronosync.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kronosync.domain.analytics.AnalyticsAggregator.ActivitySlice

/** Fixed categorical palette (not tied to Done/Partial/Skipped tone rules — these are neutral activity labels, not judgments). */
private val PaletteLight = listOf(
    Color(0xFF006C58), Color(0xFF3F6180), Color(0xFF8A5A00),
    Color(0xFF6B5C9E), Color(0xFF7A5548), Color(0xFF00696D), Color(0xFF707973)
)
private val PaletteDark = listOf(
    Color(0xFF5CDBBA), Color(0xFFA9CBEC), Color(0xFFFFC26B),
    Color(0xFFCBBEFF), Color(0xFFE7BDAF), Color(0xFF4FD8DE), Color(0xFFBFC9C2)
)

private fun formatHoursMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@Composable
fun ActivityBreakdown(slices: List<ActivitySlice>, isDark: Boolean) {
    if (slices.isEmpty()) return
    val palette = if (isDark) PaletteDark else PaletteLight
    val total = slices.sumOf { it.minutes }.coerceAtLeast(1)

    Column {
        Text("Where the time went", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActivityPieChart(
                slices = slices,
                colors = palette,
                total = total,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                slices.forEachIndexed { index, slice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(palette[index % palette.size])
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            slice.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            formatHoursMinutes(slice.minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ActivityBarChart(slices = slices, colors = palette, total = total)
    }
}

@Composable
private fun ActivityPieChart(
    slices: List<ActivitySlice>,
    colors: List<Color>,
    total: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val diameter = kotlin.math.min(size.width, size.height)
        val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        slices.forEachIndexed { index, slice ->
            val sweep = 360f * slice.minutes / total
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun ActivityBarChart(slices: List<ActivitySlice>, colors: List<Color>, total: Int) {
    val maxMinutes = remember(slices) { slices.maxOf { it.minutes }.coerceAtLeast(1) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEachIndexed { index, slice ->
            val fraction = slice.minutes.toFloat() / maxMinutes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    slice.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(72.dp),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0.03f, 1f))
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors[index % colors.size])
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    formatHoursMinutes(slice.minutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
