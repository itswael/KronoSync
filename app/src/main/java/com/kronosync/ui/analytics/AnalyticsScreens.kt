package com.kronosync.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonRow
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kronosync.domain.analytics.AnalyticsAggregator
import com.kronosync.domain.quotes.QuoteBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.temporal.WeekFields
import java.util.Locale

class AnalyticsViewModel @Inject constructor(
    private val agg: AnalyticsAggregator,
    private val quotes: QuoteBank
) : ViewModel() {
    data class UiState(val done: Int = 0, val partial: Int = 0, val skipped: Int = 0)
    var state = mutableStateOf(UiState())
        private set
    var quote = mutableStateOf("")

    fun loadDay(day: LocalDate) {
        viewModelScope.launch {
            val start = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val s = agg.daySummary(start)
            state.value = UiState(s.done, s.partial, s.skipped)
            quote.value = quotes.pick(s.done, s.partial, s.skipped)
        }
    }

    fun loadWeek(dayInWeek: LocalDate) {
        viewModelScope.launch {
            val wf = WeekFields.of(Locale.getDefault())
            val weekStart = dayInWeek.with(wf.dayOfWeek(), 1)
            val start = weekStart.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val s = agg.weekSummary(start)
            state.value = UiState(s.done, s.partial, s.skipped)
            quote.value = quotes.pick(s.done, s.partial, s.skipped)
        }
    }

    fun loadMonth(month: LocalDate) {
        viewModelScope.launch {
            val monthStart = month.withDayOfMonth(1)
            val start = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            val days = month.lengthOfMonth()
            val s = agg.monthSummary(start, days)
            state.value = UiState(s.done, s.partial, s.skipped)
            quote.value = quotes.pick(s.done, s.partial, s.skipped)
        }
    }
}

@Composable
fun DayAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadDay(LocalDate.now()) }
    val ui = vm.state.value
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card { Text("Done: ${ui.done}", Modifier.padding(16.dp)) }
        Card { Text("Partial: ${ui.partial}", Modifier.padding(16.dp)) }
        Card { Text("Skipped: ${ui.skipped}", Modifier.padding(16.dp)) }
        Card { Text(vm.quote.value, Modifier.padding(16.dp)) }
    }
}

@Composable
fun WeekAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadWeek(LocalDate.now()) }
    val ui = vm.state.value
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card { Text("Week Done: ${ui.done}", Modifier.padding(16.dp)) }
        Card { Text("Week Partial: ${ui.partial}", Modifier.padding(16.dp)) }
        Card { Text("Week Skipped: ${ui.skipped}", Modifier.padding(16.dp)) }
        Card { Text(vm.quote.value, Modifier.padding(16.dp)) }
    }
}

@Composable
fun MonthAnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadMonth(LocalDate.now()) }
    val ui = vm.state.value
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card { Text("Month Done: ${ui.done}", Modifier.padding(16.dp)) }
        Card { Text("Month Partial: ${ui.partial}", Modifier.padding(16.dp)) }
        Card { Text("Month Skipped: ${ui.skipped}", Modifier.padding(16.dp)) }
        Card { Text(vm.quote.value, Modifier.padding(16.dp)) }
    }
}

@Composable
fun AnalyticsTabScreen() {
    val (tab, setTab) = remember { mutableStateOf(0) } // 0: Day, 1: Week, 2: Month
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedButtonRow {
            SegmentedButton(selected = tab == 0, onClick = { setTab(0) }, shape = null) { Text("Day") }
            SegmentedButton(selected = tab == 1, onClick = { setTab(1) }, shape = null) { Text("Week") }
            SegmentedButton(selected = tab == 2, onClick = { setTab(2) }, shape = null) { Text("Month") }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> DayAnalyticsScreen()
            1 -> WeekAnalyticsScreen()
            else -> MonthAnalyticsScreen()
        }
    }
}
