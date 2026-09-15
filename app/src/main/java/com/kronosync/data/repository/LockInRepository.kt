package com.kronosync.data.repository

import com.kronosync.data.db.LockInSession
import com.kronosync.data.db.LockInSessionDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockInRepository @Inject constructor(
    private val dao: LockInSessionDao
) {
    suspend fun start(blockId: Long?, plannedMinutes: Int): Long =
        dao.insert(
            LockInSession(
                blockId = blockId,
                startedAt = System.currentTimeMillis(),
                endedAt = null,
                plannedMinutes = plannedMinutes
            )
        )

    suspend fun recordBreakUsed(sessionId: Long) {
        dao.get(sessionId)?.let { dao.update(it.copy(breaksUsed = it.breaksUsed + 1)) }
    }

    suspend fun finish(sessionId: Long, cancelled: Boolean = false) {
        dao.get(sessionId)?.let { dao.update(it.copy(endedAt = System.currentTimeMillis(), cancelled = cancelled)) }
    }

    suspend fun completedMinutesBetween(start: Long, end: Long): List<LockInSession> =
        dao.getCompletedBetween(start, end)
}
