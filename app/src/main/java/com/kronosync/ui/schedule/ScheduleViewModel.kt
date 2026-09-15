package com.kronosync.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.repository.ScheduleRepository
import com.kronosync.data.repository.CheckInRepository
import com.kronosync.data.alarm.AlarmScheduler
import com.kronosync.data.alarm.Rescheduler
import com.kronosync.ui.notifications.Notifier
import com.kronosync.util.DayEpoch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import com.kronosync.domain.CopyDayUseCase

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repo: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
    private val copyDay: CopyDayUseCase,
    private val rescheduler: Rescheduler,
    private val checkInRepo: CheckInRepository,
    private val notifier: Notifier
) : ViewModel() {

    data class UiState(
        val dayEpoch: Long,
        val blocks: List<ScheduleBlock> = emptyList(),
        val needsExactAlarmPermission: Boolean = false
    )

    private val _state = MutableStateFlow(UiState(dayEpoch = DayEpoch.of(LocalDate.now())))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        observeDay()
        // Re-sync alarms on app open to maintain just-in-time chaining
        viewModelScope.launch { rescheduler.onBootCompleted() }
    }

    private fun observeDay() {
        viewModelScope.launch {
            repo.observeForDay(_state.value.dayEpoch).collectLatest { list ->
                _state.value = _state.value.copy(blocks = list)
            }
        }
    }

    fun addBlockFor(dayEpoch: Long, startMinute: Int, durationMinutes: Int, title: String, lockIn: Boolean = false) {
        viewModelScope.launch {
            val block = ScheduleBlock(
                dayEpoch = dayEpoch,
                startMinute = startMinute,
                durationMinutes = durationMinutes,
                title = title,
                lockIn = lockIn
            )
            val id = repo.add(block)
            val saved = block.copy(id = id)
            scheduleAlarm(saved)
        }
    }

    fun updateBlock(block: ScheduleBlock, startMinute: Int, durationMinutes: Int, title: String, lockIn: Boolean = block.lockIn) {
        viewModelScope.launch {
            alarmScheduler.cancel(block)
            val updated = block.copy(startMinute = startMinute, durationMinutes = durationMinutes, title = title, lockIn = lockIn)
            repo.update(updated)
            scheduleAlarm(updated)
        }
    }

    fun deleteBlock(block: ScheduleBlock) {
        viewModelScope.launch {
            alarmScheduler.cancel(block)
            repo.delete(block)
        }
    }

    private fun scheduleAlarm(block: ScheduleBlock) {
        val scheduled = alarmScheduler.scheduleExact(block)
        if (!scheduled) {
            _state.value = _state.value.copy(needsExactAlarmPermission = true)
        }
    }

    fun exactAlarmPermissionHandled() {
        _state.value = _state.value.copy(needsExactAlarmPermission = false)
    }

    fun copyDayTo(targetDayEpochs: List<Long>) {
        viewModelScope.launch {
            copyDay.copyDay(_state.value.dayEpoch, targetDayEpochs)
        }
    }

    fun copyWeekTo(targetWeekStartEpoch: Long) {
        viewModelScope.launch {
            val sourceWeekStart = com.kronosync.util.WeekStart.of(DayEpoch.toLocalDate(_state.value.dayEpoch))
            copyDay.copyWeek(DayEpoch.of(sourceWeekStart), targetWeekStartEpoch)
        }
    }

    fun copyBlockToNow(block: ScheduleBlock) {
        viewModelScope.launch {
            val now = java.time.LocalTime.now()
            val startMinute = now.hour * 60
            val todayEpoch = DayEpoch.of(LocalDate.now())
            val copy = ScheduleBlock(
                dayEpoch = todayEpoch,
                startMinute = startMinute,
                durationMinutes = block.durationMinutes,
                title = block.title,
                tag = block.tag
            )
            val id = repo.add(copy)
            scheduleAlarm(copy.copy(id = id))
        }
    }

    fun checkIn(blockId: Long, status: String) {
        viewModelScope.launch {
            checkInRepo.log(blockId, status)
        }
        notifier.cancelStartNotification(blockId)
    }
}
