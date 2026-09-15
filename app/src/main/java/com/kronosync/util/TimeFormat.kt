package com.kronosync.util

/** Single source of truth for rendering a clock time, honoring the user's 12h/24h setting. */
object TimeFormat {
    /** [minutes] is minutes since local midnight, wrapped into [0, 1440). */
    fun minutesLabel(minutes: Int, use24Hour: Boolean): String {
        val mm = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val h = mm / 60
        val min = mm % 60
        return if (use24Hour) {
            String.format("%02d:%02d", h, min)
        } else {
            val period = if (h < 12) "AM" else "PM"
            val h12 = when (h % 12) { 0 -> 12; else -> h % 12 }
            String.format("%d:%02d %s", h12, min, period)
        }
    }

    fun hourLabel(hour: Int, use24Hour: Boolean): String {
        val h = ((hour % 24) + 24) % 24
        return if (use24Hour) {
            String.format("%02d:00", h)
        } else when {
            h == 0 -> "12 AM"
            h < 12 -> "$h AM"
            h == 12 -> "12 PM"
            else -> "${h - 12} PM"
        }
    }
}
