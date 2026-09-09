package com.kronosync.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: Template): Long

    @Update
    suspend fun update(template: Template)

    @Delete
    suspend fun delete(template: Template)

    @Query("SELECT * FROM templates ORDER BY name")
    fun observeAll(): Flow<List<Template>>
}

@Dao
interface ScheduleBlockDao {
    @Insert
    suspend fun insert(block: ScheduleBlock): Long

    @Update
    suspend fun update(block: ScheduleBlock)

    @Delete
    suspend fun delete(block: ScheduleBlock)

    @Query("SELECT * FROM schedule_blocks WHERE dayEpoch = :dayEpoch ORDER BY startMinute")
    fun observeForDay(dayEpoch: Long): Flow<List<ScheduleBlock>>
}

@Dao
interface DailyLogEntryDao {
    @Insert
    suspend fun insert(entry: DailyLogEntry): Long

    @Query("SELECT * FROM daily_log_entries WHERE blockId = :blockId ORDER BY timestamp DESC")
    fun observeForBlock(blockId: Long): Flow<List<DailyLogEntry>>
}

@Dao
interface BackupSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BackupSettings)

    @Query("SELECT * FROM backup_settings WHERE id = 0")
    fun observe(): Flow<BackupSettings?>
}

@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettings)

    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun observe(): Flow<AppSettings?>
}
