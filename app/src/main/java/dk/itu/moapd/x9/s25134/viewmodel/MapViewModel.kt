package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.repository.LocationRepositoryImpl
import dk.itu.moapd.x9.s25134.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MapViewModel"
    }

    private val reportRepository = ReportRepository()
    private val locationRepository = LocationRepositoryImpl(application)

    val reports: LiveData<List<TrafficReport>> = reportRepository.reports

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType: StateFlow<MapType> = _mapType.asStateFlow()

    init {
        reportRepository.startListening()
        Log.d(TAG, "MapViewModel initialised — listening for reports")
    }

    override fun onCleared() {
        super.onCleared()
        reportRepository.stopListening()
        Log.d(TAG, "MapViewModel cleared — listener detached")
    }

    fun loadUserLocation() {
        viewModelScope.launch {
            _userLocation.value = locationRepository.getCurrentLocation()
            Log.d(TAG, "User location: ${_userLocation.value}")
        }
    }

    fun setMapType(type: MapType) {
        _mapType.value = type
    }
}
