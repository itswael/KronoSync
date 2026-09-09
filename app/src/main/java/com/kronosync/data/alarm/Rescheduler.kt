package com.kronosync.data.alarm

import com.kronosync.data.db.ScheduleBlockDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Rescheduler @Inject constructor(
    private val dao: ScheduleBlockDao,
    private val alarmScheduler: AlarmScheduler
) {
    suspend fun onBootCompleted() {
        val next = dao.getNextUpcoming(Instant.now().toEpochMilli())
        if (next != null) alarmScheduler.scheduleExact(next)
    }
}
