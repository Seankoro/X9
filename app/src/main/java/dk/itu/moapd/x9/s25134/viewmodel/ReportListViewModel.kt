package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.X9Application
import dk.itu.moapd.x9.s25134.model.TrafficReport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// AndroidViewModel because we need Application context for SharedPreferences and string resources.
class ReportListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportListViewModel"
        const val PREFS_NAME = "x9_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // null = no user preference saved yet; MainActivity will fall back to isSystemInDarkTheme().
    // Boolean = user has explicitly toggled; their choice overrides the system setting.
    private val _isDarkMode: MutableStateFlow<Boolean?> = MutableStateFlow(
        if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        Log.d(TAG, "Dark mode set to $enabled")
        _isDarkMode.value = enabled
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    private val repository = getApplication<X9Application>().reportRepository

    val reports: LiveData<List<TrafficReport>> = repository.reports

    // Unified error channel: merges database errors from the repository with
    // authorization rule violations enforced here in the ViewModel.
    private val _error = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        repository.startListening()
        // Forward database errors through the ViewModel's unified error channel.
        viewModelScope.launch {
            repository.dbError.collect { emitError(it) }
        }
        Log.d(TAG, "ViewModel initialised — listening for reports")
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        Log.d(TAG, "ViewModel cleared — listener detached")
    }

    fun addReport(report: TrafficReport, currentUserId: String?) {
        // Only authenticated users will be able to create new reports
        if (currentUserId == null) {
            emitError(getApplication<Application>().getString(R.string.msg_sign_in_required))
            return
        }
        repository.addReport(report)
    }

    fun deleteReport(id: String, currentUserId: String?) {
        // Report must be a valid report in DB and only creators can delete their own report
        val report = reports.value?.find { it.id == id }
        // Block if the report is not in local cache or the caller is not the creator.
        if (report == null || report.creatorId != currentUserId) {
            emitError(getApplication<Application>().getString(R.string.error_not_owner_delete))
            return
        }
        repository.deleteReport(id)
    }

    fun updateReport(report: TrafficReport, currentUserId: String?) {
        // Report must be a valid report in DB and only creators of the report can edit it
        if (report.creatorId != currentUserId) {
            emitError(getApplication<Application>().getString(R.string.error_not_owner_edit))
            return
        }
        repository.updateReport(report)
    }

    private fun emitError(message: String) {
        val delivered = _error.tryEmit(message)
        if (!delivered) Log.w(TAG, "Error dropped (buffer full): $message")
    }
}
