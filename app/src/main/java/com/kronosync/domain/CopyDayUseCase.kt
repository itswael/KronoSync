package com.kronosync.domain

import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.db.ScheduleBlockDao
import javax.inject.Inject

class CopyDayUseCase @Inject constructor(
    private val dao: ScheduleBlockDao
) {
    /** Copies one day's blocks onto one or more target days. Rows are independent copies — never live references. */
    suspend fun copyDay(sourceDayEpoch: Long, targetDayEpochs: List<Long>) {
        val source = dao.getForDay(sourceDayEpoch)
        for (target in targetDayEpochs) {
            copyBlocksToDay(source, target)
        }
    }

    /** Copies every block in the 7-day span starting at [sourceWeekStartEpoch] onto the week starting at [targetWeekStartEpoch]. */
    suspend fun copyWeek(sourceWeekStartEpoch: Long, targetWeekStartEpoch: Long) {
        val oneDayMillis = 24L * 60 * 60 * 1000
        for (offset in 0 until 7) {
            val sourceDay = sourceWeekStartEpoch + offset * oneDayMillis
            val targetDay = targetWeekStartEpoch + offset * oneDayMillis
            val source = dao.getForDay(sourceDay)
            copyBlocksToDay(source, targetDay)
        }
    }

    private suspend fun copyBlocksToDay(source: List<ScheduleBlock>, targetDayEpoch: Long) {
        for (b in source) {
            // Independent rows: never reference source
            dao.insert(
                ScheduleBlock(
                    dayEpoch = targetDayEpoch,
                    startMinute = b.startMinute,
                    durationMinutes = b.durationMinutes,
                    title = b.title,
                    tag = b.tag,
                    templateId = b.templateId
                )
            )
        }
    }
}
