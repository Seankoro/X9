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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.components.severityBgColor
import dk.itu.moapd.x9.s25134.ui.components.severityColor
import dk.itu.moapd.x9.s25134.ui.components.severityLabel
import dk.itu.moapd.x9.s25134.ui.components.typeEmoji
import kotlin.math.roundToInt

private val DEFAULT_POSITION = LatLng(55.676098, 12.568337) // Copenhagen city centre
private const val DEFAULT_ZOOM = 12f

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

@OptIn(ExperimentalMaterial3Api::class)
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

    var showRationale by remember { mutableStateOf(false) }
    var showLayerPicker by remember { mutableStateOf(false) }

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
                CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM)
            )
            hasCameraInitialized = true
        } else if (!hasCameraInitialized) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(DEFAULT_POSITION, DEFAULT_ZOOM)
            )
            hasCameraInitialized = true
        }
    }

    var selectedReports by remember { mutableStateOf<List<TrafficReport>>(emptyList()) }
    var selectedReportInCluster by remember { mutableStateOf<TrafficReport?>(null) }
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
                    selectedReports = listOf(item.report)
                    showBottomSheet = true
                    true
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
            onDismissRequest = {
                showBottomSheet = false
                selectedReportInCluster = null
            },
            sheetState = bottomSheetState
        ) {
            val drillTarget = selectedReportInCluster
            when {
                selectedReports.size == 1 -> SingleReportSheet(
                    report = selectedReports.first(),
                    userLocation = userLocation,
                    onBack = null,
                    onViewDetail = {
                        showBottomSheet = false
                        onNavigateToDetail(selectedReports.first().id)
                    }
                )
                drillTarget != null -> SingleReportSheet(
                    report = drillTarget,
                    userLocation = userLocation,
                    onBack = { selectedReportInCluster = null },
                    onViewDetail = {
                        showBottomSheet = false
                        selectedReportInCluster = null
                        onNavigateToDetail(drillTarget.id)
                    }
                )
                else -> ClusterSheet(
                    reports = selectedReports,
                    onReportClick = { report -> selectedReportInCluster = report }
                )
            }
        }
    }
}

@Composable
private fun SingleReportSheet(
    report: TrafficReport,
    userLocation: LatLng?,
    onBack: (() -> Unit)?,
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
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs))
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Text(
                    text = stringResource(R.string.nav_reports),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    modifier = Modifier.size(16.dp)
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
private fun ClusterSheet(
    reports: List<TrafficReport>,
    onReportClick: (TrafficReport) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacing_large))
    ) {
        Text(
            text = stringResource(R.string.cluster_reports_title, reports.size),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.screen_horizontal_padding),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
        )
        HorizontalDivider()
        LazyColumn {
            items(reports) { report ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReportClick(report) }
                        .padding(
                            horizontal = dimensionResource(R.dimen.screen_horizontal_padding),
                            vertical = dimensionResource(R.dimen.item_spacing)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
                ) {
                    Text(text = typeEmoji(report.type), style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.type,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SeverityBadge(severity = report.severity)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
                )
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.badge_corner_radius)))
            .background(severityBgColor(severity))
            .padding(
                horizontal = dimensionResource(R.dimen.card_severity_chip_padding_horizontal),
                vertical = dimensionResource(R.dimen.card_severity_chip_padding_vertical)
            )
    ) {
        Text(
            text = severityLabel(severity),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = severityColor(severity),
            letterSpacing = 0.5.sp
        )
    }
}
