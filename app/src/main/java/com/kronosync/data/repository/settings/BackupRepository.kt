package com.kronosync.data.repository.settings

import com.kronosync.data.db.BackupSettings
import com.kronosync.data.db.BackupSettingsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val dao: BackupSettingsDao
) {
    fun observe(): Flow<BackupSettings?> = dao.observe()
    suspend fun upsert(s: BackupSettings) = dao.upsert(s)
}
