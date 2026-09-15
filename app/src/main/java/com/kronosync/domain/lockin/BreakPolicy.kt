package com.kronosync.domain.lockin

/**
 * Task-linked lock-in break rules:
 * - Tasks of 2h or less: no break, one continuous session.
 * - Tasks over 2h: break slot k (1-indexed) becomes available once *active* lock-in time
 *   (wall-clock minus any time already spent on a break) reaches k * BREAK_INTERVAL_MINUTES —
 *   and only after break (k-1) has actually been used. Breaks don't expire or force themselves;
 *   they just sit available until claimed. Ad-hoc sessions never get a break (they're capped at
 *   2h anyway, per F4).
 */
object BreakPolicy {
    const val BREAK_INTERVAL_MINUTES = 120
    const val BREAK_LENGTH_MINUTES = 20
    const val MIN_TASK_MINUTES_FOR_BREAKS = BREAK_INTERVAL_MINUTES + 1

    fun supportsBreaks(plannedMinutes: Int): Boolean = plannedMinutes >= MIN_TASK_MINUTES_FOR_BREAKS

    fun isBreakAvailable(plannedMinutes: Int, activeElapsedMinutes: Int, breaksUsed: Int): Boolean {
        if (!supportsBreaks(plannedMinutes)) return false
        val nextEligibleAt = (breaksUsed + 1) * BREAK_INTERVAL_MINUTES
        return activeElapsedMinutes in nextEligibleAt until plannedMinutes
    }
}
