package dk.itu.moapd.x9.s25134.repository

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dk.itu.moapd.x9.s25134.ProximityNotificationReceiver
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport

/**
 * Manages registration and removal of geofences with the system GeofencingClient.
 *
 * Call [sync] whenever the live report list changes (driven by [ReportRepository.reports]).
 * [sync] diffs the incoming list against [registeredIds], adding geofences for newly
 * eligible reports and removing geofences for reports that are no longer eligible.
 *
 * An eligible report must have [Severity.HIGH] or [Severity.CRITICAL] severity and
 * non-null coordinates. The proximity radius is fixed at 500m.
 *
 * Must be called from the main thread — GeofencingClient requires a Looper.
 */
class GeofenceRepository(private val context: Context) {

    companion object {
        private const val TAG = "GeofenceRepository"
        private const val RADIUS_METERS = 500f
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    /** IDs of geofences currently registered with the system. */
    private val registeredIds = mutableSetOf<String>()

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ProximityNotificationReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Diffs [reports] against [registeredIds]. Removes geofences for reports that
     * are no longer eligible and registers geofences for newly eligible reports.
     *
     * Returns silently if [Manifest.permission.ACCESS_FINE_LOCATION] is not yet granted.
     * The caller is responsible for re-invoking after permissions are obtained.
     */
    fun sync(reports: List<TrafficReport>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Skipping geofence sync — fine location not granted yet")
            return
        }
        val eligible = reports.filter {
            it.severity.level >= Severity.HIGH.level &&
            it.latitude != null && it.longitude != null
        }
        val eligibleIds = eligible.map { it.id }.toSet()

        val toRemove = registeredIds - eligibleIds
        val toAdd = eligible.filter { it.id !in registeredIds }

        if (toRemove.isNotEmpty()) {
            registeredIds -= toRemove
            geofencingClient.removeGeofences(toRemove.toList())
                .addOnSuccessListener {
                    Log.d(TAG, "Removed ${toRemove.size} geofence(s)")
                }
                .addOnFailureListener { e ->
                    registeredIds += toRemove
                    Log.e(TAG, "Failed to remove geofences", e)
                }
        }

        if (toAdd.isEmpty()) return

        val geofences = toAdd.map { report ->
            Geofence.Builder()
                .setRequestId(report.id)
                .setCircularRegion(report.latitude!!, report.longitude!!, RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            // INITIAL_TRIGGER_ENTER fires immediately if the user is already inside
            // a geofence when it is registered — desirable for hazard alerts.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    toAdd.forEach { registeredIds.add(it.id) }
                    Log.d(TAG, "Registered ${toAdd.size} geofence(s)")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to register geofences", e)
                }
        } catch (e: SecurityException) {
            // Thrown when ACCESS_FINE_LOCATION is missing. Feature degrades silently.
            Log.e(TAG, "Location permission missing — geofences not registered", e)
        }
    }
}
