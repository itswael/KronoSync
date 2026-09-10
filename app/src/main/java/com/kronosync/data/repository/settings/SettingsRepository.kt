package com.kronosync.data.repository.settings

import com.kronosync.data.db.AppSettings
import com.kronosync.data.db.AppSettingsDao
import com.kronosync.data.db.TemplateDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dao: AppSettingsDao,
    private val templateDao: TemplateDao
) {
    fun observe(): Flow<AppSettings?> = dao.observe()
    suspend fun upsert(s: AppSettings) = dao.upsert(s)
    suspend fun getAllTemplates() = templateDao.getAll()
}
