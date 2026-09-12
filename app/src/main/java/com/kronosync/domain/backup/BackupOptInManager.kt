package com.kronosync.domain.backup

import javax.inject.Inject
import javax.inject.Singleton
import com.kronosync.data.db.DailyLogEntryDao
import com.kronosync.data.db.AppSettingsDao
import com.kronosync.data.db.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BackupOptInManager @Inject constructor(
    private val logs: DailyLogEntryDao,
    private val appSettings: AppSettingsDao
) {
    private val threeDaysMillis = 3L * 24 * 60 * 60 * 1000

    suspend fun shouldPrompt(now: Long = System.currentTimeMillis()): Boolean = withContext(Dispatchers.IO) {
        val first = logs.getFirstLogTimestamp() ?: return@withContext false
        val s = appSettings.get() ?: AppSettings()
        if (s.backupOptInDismissed == true) return@withContext false
        // Only prompt once: if we've recorded a prompt time, don't re-prompt.
        if (s.backupOptInPromptedAt != null) return@withContext false
        return@withContext (now - first) >= threeDaysMillis
    }

    suspend fun markPromptShown(at: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val s = appSettings.get() ?: AppSettings()
        appSettings.upsert(s.copy(backupOptInPromptedAt = at))
    }

    suspend fun dismissPrompt() = withContext(Dispatchers.IO) {
        val s = appSettings.get() ?: AppSettings()
        appSettings.upsert(s.copy(backupOptInDismissed = true))
    }
}