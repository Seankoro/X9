package dk.itu.moapd.x9.s25134.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dk.itu.moapd.x9.s25134.MainActivity
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport

// Utility to get display name for different severity levels, using resource system
private fun Severity.displayName(context: Context): String = context.getString(
    when (this) {
        Severity.MINOR    -> R.string.severity_label_minor
        Severity.LOW      -> R.string.severity_label_low
        Severity.MODERATE -> R.string.severity_label_moderate
        Severity.HIGH     -> R.string.severity_label_high
        Severity.CRITICAL -> R.string.severity_label_critical
    }
)

/**
 * Utility for creating the proximity notification channel and posting individual
 * proximity alert notifications.
 *
 * [createChannel] must be called once before any notification is posted — it is
 * invoked from [dk.itu.moapd.x9.s25134.X9Application.onCreate]. Channel creation
 * is idempotent; calling it again is safe.
 */
object NotificationHelper {

    const val CHANNEL_ID = "proximity_alerts"

    fun createChannel(context: Context) {
        val name = context.getString(R.string.notif_channel_proximity_name)
        val description = context.getString(R.string.notif_channel_proximity_desc)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_HIGH
        ).apply { this.description = description }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts a proximity alert notification for [report]. Each report uses
     * [report.id.hashCode] as its notification ID so concurrent alerts for
     * different reports don't overwrite each other.
     *
     * Notification body prefers [TrafficReport.locationName] when available,
     * falling back to [TrafficReport.description].
     *
     * Tapping the notification opens [MainActivity].
     */
    fun postProximityNotification(context: Context, report: TrafficReport) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            report.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the contents of notification body
        val body = report.locationName.takeIf { it.isNotBlank() } ?: report.description
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_traffic_alert)
            .setContentTitle(context.getString(R.string.notif_proximity_title, report.severity.displayName(context), report.type))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Explicitly checking notifications permissions
        val notifManager = NotificationManagerCompat.from(context)
        if (notifManager.areNotificationsEnabled() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notifManager.notify(report.id.hashCode(), notification)
        }
    }
}
