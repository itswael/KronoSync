package com.kronosync.domain

import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.db.ScheduleBlockDao
import javax.inject.Inject

class CopyDayUseCase @Inject constructor(
    private val dao: ScheduleBlockDao
) {
    suspend operator fun invoke(sourceDayEpoch: Long, targetDayEpochs: List<Long>) {
        val source = dao.getForDay(sourceDayEpoch)
        for (target in targetDayEpochs) {
            for (b in source) {
                // Independent rows: never reference source
                dao.insert(
                    ScheduleBlock(
                        dayEpoch = target,
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
}
