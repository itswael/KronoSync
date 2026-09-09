package com.kronosync.domain.analytics

import com.kronosync.data.db.DailyLogEntryDao
import javax.inject.Inject
import kotlin.math.max

class AnalyticsAggregator @Inject constructor(
    private val dao: DailyLogEntryDao
) {
    data class Summary(val done: Int, val partial: Int, val skipped: Int)

    suspend fun summaryBetween(start: Long, end: Long): Summary {
        val logs = dao.getBetween(start, end)
        var done = 0
        var partial = 0
        var skipped = 0
        for (e in logs) {
            when (e.status) {
                "Done" -> done++
                "Partial" -> partial++
                "Skipped" -> skipped++
            }
        }
        return Summary(done, partial, skipped)
    }

    suspend fun daySummary(dayEpoch: Long): Summary = summaryBetween(dayEpoch, dayEpoch + 86_400_000 - 1)

    suspend fun weekSummary(weekStartEpoch: Long): Summary = summaryBetween(weekStartEpoch, weekStartEpoch + 7L * 86_400_000 - 1)

    suspend fun monthSummary(monthStartEpoch: Long, daysInMonth: Int): Summary = summaryBetween(monthStartEpoch, monthStartEpoch + daysInMonth * 86_400_000L - 1)
}
