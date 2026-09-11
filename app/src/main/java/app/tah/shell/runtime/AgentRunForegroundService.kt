package app.tah.shell.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.tah.shell.MainActivity

/**
 * Lightweight foreground service while at least one agent run is active.
 *
 * Honest limits: this raises process priority and shows an ongoing notification.
 * It does **not** claim immortality — OEM battery killers can still stop the process.
 * Session metadata + Needs-you state survive in prefs either way.
 */
class AgentRunForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                ensureChannel()
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "TAH run active"
                val body = intent?.getStringExtra(EXTRA_BODY)
                    ?: "Foreground while a run is live. OEM killers can still stop agents."
                val notification = buildNotification(title, body)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIF_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    startForeground(NOTIF_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun buildNotification(title: String, body: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL,
            "Active runs",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows while a TAH agent run is in the foreground. Not immortal."
        }
        mgr.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL = "tah.active_run"
        const val NOTIF_ID = 71001
        const val ACTION_START = "app.tah.shell.action.START_RUN_FG"
        const val ACTION_STOP = "app.tah.shell.action.STOP_RUN_FG"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"

        fun start(context: Context, title: String, body: String = defaultBody()) {
            val intent = Intent(context, AgentRunForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, body)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AgentRunForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        private fun defaultBody(): String =
            "Keeping the process warm for an active run. OEM battery settings can still kill TAH — see Settings → About."
    }
}
