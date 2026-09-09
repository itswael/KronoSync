package com.kronosync.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.repository.ScheduleRepository
import com.kronosync.data.alarm.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repo: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    data class UiState(
        val dayEpoch: Long,
        val blocks: List<ScheduleBlock> = emptyList(),
        val isAdding: Boolean = false
    )

    private val _state = MutableStateFlow(
        UiState(dayEpoch = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        observeDay()
    }

    private fun observeDay() {
        viewModelScope.launch {
            repo.observeForDay(_state.value.dayEpoch).collectLatest { list ->
                _state.value = _state.value.copy(blocks = list)
            }
        }
    }

    fun addBlock(timeMinutes: Int, title: String) {
        viewModelScope.launch {
            val block = ScheduleBlock(
                dayEpoch = _state.value.dayEpoch,
                startMinute = timeMinutes,
                durationMinutes = 60,
                title = title
            )
            val id = repo.add(block)
            val saved = block.copy(id = id)
            alarmScheduler.scheduleExact(saved)
        }
    }
}
