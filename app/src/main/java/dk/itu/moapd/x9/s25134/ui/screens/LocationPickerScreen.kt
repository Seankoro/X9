package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.repository.GeocodingResult
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private val DEFAULT_POSITION = LatLng(55.6761, 12.5683) // Copenhagen
private const val DEFAULT_ZOOM = 14f

@Composable
fun LocationPickerScreen(
    initialLat: Double?,
    initialLng: Double?,
    userLocation: LatLng?,
    centerLabel: String,
    geocodingResults: List<GeocodingResult>,
    isSearching: Boolean,
    onLoadUserLocation: () -> Unit,
    onReverseGeocodeCenter: (Double, Double) -> Unit,
    onSearch: (String) -> Unit,
    onClearResults: () -> Unit,
    onConfirm: (displayName: String, lat: Double, lng: Double) -> Unit,
    onBack: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current

    val initialPosition = when {
        initialLat != null && initialLng != null -> LatLng(initialLat, initialLng)
        userLocation != null -> userLocation
        else -> DEFAULT_POSITION
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, DEFAULT_ZOOM)
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) onLoadUserLocation()
    }

    // Reverse geocode the initial map center on first load
    LaunchedEffect(Unit) {
        val center = cameraPositionState.position.target
        onReverseGeocodeCenter(center.latitude, center.longitude)
    }

    // Reverse geocode whenever camera comes to rest (after panning)
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving }
            .distinctUntilChanged()
            .filter { isMoving -> !isMoving }
            .collect {
                showResults = false
                val center = cameraPositionState.position.target
                onReverseGeocodeCenter(center.latitude, center.longitude)
            }
    }

    // Animate camera to user location when it becomes available
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false
            )
        )

        // Fixed center pin
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = stringResource(R.string.cd_center_pin),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(dimensionResource(R.dimen.location_pin_size))
                .align(Alignment.Center)
                // Offset upward by half its height so the pin tip points at the center
                .padding(bottom = dimensionResource(R.dimen.location_pin_offset))
        )

        // Top overlay: back button + search bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = dimensionResource(R.dimen.spacing_small))
        ) {
            // Back button row
            IconButton(
                onClick = { onClearResults(); onBack() },
                modifier = Modifier
                    .padding(start = dimensionResource(R.dimen.spacing_small))
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius))
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.hint_search_location)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                onSearch(searchQuery)
                                showResults = true
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.button_search),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            onSearch(searchQuery)
                            showResults = true
                        }
                    }),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Search results dropdown
            if (showResults && (isSearching || geocodingResults.isNotEmpty())) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
                        .padding(top = dimensionResource(R.dimen.spacing_xs)),
                    shape = RoundedCornerShape(
                        bottomStart = dimensionResource(R.dimen.card_corner_radius),
                        bottomEnd = dimensionResource(R.dimen.card_corner_radius)
                    ),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.spacing_medium)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_small)))
                        }
                    } else {
                        LazyColumn {
                            items(geocodingResults) { result ->
                                TextButton(
                                    onClick = {
                                        val lat = result.lat.toDoubleOrNull() ?: return@TextButton
                                        val lng = result.lon.toDoubleOrNull() ?: return@TextButton
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), DEFAULT_ZOOM)
                                        )
                                        onReverseGeocodeCenter(lat, lng)
                                        onClearResults()
                                        showResults = false
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }

        // Bottom overlay: address label + action buttons
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.screen_horizontal_padding),
                        vertical = dimensionResource(R.dimen.spacing_medium)
                    )
            ) {
                // Current center address
                if (centerLabel.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = centerLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
                    )
                } else {
                    Text(
                        text = stringResource(R.string.msg_determining_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_small))
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

                // Use My Location button
                OutlinedButton(
                    onClick = {
                        if (hasLocationPermission) {
                            onLoadUserLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.cd_my_location_button),
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_size_inline))
                            .padding(end = dimensionResource(R.dimen.spacing_xs))
                    )
                    Text(stringResource(R.string.btn_use_my_location))
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

                // Confirm button
                val coordFormat = stringResource(R.string.format_lat_lng)
                Button(
                    onClick = {
                        val center = cameraPositionState.position.target
                        val label = centerLabel.ifBlank {
                            String.format(coordFormat, center.latitude, center.longitude)
                        }
                        onConfirm(label, center.latitude, center.longitude)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true
                ) {
                    Text(
                        text = stringResource(R.string.btn_confirm_location),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
