package dk.itu.moapd.x9.s25134

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Re-registers geofences after device reboot. Android clears all registered geofences
 * on reboot; this receiver restores them from the Firebase local disk cache
 * (enabled in [X9Application.onCreate] via [com.google.firebase.Firebase.database.setPersistenceEnabled]).
 *
 * [goAsync] extends the [BroadcastReceiver] deadline beyond the default ~10s limit.
 * A [CACHE_TIMEOUT_MS] timeout guards against a cold cache (e.g. first launch after
 * fresh install). If the cache is cold, geofences will be registered the next time
 * the user opens the app via the normal [ReportListViewModel] init flow.
 *
 * [GeofenceRepository.sync] must be called on the main thread, so the coroutine
 * uses [Dispatchers.Main].
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val CACHE_TIMEOUT_MS = 5_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Geofences require ACCESS_BACKGROUND_LOCATION which only exists on API 29+.
        // On API 28 the app re-registers geofences on next launch via the normal flow.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val pendingResult = goAsync()
        // SupervisorJob lets the scope survive individual child failures. The scope is
        // bound to pendingResult.finish() — it is always cancelled in the finally block
        // so no coroutines outlive the BroadcastReceiver's async window.
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Main + job)
        val app = context.applicationContext as X9Application

        scope.launch {
            try {
                app.reportRepository.startListening()
                val reports = withTimeoutOrNull(CACHE_TIMEOUT_MS) {
                    app.reportRepository.reports.first { it.isNotEmpty() }
                }
                if (reports != null) {
                    app.geofenceRepository.sync(reports)
                    Log.d(TAG, "Geofences re-registered after boot: ${reports.size} report(s)")
                } else {
                    Log.w(TAG, "Firebase cache empty on boot — geofences will register on next app open")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error re-registering geofences on boot", e)
            } finally {
                app.reportRepository.stopListening()
                job.cancel()
                pendingResult.finish()
            }
        }
    }
}
