package app.tah.shell.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.tah.shell.MainActivity
import app.tah.shell.data.AgentSession
import app.tah.shell.data.PendingPermission

class NeedsYouNotifier(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL,
            "Needs you",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Permission gates. Dismiss never approves."
        }
        mgr.createNotificationChannel(channel)
    }

    fun notifyNeedsYou(session: AgentSession, permission: PendingPermission) {
        ensureChannel()
        val uri = Uri.parse("tah://session/${session.id}?focus=permission")
        val intent = Intent(Intent.ACTION_VIEW, uri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            session.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(session.title.ifBlank { "TAH needs you" })
            .setContentText("${permission.toolName} · ${permission.target}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${permission.toolName} on ${permission.target}\n${permission.summary}\nDismiss does not approve."),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(session.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied — Settings explains this.
        }
    }

    fun cancel(sessionId: String) {
        NotificationManagerCompat.from(context).cancel(sessionId.hashCode())
    }

    companion object {
        const val CHANNEL = "tah.needs_you"
    }
}
