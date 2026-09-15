package com.kronosync.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.kronosync.data.db.ScheduleBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {

    /** Returns false (and schedules nothing) if exact-alarm permission isn't granted yet. */
    fun scheduleExact(block: ScheduleBlock): Boolean {
        if (!ExactAlarmPermission.canScheduleExactAlarms(context)) return false
        val triggerAt = blockTriggerMillis(block)
        val pi = intentFor(block)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        return true
    }

    fun cancel(block: ScheduleBlock) {
        alarmManager.cancel(intentFor(block))
    }

    private fun intentFor(block: ScheduleBlock): PendingIntent {
        val intent = Intent(context, StartAlertReceiver::class.java).apply {
            action = ACTION_START_ALERT
            putExtra(EXTRA_BLOCK_ID, block.id)
            putExtra(EXTRA_DAY_EPOCH, block.dayEpoch)
            putExtra(EXTRA_START_MINUTE, block.startMinute)
            putExtra(EXTRA_TITLE, block.title)
            putExtra(EXTRA_DURATION_MINUTES, block.durationMinutes)
            putExtra(EXTRA_LOCK_IN, block.lockIn)
        }
        return PendingIntent.getBroadcast(
            context,
            block.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
    }

    private fun mutableFlag(): Int = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0

    private fun blockTriggerMillis(block: ScheduleBlock): Long = block.dayEpoch + block.startMinute * 60_000L

    companion object {
        const val ACTION_START_ALERT = "com.kronosync.action.START_ALERT"
        const val EXTRA_BLOCK_ID = "block_id"
        const val EXTRA_DAY_EPOCH = "day_epoch"
        const val EXTRA_START_MINUTE = "start_minute"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_LOCK_IN = "lock_in"
    }
}
