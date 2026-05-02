package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.X9Application
import dk.itu.moapd.x9.s25134.model.TrafficReport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// Central data hub for report data: streams the live Firestore list, keeps geofences
// in sync with report changes, and handles ownership-guarded deletion.
class ReportListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportListViewModel"
    }

    // Both repositories are obtained from X9Application rather than constructed here so
    // that the same app-scoped instances are shared across all ViewModels.
    private val repository = getApplication<X9Application>().reportRepository
    private val geofenceRepository = getApplication<X9Application>().geofenceRepository

    // Report list is owned by the repository (Firestore listener lives there).
    // The ViewModel exposes it directly rather than copying into a new StateFlow.
    val reports: StateFlow<List<TrafficReport>> = repository.reports

    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        repository.startListening()
        // Two separate coroutines so a slow or failing collector cannot block the other.
        viewModelScope.launch {
            repository.dbError.collect { emitError(it) }
        }
        viewModelScope.launch {
            repository.reports.collect { updatedReports ->
                geofenceRepository.sync(updatedReports)
            }
        }
        Log.d(TAG, "ViewModel initialised — listening for reports")
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        Log.d(TAG, "ViewModel cleared — listener detached")
    }

    // Called by PermissionOnboardingEffect once location permissions are granted,
    // triggering an immediate geofence registration pass against the current report list.
    fun syncGeofences() {
        geofenceRepository.sync(reports.value)
    }

    fun deleteReport(id: String, currentUserId: String?) {
        val report = reports.value.find { it.id == id }
        if (report == null || report.creatorId != currentUserId) {
            emitError(getApplication<Application>().getString(R.string.error_not_owner_delete))
            return
        }
        repository.deleteReport(id)
    }

    private fun emitError(message: String) {
        val delivered = _error.tryEmit(message)
        if (!delivered) Log.w(TAG, "Error dropped (buffer full): $message")
    }
}
