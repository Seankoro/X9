package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.severityLabel
import java.time.Instant
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    reportId: String? = null,
    reports: List<TrafficReport> = emptyList(),
    currentUser: User? = null,
    locationDisplayName: String,
    locationLat: Double?,
    locationLng: Double?,
    isLoadingLocation: Boolean,
    onRequestLocation: () -> Unit,
    onSubmit: (TrafficReport) -> Unit,
    onBack: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val existingReport = remember(reportId) { reports.find { it.id == reportId } }
    val isEditMode = existingReport != null

    var expanded by remember { mutableStateOf(false) }
    var selectedType by rememberSaveable(reportId) { mutableStateOf(existingReport?.type ?: trafficTypes[0]) }
    var description by rememberSaveable(reportId) { mutableStateOf(existingReport?.description ?: "") }
    var descriptionError by rememberSaveable(reportId) { mutableStateOf<String?>(null) }
    var severityFloat by rememberSaveable(reportId) {
        mutableFloatStateOf(existingReport?.severity?.level?.toFloat() ?: 1f)
    }

    val severity: Severity = Severity.entries[severityFloat.roundToInt() - 1]
    val hasInput = description.isNotBlank()
    val showDiscardDialog = rememberSaveable { mutableStateOf(false) }
    val errorEmptyDescription = stringResource(R.string.error_empty_description)

    // On form open: request location for new reports
    LaunchedEffect(reportId) {
        if (existingReport == null) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) onRequestLocation()
        }
    }

    BackHandler(enabled = hasInput) { showDiscardDialog.value = true }

    if (showDiscardDialog.value) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog.value = false },
            title = { Text(stringResource(R.string.discard_report_title)) },
            text = { Text(stringResource(R.string.discard_report_message)) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog.value = false; onBack() }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog.value = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = if (isEditMode) stringResource(R.string.title_edit_report)
                    else stringResource(R.string.title_new_report),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
                .padding(top = dimensionResource(R.dimen.spacing_medium), bottom = dimensionResource(R.dimen.spacing_large))
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

            // Type dropdown
            Text(
                text = stringResource(R.string.label_type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius)),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    trafficTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = { selectedType = type; expanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (it.trim().isNotEmpty()) descriptionError = null
                },
                label = { Text(stringResource(R.string.label_description)) },
                placeholder = { Text(stringResource(R.string.hint_description)) },
                isError = descriptionError != null,
                supportingText = descriptionError?.let { { Text(it) } },
                shape = RoundedCornerShape(dimensionResource(R.dimen.text_field_corner_radius)),
                modifier = Modifier.fillMaxWidth().height(dimensionResource(R.dimen.text_field_description_height)),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            // Severity slider
            Text(
                text = stringResource(R.string.label_severity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))
            val severityColor = colorResource(
                when (severity) {
                    Severity.MINOR    -> R.color.severity_minor
                    Severity.LOW      -> R.color.severity_low
                    Severity.MODERATE -> R.color.severity_moderate
                    Severity.HIGH     -> R.color.severity_high
                    Severity.CRITICAL -> R.color.severity_critical
                }
            )
            Slider(
                value = severityFloat,
                onValueChange = { severityFloat = it },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = severityColor,
                    activeTrackColor = severityColor
                )
            )
            Text(
                text = stringResource(R.string.severity_level_display, severityLabel(severity), severity.level),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            // Location section — new reports only
            if (!isEditMode) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

                Text(
                    text = stringResource(R.string.label_location),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))

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
                        // No GPS fix obtained
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

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

            // Submit button
            val submitEnabled = isEditMode || (locationLat != null && locationLng != null)
            Button(
                onClick = {
                    val trimmed = description.trim()
                    if (trimmed.isEmpty()) { descriptionError = errorEmptyDescription; return@Button }
                    if (reportId == null) {
                        onSubmit(
                            TrafficReport(
                                type = selectedType,
                                description = trimmed,
                                severity = severity,
                                latitude = locationLat,
                                longitude = locationLng,
                                locationName = locationDisplayName,
                                creatorId = currentUser?.uid ?: ""
                            )
                        )
                    } else {
                        onSubmit(
                            TrafficReport(
                                type = selectedType,
                                description = trimmed,
                                severity = severity,
                                latitude = locationLat,
                                longitude = locationLng,
                                locationName = locationDisplayName,
                                id = reportId,
                                creatorId = existingReport?.creatorId ?: "",
                                createdAt = existingReport?.createdAt ?: Instant.now().toEpochMilli()
                            )
                        )
                    }
                },
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth().height(dimensionResource(R.dimen.button_height_primary)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(stringResource(R.string.button_submit), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
