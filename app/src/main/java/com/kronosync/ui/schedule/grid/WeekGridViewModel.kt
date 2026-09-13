package com.kronosync.ui.schedule.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.repository.CheckInRepository
import com.kronosync.data.repository.ScheduleRepository
import com.kronosync.util.DayEpoch
import com.kronosync.util.WeekStart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeekGridViewModel @Inject constructor(
    private val scheduleRepo: ScheduleRepository,
    private val checkInRepo: CheckInRepository
) : ViewModel() {

    data class BlockStatus(val block: ScheduleBlock, val status: String?)
    data class DayColumn(val date: LocalDate, val dayEpoch: Long, val blocks: List<BlockStatus>)
    data class UiState(val weekStart: LocalDate = WeekStart.of(LocalDate.now()), val days: List<DayColumn> = emptyList())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        observeWeek()
    }

    private fun observeWeek() {
        viewModelScope.launch {
            val weekStart = _state.value.weekStart
            val startEpoch = DayEpoch.of(weekStart)
            val endEpochExclusive = DayEpoch.of(weekStart.plusDays(7))
            scheduleRepo.observeBetweenDays(startEpoch, endEpochExclusive).collectLatest { blocks ->
                val logs = checkInRepo.getAllEntries()
                val latestStatusByBlock = logs
                    .filter { it.blockId != null }
                    .groupBy { it.blockId }
                    .mapValues { (_, entries) -> entries.maxByOrNull { it.timestamp }?.status }

                val days = (0 until 7).map { offset ->
                    val date = weekStart.plusDays(offset.toLong())
                    val dEpoch = DayEpoch.of(date)
                    val dayBlocks = blocks
                        .filter { it.dayEpoch == dEpoch }
                        .sortedBy { it.startMinute }
                        .map { BlockStatus(it, latestStatusByBlock[it.id]) }
                    DayColumn(date, dEpoch, dayBlocks)
                }
                _state.value = _state.value.copy(days = days)
            }
        }
    }
}
