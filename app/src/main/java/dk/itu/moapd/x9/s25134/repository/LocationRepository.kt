package dk.itu.moapd.x9.s25134.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await

/** Abstraction over the device GPS so callers don't depend on FusedLocationProviderClient directly. */
interface LocationRepository {
    /** Returns the device's current position, or null if permission is missing or the fix fails. */
    suspend fun getCurrentLocation(): LatLng?
}

class LocationRepositoryImpl(private val context: Context) : LocationRepository {

    companion object {
        private const val TAG = "LocationRepository"
    }

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Explicit check for permissions
    override suspend fun getCurrentLocation(): LatLng? {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Location permission not granted — returning null")
            return null
        }

        // Return the device's current location when called
        return try {
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location", e)
            null
        }
    }
}
