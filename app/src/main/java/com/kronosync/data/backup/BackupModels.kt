package com.kronosync.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleBackup(
    val templates: List<TemplateDTO>,
    val blocks: List<ScheduleBlockDTO>
)

@Serializable
data class ProgressBackup(
    val entries: List<DailyLogEntryDTO>
)

@Serializable
data class TemplateDTO(val id: Long, val name: String)

@Serializable
data class ScheduleBlockDTO(
    val id: Long,
    val dayEpoch: Long,
    val startMinute: Int,
    val title: String,
    val tag: String?
)

@Serializable
data class DailyLogEntryDTO(
    val id: Long,
    val blockId: Long?,
    val timestamp: Long,
    val status: String
)