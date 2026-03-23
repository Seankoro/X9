package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ViewModels are like the Controllers + Services in a typical backend.
// It handles the business logic and data processing in an Android application.
// A screen gets its own ViewModel if the data does not concern any other screen, if not it should
// be scoped to a common parent ViewModel.

// AndroidViewModel because we need Application context to access SharedPreferences.
// Reports are kept in LiveData so the UI re-renders automatically on list changes.
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
        _isDarkMode.value = enabled
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }

    // In-memory list seeded with demo data
    private val _reports = MutableLiveData<List<TrafficReport>>()
    val reports: LiveData<List<TrafficReport>> = _reports

    // Initialize the report list with the dummy reports
    init {
        _reports.value = listOf(
            TrafficReport("Accident", "Multi-car collision blocking two lanes on highway E45 near exit 12", Severity.CRITICAL),
            TrafficReport("Heavy Traffic", "Slow-moving traffic on Lyngbyvejen towards city centre", Severity.MODERATE),
            TrafficReport("Speed Camera", "Mobile speed camera spotted near Nørreport station", Severity.MINOR),
            TrafficReport("Road Work", "Lane closure on Amager Strandvej due to road resurfacing", Severity.HIGH),
            TrafficReport("Heavy Traffic", "Congestion building on Hillerødmotorvejen after morning rush", Severity.LOW),
            TrafficReport("Accident", "Minor fender-bender on Østerbrogade, right lane blocked", Severity.MODERATE),
            TrafficReport("Road Work", "Utility maintenance causing delays on Vesterbrogade", Severity.LOW),
            TrafficReport("Speed Camera", "Fixed speed camera active at Folehaven 60 km/h zone", Severity.MINOR)
        )
    }

    fun addReport(report: TrafficReport) {
        Log.d(TAG, "Report submitted — type: ${report.type}, severity: ${report.severity.level}/5, description: ${report.description}")
        val current = _reports.value.orEmpty()
        _reports.value = (current + report).sortedByDescending { it.timestamp }
    }

    fun deleteReport(id: String) {
        _reports.value = _reports.value.orEmpty().filter { it.id != id }
    }
}
