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

class ReportListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportListViewModel"
    }

    private val repository = getApplication<X9Application>().reportRepository

    val reports: StateFlow<List<TrafficReport>> = repository.reports

    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        repository.startListening()
        viewModelScope.launch {
            repository.dbError.collect { emitError(it) }
        }
        viewModelScope.launch {
            repository.reports.collect { updatedReports ->
                getApplication<X9Application>().geofenceRepository.sync(updatedReports)
            }
        }
        Log.d(TAG, "ViewModel initialised — listening for reports")
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        Log.d(TAG, "ViewModel cleared — listener detached")
    }

    fun syncGeofences() {
        getApplication<X9Application>().geofenceRepository.sync(reports.value)
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
