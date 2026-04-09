package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapType
import dk.itu.moapd.x9.s25134.X9Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MapViewModel"
    }

    private val locationRepository = getApplication<X9Application>().locationRepository

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType: StateFlow<MapType> = _mapType.asStateFlow()

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
