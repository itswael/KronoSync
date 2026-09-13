package com.kronosync.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A `dayEpoch` is midnight of a given calendar day **in the device's local zone**, as
 * absolute epoch millis. Blocks store `startMinute` as local wall-clock minutes since
 * midnight, so `dayEpoch + startMinute * 60_000` must be a local-midnight-based instant —
 * mixing this with UTC midnight silently shifts every alarm by the device's UTC offset
 * (this was the root cause of notifications firing immediately instead of at the block's
 * actual start time for any non-UTC timezone).
 */
object DayEpoch {
    fun of(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
}
