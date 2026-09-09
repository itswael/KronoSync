package com.kronosync.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kronosync.data.repository.CheckInRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CheckInReceiver : BroadcastReceiver() {

    @Inject lateinit var repo: CheckInRepository

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getStringExtra(EXTRA_STATUS) ?: return
        val blockId = intent.getLongExtra(AlarmScheduler.EXTRA_BLOCK_ID, -1L).takeIf { it > 0 }
        CoroutineScope(Dispatchers.Default).launch {
            repo.log(blockId, status)
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.kronosync.action.CHECK_IN"
        const val EXTRA_STATUS = "status"
    }
}
