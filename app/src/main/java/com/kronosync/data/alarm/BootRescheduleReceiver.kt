package com.kronosync.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var rescheduler: Rescheduler

    override fun onReceive(context: Context, intent: Intent) {
        rescheduler.onBootCompleted()
    }
}
