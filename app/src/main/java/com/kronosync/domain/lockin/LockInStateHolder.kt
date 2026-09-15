package com.kronosync.domain.lockin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class LockInPhase { IDLE, COUNTDOWN, ACTIVE, ON_BREAK }

data class LockInUiState(
    val phase: LockInPhase = LockInPhase.IDLE,
    val sessionId: Long? = null,
    val blockId: Long? = null,
    val taskTitle: String? = null,
    val plannedMinutes: Int = 0,
    val startedAtMillis: Long = 0,
    val countdownSecondsRemaining: Int = 0,
    val breaksUsed: Int = 0,
    val breakEndsAtMillis: Long = 0,
    val activeElapsedMinutesAtBreakStart: Int = 0,
    val breakAvailable: Boolean = false
)

/**
 * In-process shared state between LockInForegroundService (which owns all writes) and any UI
 * observing it (persistent icon, overlay). A plain singleton StateFlow is simpler and just as
 * correct as a bound service for single-process state like this.
 */
@Singleton
class LockInStateHolder @Inject constructor() {
    private val _state = MutableStateFlow(LockInUiState())
    val state: StateFlow<LockInUiState> = _state

    fun update(transform: (LockInUiState) -> LockInUiState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = LockInUiState()
    }
}
