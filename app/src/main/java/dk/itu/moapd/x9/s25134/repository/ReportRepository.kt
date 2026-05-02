package dk.itu.moapd.x9.s25134.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.toMap
import dk.itu.moapd.x9.s25134.model.toTrafficReport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * Manages CRUD operations against the Firebase Realtime Database "reports" node.
 * Maintains a persistent [ValueEventListener] while active so the local [reports]
 * StateFlow stays in sync with remote changes in real time.
 *
 * Rest of application will interact with Firebase Realtime Database through the
 * repository layer.
 *
 * Call [startListening] when the ViewModel is ready and [stopListening] in [onCleared]
 * to avoid memory leaks.
 */
class ReportRepository {

    companion object {
        private const val TAG = "ReportRepository"
        private const val DB_PATH_REPORTS = "reports"
    }

    private val reportsRef = Firebase.database.reference.child(DB_PATH_REPORTS)

    private val _reports = MutableStateFlow<List<TrafficReport>>(emptyList())
    val reports: StateFlow<List<TrafficReport>> = _reports.asStateFlow()

    private val _dbError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val dbError: SharedFlow<String> = _dbError.asSharedFlow()

    private val listener = object : ValueEventListener {
        // EventListener to update the local reports StateFlow when Database changes
        override fun onDataChange(snapshot: DataSnapshot) {
            val list = snapshot.children.mapNotNull { it.toTrafficReport() }
            _reports.value = list.sortedByDescending { it.createdAt }
            Log.d(TAG, "Reports synced: ${list.size} report(s)")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Database read cancelled: ${error.message}", error.toException())
            if (!_dbError.tryEmit(error.message)) {
                Log.w(TAG, "dbError dropped (buffer full): ${error.message}")
            }
        }
    }

    fun startListening() {
        reportsRef.addValueEventListener(listener)
        Log.d(TAG, "Listener attached to $DB_PATH_REPORTS")
    }

    fun stopListening() {
        reportsRef.removeEventListener(listener)
        Log.d(TAG, "Listener detached from $DB_PATH_REPORTS")
    }

    fun addReport(report: TrafficReport) {
        require(report.id.isNotBlank()) { "report.id must not be blank" }
        Log.d(TAG, "Writing report id=${report.id}")
        reportsRef.child(report.id).setValue(report.toMap())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save report id=${report.id}: ${e.message}", e)
                emitError(e.message ?: "Failed to save report")
            }
    }

    fun deleteReport(id: String) {
        Log.d(TAG, "Deleting report id=$id")
        reportsRef.child(id).removeValue()
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete report id=$id: ${e.message}", e)
                emitError(e.message ?: "Failed to delete report")
            }
    }

    fun updateReport(updated: TrafficReport) {
        require(updated.id.isNotBlank()) { "report.id must not be blank" }
        val stamped = updated.copy(updatedAt = Instant.now().toEpochMilli())
        Log.d(TAG, "Updating report id=${stamped.id}")
        reportsRef.child(stamped.id).setValue(stamped.toMap())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update report id=${stamped.id}: ${e.message}", e)
                emitError(e.message ?: "Failed to update report")
            }
    }

    private fun emitError(message: String) {
        if (!_dbError.tryEmit(message)) {
            Log.w(TAG, "dbError dropped (buffer full): $message")
        }
    }
}
