package com.kronosync.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import android.content.Intent
import com.kronosync.data.alarm.CheckInReceiver
import com.kronosync.data.alarm.AlarmScheduler
import androidx.core.app.NotificationManagerCompat
import com.kronosync.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val channelId = "start_alerts"

    init {
        ensureChannel()
    }

    fun showStartNotification(title: String) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("It's time")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(buildAction("Mark done", "Done"))
            .addAction(buildAction("Mark partial", "Partial"))
            .addAction(buildAction("Skip for now", "Skipped"))
        NotificationManagerCompat.from(context).notify(title.hashCode(), builder.build())
    }

    private fun buildAction(label: String, status: String): NotificationCompat.Action {
        val intent = Intent(context, CheckInReceiver::class.java).apply {
            action = CheckInReceiver.ACTION_CHECK_IN
            putExtra(CheckInReceiver.EXTRA_STATUS, status)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            status.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        return NotificationCompat.Action.Builder(0, label, pi).build()
    }

    private fun mutableFlag(): Int = if (android.os.Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Start reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Gentle reminders when a scheduled block begins."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
