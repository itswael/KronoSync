package com.kronosync.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kronosync.data.lockin.LockInForegroundService
import com.kronosync.domain.lockin.LockInPermissions
import com.kronosync.ui.notifications.Notifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StartAlertReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: Notifier
    @Inject lateinit var rescheduler: Rescheduler

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: return
        val blockId = intent.getLongExtra(AlarmScheduler.EXTRA_BLOCK_ID, -1L)
        notifier.showStartNotification(blockId, title)

        val lockIn = intent.getBooleanExtra(AlarmScheduler.EXTRA_LOCK_IN, false)
        if (lockIn && LockInPermissions.hasAllPermissions(context)) {
            val durationMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_DURATION_MINUTES, 0)
            if (durationMinutes > 0) {
                LockInForegroundService.startCountdown(context, blockId.takeIf { it > 0 }, title, durationMinutes)
            }
        }

        // Chain next upcoming alarm
        CoroutineScope(Dispatchers.Default).launch {
            rescheduler.onBootCompleted() // reuse logic to schedule the next upcoming based on now
        }
    }
}
