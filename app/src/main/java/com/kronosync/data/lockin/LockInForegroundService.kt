package com.kronosync.data.lockin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kronosync.MainActivity
import com.kronosync.R
import com.kronosync.data.repository.LockInRepository
import com.kronosync.domain.lockin.AllowlistResolver
import com.kronosync.domain.lockin.BreakPolicy
import com.kronosync.domain.lockin.LockInOverlayController
import com.kronosync.domain.lockin.LockInPhase
import com.kronosync.domain.lockin.LockInStateHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LockInForegroundService : Service() {

    @Inject lateinit var stateHolder: LockInStateHolder
    @Inject lateinit var lockInRepo: LockInRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null
    private var sessionJob: Job? = null
    private lateinit var overlay: LockInOverlayController

    // Tracks active (non-break) minutes across possibly multiple stretches separated by breaks.
    private var accumulatedActiveMinutesBeforeStretch = 0
    private var activeStretchStartMillis = 0L

    override fun onCreate() {
        super.onCreate()
        overlay = LockInOverlayController(applicationContext)
        ensureChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_COUNTDOWN -> {
                val blockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L).takeIf { it > 0 }
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
                val plannedMinutes = intent.getIntExtra(EXTRA_PLANNED_MINUTES, 0)
                startCountdown(blockId, taskTitle, plannedMinutes)
            }
            ACTION_CANCEL_COUNTDOWN -> cancelCountdown()
            ACTION_START_BREAK -> startBreak()
            ACTION_STOP -> endSession(cancelled = true)
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(blockId: Long?, taskTitle: String?, plannedMinutes: Int) {
        stateHolder.update {
            it.copy(
                phase = LockInPhase.COUNTDOWN,
                blockId = blockId,
                taskTitle = taskTitle,
                plannedMinutes = plannedMinutes,
                countdownSecondsRemaining = COUNTDOWN_SECONDS
            )
        }
        startForeground(NOTIFICATION_ID, buildCountdownNotification(COUNTDOWN_SECONDS))

        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            for (remaining in COUNTDOWN_SECONDS - 1 downTo 0) {
                delay(1000)
                stateHolder.update { it.copy(countdownSecondsRemaining = remaining) }
                notificationManager().notify(NOTIFICATION_ID, buildCountdownNotification(remaining))
            }
            beginActiveSession(blockId, taskTitle, plannedMinutes)
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        stateHolder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun beginActiveSession(blockId: Long?, taskTitle: String?, plannedMinutes: Int) {
        accumulatedActiveMinutesBeforeStretch = 0
        activeStretchStartMillis = System.currentTimeMillis()

        serviceScope.launch {
            val sessionId = lockInRepo.start(blockId, plannedMinutes)
            stateHolder.update {
                it.copy(
                    phase = LockInPhase.ACTIVE,
                    sessionId = sessionId,
                    startedAtMillis = System.currentTimeMillis(),
                    breaksUsed = 0
                )
            }
            notificationManager().notify(NOTIFICATION_ID, buildActiveNotification(plannedMinutes, plannedMinutes))
            runMonitorLoop(plannedMinutes)
        }
    }

    private fun startBreak() {
        val state = stateHolder.state.value
        if (state.phase != LockInPhase.ACTIVE) return
        val elapsedThisStretch = ((System.currentTimeMillis() - activeStretchStartMillis) / 60_000L).toInt()
        accumulatedActiveMinutesBeforeStretch += elapsedThisStretch
        val breakEndsAt = System.currentTimeMillis() + BreakPolicy.BREAK_LENGTH_MINUTES * 60_000L
        stateHolder.update {
            it.copy(
                phase = LockInPhase.ON_BREAK,
                breakEndsAtMillis = breakEndsAt,
                activeElapsedMinutesAtBreakStart = accumulatedActiveMinutesBeforeStretch
            )
        }
        overlay.hide()
        serviceScope.launch { stateHolder.state.value.sessionId?.let { lockInRepo.recordBreakUsed(it) } }
        notificationManager().notify(NOTIFICATION_ID, buildBreakNotification())
    }

    private fun resumeFromBreak() {
        activeStretchStartMillis = System.currentTimeMillis()
        stateHolder.update { it.copy(phase = LockInPhase.ACTIVE, breaksUsed = it.breaksUsed + 1) }
    }

    private fun currentActiveElapsedMinutes(): Int {
        val state = stateHolder.state.value
        return if (state.phase == LockInPhase.ON_BREAK) {
            accumulatedActiveMinutesBeforeStretch
        } else {
            accumulatedActiveMinutesBeforeStretch + ((System.currentTimeMillis() - activeStretchStartMillis) / 60_000L).toInt()
        }
    }

    private fun runMonitorLoop(plannedMinutes: Int) {
        sessionJob?.cancel()
        sessionJob = serviceScope.launch {
            val allowed = AllowlistResolver.allowedPackages(applicationContext)
            while (true) {
                delay(POLL_INTERVAL_MS)
                val state = stateHolder.state.value
                if (state.phase == LockInPhase.ON_BREAK) {
                    if (System.currentTimeMillis() >= state.breakEndsAtMillis) {
                        resumeFromBreak()
                        notificationManager().notify(NOTIFICATION_ID, buildActiveNotification(plannedMinutes, plannedMinutes - currentActiveElapsedMinutes()))
                    }
                    continue
                }
                if (state.phase != LockInPhase.ACTIVE) continue

                val activeElapsed = currentActiveElapsedMinutes()
                if (activeElapsed >= plannedMinutes) {
                    endSession(cancelled = false)
                    return@launch
                }

                stateHolder.update {
                    it.copy(breakAvailable = BreakPolicy.isBreakAvailable(plannedMinutes, activeElapsed, it.breaksUsed))
                }

                val foregroundPackage = currentForegroundPackage()
                if (foregroundPackage != null && foregroundPackage !in allowed) {
                    overlay.show(state.taskTitle) {
                        overlay.hide()
                        val launch = Intent(applicationContext, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        applicationContext.startActivity(launch)
                    }
                } else {
                    overlay.hide()
                }

                notificationManager().notify(NOTIFICATION_ID, buildActiveNotification(plannedMinutes, plannedMinutes - activeElapsed))
            }
        }
    }

    private fun currentForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 15_000
        val events = usm.queryEvents(begin, end)
        var lastPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isForeground) lastPackage = event.packageName
        }
        return lastPackage
    }

    private fun endSession(cancelled: Boolean) {
        sessionJob?.cancel()
        countdownJob?.cancel()
        overlay.hide()
        val sessionId = stateHolder.state.value.sessionId
        serviceScope.launch { sessionId?.let { lockInRepo.finish(it, cancelled) } }
        stateHolder.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionJob?.cancel()
        countdownJob?.cancel()
        overlay.hide()
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Lock-In Mode", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Shows countdown and status while Lock-In Mode is active."
            }
            notificationManager().createNotificationChannel(channel)
        }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun serviceActionIntent(action: String): PendingIntent {
        val intent = Intent(this, LockInForegroundService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun buildCountdownNotification(secondsRemaining: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Locking in in ${secondsRemaining}s")
            .setContentText("Tap Cancel if this wasn't intentional.")
            .setOngoing(true)
            .addAction(0, "Cancel", serviceActionIntent(ACTION_CANCEL_COUNTDOWN))
            .setContentIntent(openAppIntent())
            .build()

    private fun buildActiveNotification(plannedMinutes: Int, minutesLeft: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Locked in")
            .setContentText("$minutesLeft of $plannedMinutes min left")
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun buildBreakNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("On a break")
            .setContentText("Lock-in resumes automatically in ${BreakPolicy.BREAK_LENGTH_MINUTES} min.")
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .build()

    companion object {
        const val ACTION_START_COUNTDOWN = "com.kronosync.lockin.action.START_COUNTDOWN"
        const val ACTION_CANCEL_COUNTDOWN = "com.kronosync.lockin.action.CANCEL_COUNTDOWN"
        const val ACTION_START_BREAK = "com.kronosync.lockin.action.START_BREAK"
        const val ACTION_STOP = "com.kronosync.lockin.action.STOP"
        const val EXTRA_BLOCK_ID = "block_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_PLANNED_MINUTES = "planned_minutes"

        private const val CHANNEL_ID = "lock_in"
        private const val NOTIFICATION_ID = 9001
        private const val COUNTDOWN_SECONDS = 10
        private const val POLL_INTERVAL_MS = 1500L

        fun startCountdown(context: Context, blockId: Long?, taskTitle: String?, plannedMinutes: Int) {
            val intent = Intent(context, LockInForegroundService::class.java).apply {
                action = ACTION_START_COUNTDOWN
                blockId?.let { putExtra(EXTRA_BLOCK_ID, it) }
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_PLANNED_MINUTES, plannedMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelCountdown(context: Context) {
            context.startService(Intent(context, LockInForegroundService::class.java).setAction(ACTION_CANCEL_COUNTDOWN))
        }

        fun requestBreak(context: Context) {
            context.startService(Intent(context, LockInForegroundService::class.java).setAction(ACTION_START_BREAK))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LockInForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}
