package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.X9Application
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.repository.GeocodingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportFormViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportFormViewModel"
    }

    private val locationRepository = getApplication<X9Application>().locationRepository
    private val reportRepository = getApplication<X9Application>().reportRepository
    private val storageRepository = getApplication<X9Application>().storageRepository
    private val geocodingRepository = GeocodingRepository()

    private fun formatCoords(lat: Double, lng: Double): String =
        getApplication<Application>().getString(R.string.format_lat_lng, lat, lng)

    // --- Location state ---
    private val _locationDisplayName = MutableStateFlow("")
    val locationDisplayName: StateFlow<String> = _locationDisplayName.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    // --- Image state ---
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // --- Submission state ---
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submissionComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val submissionComplete: SharedFlow<Unit> = _submissionComplete.asSharedFlow()

    private val _submissionError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val submissionError: SharedFlow<String> = _submissionError.asSharedFlow()

    /** Fetches GPS location and reverse geocodes it for display. */
    fun loadCurrentLocation() {
        viewModelScope.launch {
            _isLoadingLocation.value = true
            val latLng: LatLng? = locationRepository.getCurrentLocation()
            if (latLng != null) {
                _latitude.value = latLng.latitude
                _longitude.value = latLng.longitude
                val address = geocodingRepository.reverseGeocode(latLng.latitude, latLng.longitude)
                _locationDisplayName.value = address
                    ?: formatCoords(latLng.latitude, latLng.longitude)
            }
            _isLoadingLocation.value = false
            Log.d(TAG, "Location loaded: lat=${_latitude.value}, lng=${_longitude.value}")
        }
    }

    fun setImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * Pre-populates location state from [report] when entering edit mode.
     * Call after reset() so the ViewModel holds the report's existing coordinates
     * and the form's submit button reads the correct values without branching.
     */
    fun loadFromExistingReport(report: TrafficReport) {
        _latitude.value = report.latitude
        _longitude.value = report.longitude
        _locationDisplayName.value = report.locationName
    }

    /**
     * Uploads the selected image (if any) then updates [report] in the database.
     * Only the report owner (matched by [currentUserId]) may update.
     * If the upload fails, emits [submissionError] but still saves the report
     * with whatever [report.imageUrl] was passed in (existing or null).
     * On success, emits [submissionComplete].
     */
    fun submitUpdatedReport(report: TrafficReport, currentUserId: String?) {
        if (currentUserId == null || report.creatorId != currentUserId) {
            _submissionError.tryEmit(
                getApplication<Application>().getString(R.string.error_not_owner_edit)
            )
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val imageUrl = _selectedImageUri.value?.let { uri ->
                    storageRepository.uploadReportImage(uri, report.creatorId, report.id)
                        .onFailure { e ->
                            Log.e(TAG, "Image upload failed during update, keeping existing: ${e.message}")
                            _submissionError.tryEmit(
                                getApplication<Application>().getString(R.string.error_image_upload_failed)
                            )
                        }
                        .getOrNull()
                }
                val finalReport = if (imageUrl != null) report.copy(imageUrl = imageUrl) else report
                reportRepository.updateReport(finalReport)
                // Intentional: submissionError (for the image warning) fires before submissionComplete
                // (which triggers navigation). The UI's LaunchedEffect collectors on the main dispatcher
                // process them in emission order, so the snackbar appears before the screen pops.
                // This mirrors the same pattern in submitNewReport.
                _submissionComplete.tryEmit(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "submitUpdatedReport failed: ${e.message}", e)
                _submissionError.tryEmit(
                    getApplication<Application>().getString(R.string.error_report_save_failed)
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /**
     * Uploads the selected image (if any) then saves [report] to the database.
     * If the upload fails, emits [submissionError] and saves the report without an image.
     * On success, emits [submissionComplete].
     */
    fun submitNewReport(report: TrafficReport) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val imageUrl = _selectedImageUri.value?.let { uri ->
                    storageRepository.uploadReportImage(uri, report.creatorId, report.id)
                        .onFailure { e ->
                            Log.e(TAG, "Image upload failed, saving without image: ${e.message}")
                            _submissionError.tryEmit(
                                getApplication<Application>().getString(R.string.error_image_upload_failed)
                            )
                        }
                        .getOrNull()
                }
                val finalReport = if (imageUrl != null) report.copy(imageUrl = imageUrl) else report
                reportRepository.addReport(finalReport)
                _submissionComplete.tryEmit(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "submitNewReport failed: ${e.message}", e)
                _submissionError.tryEmit(
                    getApplication<Application>().getString(R.string.error_report_save_failed)
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /** Resets all form state — call before opening the form for a new report. */
    fun reset() {
        _isSubmitting.value = false
        _locationDisplayName.value = ""
        _latitude.value = null
        _longitude.value = null
        _isLoadingLocation.value = false
        _selectedImageUri.value = null
    }
}
