package com.kronosync.data.repository

import com.kronosync.data.db.ScheduleBlock
import com.kronosync.data.db.ScheduleBlockDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val dao: ScheduleBlockDao
) {
    fun observeForDay(dayEpoch: Long): Flow<List<ScheduleBlock>> = dao.observeForDay(dayEpoch)

    suspend fun add(block: ScheduleBlock): Long = dao.insert(block)
    suspend fun update(block: ScheduleBlock) = dao.update(block)
    suspend fun delete(block: ScheduleBlock) = dao.delete(block)
}
