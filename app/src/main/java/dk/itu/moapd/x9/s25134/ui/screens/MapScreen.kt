@file:OptIn(
    ExperimentalMaterial3Api::class,
    com.google.maps.android.compose.MapsComposeExperimentalApi::class
)

package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.components.SeverityBadge
import dk.itu.moapd.x9.s25134.ui.components.typeEmoji
import kotlin.math.roundToInt

private val DEFAULT_POSITION = LatLng(55.676098, 12.568337) // Copenhagen city center

private class ReportClusterItem(val report: TrafficReport) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(report.latitude ?: 0.0, report.longitude ?: 0.0)
    override fun getTitle(): String = report.type
    override fun getSnippet(): String = report.description.take(60)
    override fun getZIndex(): Float = 0f
}

private fun formatDistance(report: TrafficReport, userLocation: LatLng?, context: Context): String? {
    if (userLocation == null || report.latitude == null || report.longitude == null) return null
    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        userLocation.latitude, userLocation.longitude,
        report.latitude, report.longitude,
        results
    )
    val distanceM = results[0]
    return if (distanceM < 1000)
        context.getString(R.string.distance_meters, distanceM.roundToInt())
    else
        context.getString(R.string.distance_km, distanceM / 1000)
}

@Composable
fun MapScreen(
    reports: List<TrafficReport>,
    userLocation: LatLng?,
    mapType: MapType,
    onMapTypeChange: (MapType) -> Unit,
    onLoadUserLocation: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val defaultZoom = integerResource(R.integer.map_default_zoom).toFloat()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted && !hasPermission) {
                    hasPermission = true
                    onLoadUserLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showRationale by rememberSaveable { mutableStateOf(false) }
    var showLayerPicker by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        hasPermission = granted
        if (granted) onLoadUserLocation() else showRationale = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            onLoadUserLocation()
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    var hasCameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLocation, defaultZoom)
            )
            hasCameraInitialized = true
        } else if (!hasCameraInitialized) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, defaultZoom)
            )
            hasCameraInitialized = true
        }
    }

    var selectedReports by remember { mutableStateOf<List<TrafficReport>>(emptyList()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val clusterItems = remember(reports) {
        reports
            .filter { it.latitude != null && it.longitude != null }
            .map { ReportClusterItem(it) }
    }
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.label_location)) },
            text = { Text(stringResource(R.string.msg_location_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.button_grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasPermission,
                mapType = mapType
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = hasPermission,
                zoomControlsEnabled = true
            )
        ) {
            Clustering(
                items = clusterItems,
                onClusterClick = { cluster ->
                    selectedReports = cluster.items.map { it.report }
                    showBottomSheet = true
                    true
                },
                onClusterItemClick = { item ->
                    // Detect reports stacked at the exact same position (renders as one overlapping pin).
                    // Exact Double equality is correct here: coordinates come from Firebase storage,
                    // so same-location reports carry bit-identical values.
                    val atSamePosition = clusterItems.filter {
                        it.position.latitude == item.position.latitude &&
                        it.position.longitude == item.position.longitude
                    }
                    selectedReports = if (atSamePosition.size > 1) {
                        atSamePosition.map { it.report }
                    } else {
                        listOf(item.report)
                    }
                    showBottomSheet = true
                    true
                },
                clusterContent = { cluster ->
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.map_cluster_marker_size))
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cluster.size.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                clusterItemContent = { _ ->
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(dimensionResource(R.dimen.map_single_marker_size))
                    )
                }
            )
        }

        // Layers button — top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(R.dimen.spacing_small))
        ) {
            if (showLayerPicker) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_small), vertical = dimensionResource(R.dimen.spacing_xs))) {
                        listOf(
                            MapType.NORMAL to stringResource(R.string.map_type_normal),
                            MapType.SATELLITE to stringResource(R.string.map_type_satellite),
                            MapType.HYBRID to stringResource(R.string.map_type_hybrid),
                            MapType.TERRAIN to stringResource(R.string.map_type_terrain)
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = mapType == type,
                                onClick = {
                                    onMapTypeChange(type)
                                    showLayerPicker = false
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_xs))
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    IconButton(onClick = { showLayerPicker = true }) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = stringResource(R.string.cd_layers_button)
                        )
                    }
                }
            }
        }
    }

    // ModalBottomSheet is intentionally outside the Box — it renders in its own window layer
    if (showBottomSheet && selectedReports.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            if (selectedReports.size == 1) {
                SingleReportSheet(
                    report = selectedReports.first(),
                    userLocation = userLocation,
                    onViewDetail = {
                        showBottomSheet = false
                        onNavigateToDetail(selectedReports.first().id)
                    }
                )
            } else {
                SwipeableClusterSheet(
                    reports = selectedReports,
                    userLocation = userLocation,
                    onViewDetail = { id ->
                        showBottomSheet = false
                        onNavigateToDetail(id)
                    }
                )
            }
        }
    }
}

@Composable
private fun SingleReportSheet(
    report: TrafficReport,
    userLocation: LatLng?,
    onViewDetail: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
            .padding(bottom = dimensionResource(R.dimen.spacing_large)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
    ) {
        // Header row: emoji + type name + severity badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
            ) {
                Text(text = typeEmoji(report.type), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = report.type,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            SeverityBadge(severity = report.severity)
        }

        HorizontalDivider()

        // Full description
        Text(
            text = report.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Distance from user, or stored location name as fallback
        val distanceText = formatDistance(report, userLocation, context)
            ?: report.locationName.ifBlank { null }

        if (distanceText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_location_pin))
                )
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onViewDetail,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
        ) {
            Text(stringResource(R.string.btn_view_full_report), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SwipeableClusterSheet(
    reports: List<TrafficReport>,
    userLocation: LatLng?,
    onViewDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { reports.size })

    Column(modifier = Modifier.fillMaxWidth()) {

        // "X / Y" page counter
        Text(
            text = stringResource(
                R.string.map_pager_indicator,
                pagerState.currentPage + 1,
                reports.size
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.spacing_small),
                    bottom = dimensionResource(R.dimen.spacing_xs)
                )
        )

        // Dot indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.spacing_small)),
            horizontalArrangement = Arrangement.Center
        ) {
            reports.indices.forEach { index ->
                key(index) {
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.pager_dot_spacing))
                            .size(dimensionResource(R.dimen.pager_dot_size))
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                }
                            )
                    )
                }
            }
        }

        // Swipeable report cards
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val report = reports[page]
            val distanceText = formatDistance(report, userLocation, context)
                ?: report.locationName.ifBlank { null }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
                    .padding(bottom = dimensionResource(R.dimen.spacing_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                // Header: emoji + type name + severity badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        Text(
                            text = typeEmoji(report.type),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = report.type,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    SeverityBadge(severity = report.severity)
                }

                HorizontalDivider()

                // Description
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Distance / location name
                if (distanceText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_location_pin))
                        )
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Navigate to full detail
                Button(
                    onClick = { onViewDetail(report.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius))
                ) {
                    Text(
                        text = stringResource(R.string.btn_view_full_report),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
