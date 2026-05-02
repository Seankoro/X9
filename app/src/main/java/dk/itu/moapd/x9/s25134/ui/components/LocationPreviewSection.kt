package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import dk.itu.moapd.x9.s25134.R

// Composable to show user's current location on a Map display, used in report creation and report
// details screen.
@Composable
fun LocationPreviewSection(
    isLoadingLocation: Boolean,
    locationLat: Double?,
    locationLng: Double?,
    locationDisplayName: String
) {
    val mapHeight = dimensionResource(R.dimen.map_embed_height)
    val mapZoom = integerResource(R.integer.map_embed_zoom).toFloat()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(locationLat, locationLng) {
        if (locationLat != null && locationLng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(locationLat, locationLng),
                mapZoom
            )
        }
    }

    when {
        isLoadingLocation -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_small))
                )
            }
        }
        locationLat != null && locationLng != null -> {
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                val markerState = remember(locationLat, locationLng) {
                    MarkerState(position = LatLng(locationLat, locationLng))
                }
                Marker(state = markerState)
            }
            if (locationDisplayName.isNotBlank()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))
                Text(
                    text = locationDisplayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.error_location_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
