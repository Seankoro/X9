package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.repository.GeocodingRepository
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

    /** Resets all location state — call before opening the form for a new report. */
    fun reset() {
        _locationDisplayName.value = ""
        _latitude.value = null
        _longitude.value = null
        _isLoadingLocation.value = false
    }
}
