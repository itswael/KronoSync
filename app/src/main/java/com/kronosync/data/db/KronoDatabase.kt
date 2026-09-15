package com.kronosync.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Template::class,
        ScheduleBlock::class,
        DailyLogEntry::class,
        BackupSettings::class,
        AppSettings::class
    ],
    version = 3,
    exportSchema = true
)
abstract class KronoDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun scheduleBlockDao(): ScheduleBlockDao
    abstract fun dailyLogEntryDao(): DailyLogEntryDao
    abstract fun backupSettingsDao(): BackupSettingsDao
    abstract fun appSettingsDao(): AppSettingsDao
}
