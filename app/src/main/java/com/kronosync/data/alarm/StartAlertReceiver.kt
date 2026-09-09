package com.kronosync.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kronosync.ui.notifications.Notifier
import dagger.hil t.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StartAlertReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: Notifier

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: return
        notifier.showStartNotification(title)
        // Chain next alarm is handled by a coordinator (future work per Step 4)
    }
}
