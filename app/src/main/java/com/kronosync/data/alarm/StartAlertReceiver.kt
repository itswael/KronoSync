package com.kronosync.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kronosync.ui.notifications.Notifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

@AndroidEntryPoint
class StartAlertReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: Notifier
    @Inject lateinit var rescheduler: Rescheduler

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: return
        notifier.showStartNotification(title)

        // Chain next upcoming alarm
        CoroutineScope(Dispatchers.Default).launch {
            rescheduler.onBootCompleted() // reuse logic to schedule the next upcoming based on now
        }
    }
}
