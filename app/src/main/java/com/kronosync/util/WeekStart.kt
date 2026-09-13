package com.kronosync.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * KronoSync's weeks always start on Monday, regardless of the device locale
 * (the default `WeekFields.of(Locale.getDefault())` would start on Sunday for
 * a US locale, which disagreed with the week-table grid and analytics).
 */
object WeekStart {
    fun of(dateInWeek: LocalDate): LocalDate =
        dateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
