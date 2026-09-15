package com.kronosync.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kronosync.domain.analytics.AnalyticsAggregator
import com.kronosync.domain.quotes.QuoteBank
import com.kronosync.ui.theme.StatusColors
import com.kronosync.util.DayEpoch
import com.kronosync.util.WeekStart
import java.time.LocalDate
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

private enum class Trend { UP, DOWN, FLAT }

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val agg: AnalyticsAggregator,
    private val quotes: QuoteBank
) : ViewModel() {
    data class UiState(
        val done: Int = 0,
        val partial: Int = 0,
        val skipped: Int = 0,
        val previousDone: Int = 0,
        val previousPartial: Int = 0,
        val previousSkipped: Int = 0,
        val activity: List<AnalyticsAggregator.ActivitySlice> = emptyList(),
        val lockIn: AnalyticsAggregator.LockInSummary = AnalyticsAggregator.LockInSummary(0, 0)
    )

    var state = mutableStateOf(UiState())
        private set
    var quote = mutableStateOf("")
        private set

    fun loadDay(day: LocalDate) {
        viewModelScope.launch {
            val start = DayEpoch.of(day)
            val prevStart = DayEpoch.of(day.minusDays(1))
            val lockIn = agg.lockInSummary(start, DayEpoch.of(day.plusDays(1)) - 1)
            apply(agg.daySummary(start), agg.daySummary(prevStart), emptyList(), lockIn)
        }
    }

    fun loadWeek(dayInWeek: LocalDate) {
        viewModelScope.launch {
            val weekStart = WeekStart.of(dayInWeek)
            val start = DayEpoch.of(weekStart)
            val prevStart = DayEpoch.of(weekStart.minusWeeks(1))
            val end = DayEpoch.of(weekStart.plusWeeks(1))
            val activity = agg.activityBreakdown(start, end)
            val lockIn = agg.lockInSummary(start, end - 1)
            apply(agg.weekSummary(start), agg.weekSummary(prevStart), activity, lockIn)
        }
    }

    fun loadMonth(month: LocalDate) {
        viewModelScope.launch {
            val monthStart = month.withDayOfMonth(1)
            val start = DayEpoch.of(monthStart)
            val days = month.lengthOfMonth()
            val prevMonthStart = monthStart.minusMonths(1)
            val prevStart = DayEpoch.of(prevMonthStart)
            val end = DayEpoch.of(monthStart.plusMonths(1))
            val activity = agg.activityBreakdown(start, end)
            val lockIn = agg.lockInSummary(start, end - 1)
            apply(agg.monthSummary(start, days), agg.monthSummary(prevStart, prevMonthStart.lengthOfMonth()), activity, lockIn)
        }
    }

    private fun apply(
        current: AnalyticsAggregator.Summary,
        previous: AnalyticsAggregator.Summary,
        activity: List<AnalyticsAggregator.ActivitySlice>,
        lockIn: AnalyticsAggregator.LockInSummary
    ) {
        state.value = UiState(
            done = current.done,
            partial = current.partial,
            skipped = current.skipped,
            previousDone = previous.done,
            previousPartial = previous.partial,
            previousSkipped = previous.skipped,
            activity = activity,
            lockIn = lockIn
        )
        quote.value = quotes.pick(current.done, current.partial, current.skipped)
    }
}

private fun formatLockInMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@Composable
private fun trendFor(current: Int, previous: Int): Trend = when {
    current > previous -> Trend.UP
    current < previous -> Trend.DOWN
    else -> Trend.FLAT
}

@Composable
private fun TrendLine(label: String, current: Int, previous: Int) {
    val trend = trendFor(current, previous)
    val (icon, text) = when (trend) {
        Trend.UP -> Icons.AutoMirrored.Outlined.TrendingUp to "Up from $previous $label"
        Trend.DOWN -> Icons.AutoMirrored.Outlined.TrendingDown to "Down from $previous $label"
        Trend.FLAT -> Icons.AutoMirrored.Outlined.TrendingFlat to "On par with $label"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.height(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.height(8.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun AnalyticsBody(ui: AnalyticsViewModel.UiState, quote: String, periodLabel: String) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
                label = "Done",
                count = ui.done,
                containerColor = if (isDark) StatusColors.doneContainerDark else StatusColors.doneContainerLight,
                contentColor = if (isDark) StatusColors.doneDark else StatusColors.doneLight
            )
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.PauseCircle,
                label = "Partial",
                count = ui.partial,
                containerColor = if (isDark) StatusColors.partialContainerDark else StatusColors.partialContainerLight,
                contentColor = if (isDark) StatusColors.partialDark else StatusColors.partialLight
            )
            StatTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.RadioButtonUnchecked,
                label = "Skipped",
                count = ui.skipped,
                containerColor = if (isDark) StatusColors.skippedContainerDark else StatusColors.skippedContainerLight,
                contentColor = if (isDark) StatusColors.skippedDark else StatusColors.skippedLight
            )
        }
        TrendLine(periodLabel, ui.done, ui.previousDone)

        if (ui.lockIn.totalMinutes > 0) {
            Card {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time in Lock-In", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Task-linked ${ui.lockIn.taskLinkedMinutes}m · Ad-hoc ${ui.lockIn.adHocMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatLockInMinutes(ui.lockIn.totalMinutes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (ui.activity.isNotEmpty()) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    ActivityBreakdown(slices = ui.activity, isDark = isDark)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    quote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DayAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadDay(LocalDate.now()) }
    AnalyticsBody(vm.state.value, vm.quote.value, "yesterday")
}

@Composable
fun WeekAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadWeek(LocalDate.now()) }
    AnalyticsBody(vm.state.value, vm.quote.value, "last week")
}

@Composable
fun MonthAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadMonth(LocalDate.now()) }
    AnalyticsBody(vm.state.value, vm.quote.value, "last month")
}

@Composable
fun AnalyticsTabScreen() {
    val (tab, setTab) = remember { mutableStateOf(0) } // 0: Day, 1: Week, 2: Month

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            SegmentedButton(
                selected = tab == 0,
                onClick = { setTab(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Day") }
            SegmentedButton(
                selected = tab == 1,
                onClick = { setTab(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Week") }
            SegmentedButton(
                selected = tab == 2,
                onClick = { setTab(2) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("Month") }
        }
        when (tab) {
            0 -> DayAnalyticsScreen()
            1 -> WeekAnalyticsScreen()
            else -> MonthAnalyticsScreen()
        }
    }
}
