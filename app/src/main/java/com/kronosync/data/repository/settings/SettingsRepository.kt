package com.kronosync.data.repository.settings

import com.kronosync.data.db.AppSettings
import com.kronosync.data.db.AppSettingsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dao: AppSettingsDao
) {
    fun observe(): Flow<AppSettings?> = dao.observe()
    suspend fun upsert(s: AppSettings) = dao.upsert(s)
}
