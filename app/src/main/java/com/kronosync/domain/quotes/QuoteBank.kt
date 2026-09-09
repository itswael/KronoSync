package com.kronosync.domain.quotes

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteBank @Inject constructor() {
    private val encouraging = listOf(
        "Small steps add up.",
        "You’re doing great — keep going.",
        "Progress over perfection.",
        "One block at a time."
    )
    private val gentle = listOf(
        "It’s okay to adjust.",
        "Be kind to yourself.",
        "Tomorrow’s another chance.",
        "Listen to your energy."
    )
    private val neutral = listOf(
        "Focus on the next move.",
        "Make it simple.",
        "Start small, start now.",
        "Keep a steady pace."
    )

    fun pick(done: Int, partial: Int, skipped: Int): String {
        return when {
            done > skipped -> encouraging.random()
            skipped > done -> gentle.random()
            else -> neutral.random()
        }
    }
}
