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

    @Query("SELECT * FROM templates ORDER BY name")
    suspend fun getAll(): List<Template>
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

    @Query("SELECT * FROM schedule_blocks WHERE dayEpoch = :dayEpoch ORDER BY startMinute")
    suspend fun getForDay(dayEpoch: Long): List<ScheduleBlock>

    @Query("SELECT * FROM schedule_blocks WHERE dayEpoch >= :startInclusive AND dayEpoch < :endExclusive ORDER BY dayEpoch, startMinute")
    fun observeBetweenDays(startInclusive: Long, endExclusive: Long): Flow<List<ScheduleBlock>>

    @Query("SELECT * FROM schedule_blocks WHERE dayEpoch >= :startInclusive AND dayEpoch < :endExclusive ORDER BY dayEpoch, startMinute")
    suspend fun getBetweenDays(startInclusive: Long, endExclusive: Long): List<ScheduleBlock>

    @Query("SELECT * FROM schedule_blocks ORDER BY dayEpoch, startMinute")
    suspend fun getAll(): List<ScheduleBlock>

    @Query("SELECT * FROM schedule_blocks WHERE (dayEpoch + (startMinute * 60000)) > :nowMillis ORDER BY (dayEpoch + (startMinute * 60000)) ASC LIMIT 1")
    suspend fun getNextUpcoming(nowMillis: Long): ScheduleBlock?
}

@Dao
interface DailyLogEntryDao {
    @Insert
    suspend fun insert(entry: DailyLogEntry): Long

    @Query("SELECT * FROM daily_log_entries WHERE blockId = :blockId ORDER BY timestamp DESC")
    fun observeForBlock(blockId: Long): Flow<List<DailyLogEntry>>

    @Query("SELECT * FROM daily_log_entries WHERE timestamp BETWEEN :start AND :end")
    suspend fun getBetween(start: Long, end: Long): List<DailyLogEntry>

    @Query("SELECT MIN(timestamp) FROM daily_log_entries")
    suspend fun getFirstLogTimestamp(): Long?

    @Query("SELECT * FROM daily_log_entries ORDER BY timestamp DESC")
    suspend fun getAll(): List<DailyLogEntry>
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

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun get(): AppSettings?
}

@Dao
interface LockInSessionDao {
    @Insert
    suspend fun insert(session: LockInSession): Long

    @Update
    suspend fun update(session: LockInSession)

    @Query("SELECT * FROM lock_in_sessions WHERE id = :id")
    suspend fun get(id: Long): LockInSession?

    @Query("SELECT * FROM lock_in_sessions WHERE endedAt IS NOT NULL AND cancelled = 0 AND startedAt BETWEEN :start AND :end")
    suspend fun getCompletedBetween(start: Long, end: Long): List<LockInSession>
}
