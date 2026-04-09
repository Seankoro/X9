package dk.itu.moapd.x9.s25134

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database
import dk.itu.moapd.x9.s25134.repository.LocationRepository
import dk.itu.moapd.x9.s25134.repository.LocationRepositoryImpl
import dk.itu.moapd.x9.s25134.repository.ReportRepository

// Application runs once for every lifecycle initialize firebase database connection here
class X9Application : Application() {

    companion object {
        private const val TAG = "X9Application"
    }

    // Singleton repositories shared across ViewModels to avoid duplicate Firebase listeners
    // and redundant FusedLocationProvider clients. Lazy so they are created after onCreate().
    val reportRepository: ReportRepository by lazy { ReportRepository() }
    val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(this) }

    override fun onCreate() {
        super.onCreate()
        // Must be called before any FirebaseDatabase reference is created.
        // Application.onCreate() runs exactly once per process survives state change
        Firebase.database.setPersistenceEnabled(true)
        Log.d(TAG, "Firebase disk persistence enabled")
    }
}
