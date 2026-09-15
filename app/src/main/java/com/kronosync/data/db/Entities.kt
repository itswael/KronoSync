package com.kronosync.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "templates",
    indices = [Index(value = ["name"], unique = true)]
)
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null
)

@Entity(tableName = "schedule_blocks")
data class ScheduleBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val dayEpoch: Long, // midnight millis in the device's local zone for the day (see util/DayEpoch)
    val startMinute: Int, // minutes from midnight
    val durationMinutes: Int,
    val title: String,
    val tag: String? = null,
    val templateId: Long? = null // provenance only; not a live link
)

@Entity(tableName = "daily_log_entries")
data class DailyLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val blockId: Long?,
    @ColumnInfo(index = true) val timestamp: Long,
    val status: String, // Done, Skipped, Partial (enum-like)
    val note: String? = null
)

@Entity(tableName = "backup_settings")
data class BackupSettings(
    @PrimaryKey val id: Int = 0,
    val scheduleBackupEnabled: Boolean = false,
    val progressBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long? = null
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0,
    val dynamicColorEnabled: Boolean = true,
    val amoledTrueBlack: Boolean = false,
    val checkInBatchMinutes: Int = 30,
    val quoteFrequency: Int = 1, // 0: off, 1: low, 2: medium, 3: high
    val backupOptInDismissed: Boolean = false,
    val backupOptInPromptedAt: Long? = null,
    val use24HourClock: Boolean = false
)
