package com.kronosync.data.repository.settings

import com.kronosync.data.backup.DriveBackupService
import com.kronosync.data.backup.ProgressBackup
import com.kronosync.data.backup.ScheduleBackup
import com.kronosync.data.db.BackupSettings
import com.kronosync.data.db.BackupSettingsDao
import com.kronosync.data.repository.CheckInRepository
import com.kronosync.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val backupSettingsDao: BackupSettingsDao,
    private val drive: DriveBackupService,
    private val scheduleRepo: ScheduleRepository,
    private val checkInRepo: CheckInRepository
) {
    fun observe(): Flow<BackupSettings?> = backupSettingsDao.observe()
    suspend fun upsert(s: BackupSettings) = backupSettingsDao.upsert(s)

    suspend fun backupSchedule(): Boolean {
        // Respect toggle: quietly no-op when disabled.
        val s = observe().first()
        if (s?.scheduleBackupEnabled != true) return true
        val templates = emptyList<com.kronosync.data.backup.TemplateDTO>()
        val blocks = scheduleRepo.getAllBlocks().map {
            com.kronosync.data.backup.ScheduleBlockDTO(
                id = it.id,
                dayEpoch = it.dayEpoch,
                startMinute = it.startMinute,
                title = it.title,
                tag = it.tag
            )
        }
        return drive.exportSchedule(ScheduleBackup(templates, blocks))
    }

    suspend fun backupProgress(): Boolean {
        val s = observe().first()
        if (s?.progressBackupEnabled != true) return true
        val entries = checkInRepo.getAllEntries().map {
            com.kronosync.data.backup.DailyLogEntryDTO(
                id = it.id,
                blockId = it.blockId,
                timestamp = it.timestamp,
                status = it.status
            )
        }
        return drive.exportProgress(ProgressBackup(entries))
    }
}
