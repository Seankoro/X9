package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.repository.GeocodingRepository
import dk.itu.moapd.x9.s25134.repository.GeocodingResult
import dk.itu.moapd.x9.s25134.repository.LocationRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportFormViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportFormViewModel"
    }

    private val locationRepository = LocationRepositoryImpl(application)
    private val geocodingRepository = GeocodingRepository()

    private fun formatCoords(lat: Double, lng: Double): String =
        getApplication<Application>().getString(R.string.format_lat_lng, lat, lng)

    private val _locationDisplayName = MutableStateFlow("")
    val locationDisplayName: StateFlow<String> = _locationDisplayName.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _isLoadingLocation = MutableStateFlow(false)
    val isLoadingLocation: StateFlow<Boolean> = _isLoadingLocation.asStateFlow()

    private val _geocodingResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val geocodingResults: StateFlow<List<GeocodingResult>> = _geocodingResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    /** Live reverse-geocoded label for the map center pin in LocationPickerScreen. */
    private val _centerLabel = MutableStateFlow("")
    val centerLabel: StateFlow<String> = _centerLabel.asStateFlow()

    /** Called when opening the form for a new report — fetches GPS location and reverse geocodes it. */
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

    /** Called when opening the form in edit mode — reverse geocodes the existing coordinates. */
    fun initializeForExistingReport(lat: Double?, lng: Double?) {
        if (lat == null || lng == null) return
        _latitude.value = lat
        _longitude.value = lng
        viewModelScope.launch {
            val address = geocodingRepository.reverseGeocode(lat, lng)
            _locationDisplayName.value = address ?: formatCoords(lat, lng)
        }
    }

    /** Searches for locations matching [query] using forward geocoding. */
    fun searchLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearching.value = true
            _geocodingResults.value = geocodingRepository.forwardGeocode(query)
            _isSearching.value = false
        }
    }

    /** Applies a user-selected geocoding result as the report's location. */
    fun selectLocation(result: GeocodingResult) {
        _latitude.value = result.lat.toDoubleOrNull()
        _longitude.value = result.lon.toDoubleOrNull()
        _locationDisplayName.value = result.displayName
        _geocodingResults.value = emptyList()
    }

    fun clearGeocodingResults() {
        _geocodingResults.value = emptyList()
    }

    /** Saves the location confirmed in LocationPickerScreen to the report. */
    fun confirmPickedLocation(displayName: String, lat: Double, lng: Double) {
        _locationDisplayName.value = displayName
        _latitude.value = lat
        _longitude.value = lng
        _centerLabel.value = ""
    }

    /** Reverse geocodes [lat]/[lng] and updates [centerLabel] for the picker's center pin. */
    fun reverseGeocodeCenter(lat: Double, lng: Double) {
        viewModelScope.launch {
            _centerLabel.value = geocodingRepository.reverseGeocode(lat, lng)
                ?: formatCoords(lat, lng)
        }
    }

    /** Resets all location state — call before opening the form for a new report. */
    fun reset() {
        _locationDisplayName.value = ""
        _latitude.value = null
        _longitude.value = null
        _geocodingResults.value = emptyList()
        _isLoadingLocation.value = false
        _isSearching.value = false
        _centerLabel.value = ""
    }
}
