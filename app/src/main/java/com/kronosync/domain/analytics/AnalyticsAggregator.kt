package com.kronosync.domain.analytics

import com.kronosync.data.db.DailyLogEntryDao
import com.kronosync.data.db.ScheduleBlockDao
import javax.inject.Inject

class AnalyticsAggregator @Inject constructor(
    private val dao: DailyLogEntryDao,
    private val blockDao: ScheduleBlockDao
) {
    data class Summary(val done: Int, val partial: Int, val skipped: Int)
    data class ActivitySlice(val label: String, val minutes: Int)

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

    /**
     * Minutes of scheduled time per activity (tag if set, else task title) between
     * [startDayEpochInclusive] and [endDayEpochExclusive], largest first. Everything past
     * the top [maxSlices] activities is folded into a single "Other" slice so the chart
     * stays readable even with many distinct task names.
     */
    suspend fun activityBreakdown(
        startDayEpochInclusive: Long,
        endDayEpochExclusive: Long,
        maxSlices: Int = 6
    ): List<ActivitySlice> {
        val blocks = blockDao.getBetweenDays(startDayEpochInclusive, endDayEpochExclusive)
        val byLabel = blocks
            .groupBy { it.tag?.takeIf { tag -> tag.isNotBlank() } ?: it.title }
            .mapValues { (_, blocksForLabel) -> blocksForLabel.sumOf { it.durationMinutes } }
            .toList()
            .sortedByDescending { it.second }

        if (byLabel.size <= maxSlices) {
            return byLabel.map { (label, minutes) -> ActivitySlice(label, minutes) }
        }
        val top = byLabel.take(maxSlices).map { (label, minutes) -> ActivitySlice(label, minutes) }
        val otherMinutes = byLabel.drop(maxSlices).sumOf { it.second }
        return if (otherMinutes > 0) top + ActivitySlice("Other", otherMinutes) else top
    }
}
