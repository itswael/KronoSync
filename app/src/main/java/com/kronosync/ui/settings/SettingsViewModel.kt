package com.kronosync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kronosync.data.db.AppSettings
import com.kronosync.data.repository.settings.SettingsRepository
import com.kronosync.data.repository.settings.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val backupRepo: BackupRepository
) : ViewModel() {
    data class UiState(
        val app: AppSettings = AppSettings(),
        val scheduleBackupEnabled: Boolean = false,
        val progressBackupEnabled: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            settingsRepo.observe().collectLatest { s ->
                _state.value = _state.value.copy(app = s ?: AppSettings())
            }
        }
        viewModelScope.launch {
            backupRepo.observe().collectLatest { b ->
                _state.value = _state.value.copy(
                    scheduleBackupEnabled = b?.scheduleBackupEnabled ?: false,
                    progressBackupEnabled = b?.progressBackupEnabled ?: false
                )
            }
        }
    }

    fun setScheduleBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupRepo.upsert(
                com.kronosync.data.db.BackupSettings(
                    scheduleBackupEnabled = enabled,
                    progressBackupEnabled = _state.value.progressBackupEnabled
                )
            )
        }
    }

    fun setProgressBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupRepo.upsert(
                com.kronosync.data.db.BackupSettings(
                    scheduleBackupEnabled = _state.value.scheduleBackupEnabled,
                    progressBackupEnabled = enabled
                )
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.upsert(_state.value.app.copy(dynamicColorEnabled = enabled)) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.upsert(_state.value.app.copy(amoledTrueBlack = enabled)) }
    }

    fun setCheckInMinutes(mins: Int) {
        viewModelScope.launch { settingsRepo.upsert(_state.value.app.copy(checkInBatchMinutes = mins)) }
    }

    fun setQuoteFrequency(freq: Int) {
        viewModelScope.launch { settingsRepo.upsert(_state.value.app.copy(quoteFrequency = freq)) }
    }

    fun backupNowSchedule() {
        viewModelScope.launch { backupRepo.backupSchedule() }
    }

    fun backupNowProgress() {
        viewModelScope.launch { backupRepo.backupProgress() }
    }
}
