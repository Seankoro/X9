package dk.itu.moapd.x9.s25134

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database
import dk.itu.moapd.x9.s25134.repository.GeocodingRepository
import dk.itu.moapd.x9.s25134.repository.LocationRepository
import dk.itu.moapd.x9.s25134.repository.LocationRepositoryImpl
import dk.itu.moapd.x9.s25134.repository.ReportRepository
import dk.itu.moapd.x9.s25134.repository.StorageRepository

/**
 * Application entry point. Holds singleton repository instances so that ViewModels
 * can retrieve them via [getApplication] without a DI framework. Each repository is
 * created lazily on first access and shared for the lifetime of the process.
 */
class X9Application : Application() {

    companion object {
        private const val TAG = "X9Application"
    }

    val reportRepository: ReportRepository by lazy { ReportRepository() }
    val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(this) }
    val storageRepository: StorageRepository by lazy { StorageRepository(this) }
    val geocodingRepository: GeocodingRepository by lazy { GeocodingRepository() }

    override fun onCreate() {
        super.onCreate()
        Firebase.database.setPersistenceEnabled(true)
        Log.d(TAG, "Firebase disk persistence enabled")
    }
}
