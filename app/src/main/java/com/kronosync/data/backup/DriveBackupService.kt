package com.kronosync.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DriveBackupService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportSchedule(schedule: ScheduleBackup): Boolean = withContext(Dispatchers.IO) {
        // TODO: Integrate Google Drive API; placeholder writes to app files dir for dev
        runCatching {
            val bytes = json.encodeToString(ScheduleBackup.serializer(), schedule).toByteArray()
            val file = context.getFileStreamPath("schedule-backup.json")
            file.outputStream().use { it.write(bytes) }
        }.isSuccess
    }

    suspend fun exportProgress(progress: ProgressBackup): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = json.encodeToString(ProgressBackup.serializer(), progress).toByteArray()
            val file = context.getFileStreamPath("progress-backup.json")
            file.outputStream().use { it.write(bytes) }
        }.isSuccess
    }
}