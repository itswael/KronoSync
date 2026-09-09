package com.kronosync.data.repository.settings

import com.kronosync.data.db.BackupSettings
import com.kronosync.data.backup.DriveBackupService
import com.kronosync.data.backup.ProgressBackup
import com.kronosync.data.backup.ScheduleBackup
import com.kronosync.data.repository.ScheduleRepository
import com.kronosync.data.repository.CheckInRepository
import com.kronosync.data.db.BackupSettingsDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
    private val backupSettingsDao: BackupSettingsDao,
    private val drive: DriveBackupService,
    private val scheduleRepo: ScheduleRepository,
    private val checkInRepo: CheckInRepository
@Singleton
class BackupRepository @Inject constructor(
    private val dao: BackupSettingsDao

    suspend fun backupSchedule(): Boolean {
        val templates = scheduleRepo.getAllTemplates().map { com.kronosync.data.backup.TemplateDTO(it.id, it.name) }
        val blocks = scheduleRepo.getAllBlocks().map {
            com.kronosync.data.backup.ScheduleBlockDTO(
                id = it.id,
                dayEpoch = it.dayEpoch,
                startMinutes = it.startMinutes,
                title = it.title,
                tag = it.tag
            )
        }
        return drive.exportSchedule(ScheduleBackup(templates, blocks))
    }

    suspend fun backupProgress(): Boolean {
        val entries = checkInRepo.getAllEntries().map {
            com.kronosync.data.backup.DailyLogEntryDTO(
                id = it.id,
                blockId = it.blockId,
                dayEpoch = it.dayEpoch,
                status = it.status.name
            )
        }
        return drive.exportProgress(ProgressBackup(entries))
    }
) {
    fun observe(): Flow<BackupSettings?> = dao.observe()
    suspend fun upsert(s: BackupSettings) = dao.upsert(s)
}
