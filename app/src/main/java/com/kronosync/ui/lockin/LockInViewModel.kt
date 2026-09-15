package com.kronosync.ui.lockin

import android.content.Context
import androidx.lifecycle.ViewModel
import com.kronosync.data.lockin.LockInForegroundService
import com.kronosync.domain.lockin.LockInPermissions
import com.kronosync.domain.lockin.LockInStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LockInViewModel @Inject constructor(
    private val stateHolder: LockInStateHolder
) : ViewModel() {
    val state: StateFlow<com.kronosync.domain.lockin.LockInUiState> = stateHolder.state

    fun hasPermissions(context: Context): Boolean = LockInPermissions.hasAllPermissions(context)

    fun startAdHoc(context: Context, minutes: Int) {
        LockInForegroundService.startCountdown(context, blockId = null, taskTitle = null, plannedMinutes = minutes)
    }

    fun cancelCountdown(context: Context) {
        LockInForegroundService.cancelCountdown(context)
    }

    fun requestBreak(context: Context) {
        LockInForegroundService.requestBreak(context)
    }

    fun stopSession(context: Context) {
        LockInForegroundService.stop(context)
    }
}
