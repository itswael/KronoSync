package com.kronosync.data.repository

import com.kronosync.data.db.DailyLogEntry
import com.kronosync.data.db.DailyLogEntryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val dao: DailyLogEntryDao
) {
    suspend fun log(blockId: Long?, status: String, note: String? = null) {
        dao.insert(
            DailyLogEntry(
                blockId = blockId,
                timestamp = System.currentTimeMillis(),
                status = status,
                note = note
            )
        )
    }

    fun observe(blockId: Long): Flow<List<DailyLogEntry>> = dao.observeForBlock(blockId)

    suspend fun getAllEntries(): List<DailyLogEntry> = dao.getAll()
}
