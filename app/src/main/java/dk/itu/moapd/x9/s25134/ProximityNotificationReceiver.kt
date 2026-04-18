package dk.itu.moapd.x9.s25134

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dk.itu.moapd.x9.s25134.notification.NotificationHelper
import androidx.core.content.edit

/**
 * Receives geofence ENTER events from the system [GeofencingClient] and posts a
 * proximity notification for each triggered report, subject to a 1-hour per-report
 * cooldown.
 *
 * The cooldown timestamp for each report is stored in [PREFS_NAME] SharedPreferences
 * keyed by report ID. This ensures repeated drives past the same hazard within one hour
 * do not produce duplicate alerts, while a return visit after the cooldown period will
 * trigger a fresh notification.
 */
class ProximityNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ProximityNotifReceiver"
        private const val PREFS_NAME = "proximity_cooldown"
        private const val COOLDOWN_MS = 60 * 60 * 1_000L // 1 hour
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "Null GeofencingEvent — ignoring")
            return
        }
        if (event.hasError()) {
            Log.e(TAG, "Geofencing error code: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val reports = (context.applicationContext as X9Application).reportRepository.reports.value
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // Resolve and filter triggering reports before sorting, so the sort only
        // operates on reports that will actually produce a notification.
        val triggeredReports = event.triggeringGeofences
            ?.mapNotNull { geofence ->
                reports.find { it.id == geofence.requestId }.also {
                    if (it == null) Log.w(TAG, "No report found for geofence id=${geofence.requestId}")
                }
            }
            ?.filter { report ->
                val elapsed = now - prefs.getLong(report.id, 0L)
                val ready = elapsed >= COOLDOWN_MS
                if (!ready) Log.d(TAG, "Cooldown active for report ${report.id} — skipping")
                ready
            }
            // Sort ascending so the highest-severity report is posted last and
            // therefore appears at the top of the notification shade (newest-first).
            ?.sortedBy { it.severity.level }
            ?: return

        triggeredReports.forEach { report ->
            NotificationHelper.postProximityNotification(context, report)
            prefs.edit { putLong(report.id, now) }
            Log.d(TAG, "Proximity notification posted for report ${report.id}")
        }
    }
}
