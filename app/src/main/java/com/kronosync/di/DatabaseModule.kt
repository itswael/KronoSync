package com.kronosync.di

import android.content.Context
import android.app.AlarmManager
import androidx.room.Room
import com.kronosync.data.db.KronoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KronoDatabase =
        Room.databaseBuilder(context, KronoDatabase::class.java, "krono.db").build()

    @Provides
    fun provideScheduleBlockDao(db: KronoDatabase) = db.scheduleBlockDao()

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}
