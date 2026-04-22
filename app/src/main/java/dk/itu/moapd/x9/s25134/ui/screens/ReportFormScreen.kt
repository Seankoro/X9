package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
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
import java.io.File
import java.time.Instant
import kotlin.math.roundToInt

private fun createCameraOutputUri(context: android.content.Context): Pair<File, Uri> {
    // File.createTempFile is an IO operation. It is fast enough in practice
    // not to cause an ANR (no data is written here), but it does trigger
    // StrictMode in debug builds. For this exercise scope this is acceptable.
    val dir = File(context.cacheDir, "camera_images").also { it.mkdirs() }
    val file = File.createTempFile("report_photo_", ".jpg", dir)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return file to uri
}

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
    selectedImageUri: Uri? = null,
    isSubmitting: Boolean = false,
    preFillType: String? = null,
    preFillSeverity: Severity? = null,
    onPreFillConsumed: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    speechAvailable: Boolean = true,
    onRequestLocation: () -> Unit,
    onImageSelected: (Uri?) -> Unit = {},
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
    var severityFloat by rememberSaveable(reportId) {
        mutableFloatStateOf(existingReport?.severity?.level?.toFloat() ?: 1f)
    }

    val severity: Severity = Severity.entries[severityFloat.roundToInt() - 1]
    val hasInput = description.isNotBlank() || selectedImageUri != null

    LaunchedEffect(preFillType, preFillSeverity) {
        if (preFillType != null) selectedType = preFillType
        if (preFillSeverity != null) severityFloat = preFillSeverity.level.toFloat()
        if (preFillType != null || preFillSeverity != null) onPreFillConsumed()
    }

    val showDiscardDialog = rememberSaveable { mutableStateOf(false) }
    // Tracks whether the user deliberately removed the photo in edit mode.
    // Distinguishes "no photo selected" (keep existing imageUrl) from
    // "user clicked Remove" (set imageUrl to null on submit).
    var photoExplicitlyRemoved by rememberSaveable(reportId) { mutableStateOf(false) }

    // rememberSaveable is required here (not plain remember) because TakePicture opens
    // an external camera Activity. Android may recreate the calling Activity while the
    // camera app is in the foreground. Uri implements Parcelable so rememberSaveable
    // handles it automatically.
    val cameraOutputUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    // Plain remember (not rememberSaveable) is sufficient here: if the process is killed
    // while the camera is open, the temp file persists on disk and cannot be cleaned up
    // until the next launch anyway. We only need this reference to delete on cancel.
    val cameraOutputFile = remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoExplicitlyRemoved = false
            cameraOutputUri.value?.let { onImageSelected(it) }
        } else {
            // User canceled — delete the orphaned temp file to free space.
            cameraOutputFile.value?.delete()
            cameraOutputFile.value = null
            cameraOutputUri.value = null
        }
    }

    // cameraLauncher must be declared above this block because the permission callback
    // references it via closure.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val (file, uri) = createCameraOutputUri(context)
                cameraOutputFile.value = file
                cameraOutputUri.value = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                android.util.Log.e("ReportFormScreen", "Failed to create temp file: ${e.message}")
            }
        }
    }

    // PickVisualMedia uses the Android Photo Picker on API 33+ and falls back to
    // ACTION_GET_CONTENT on older devices. No CAMERA or READ_MEDIA_IMAGES permission needed.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoExplicitlyRemoved = false
            onImageSelected(uri)
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (speechAvailable) 88.dp else 0.dp)
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
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.label_description)) },
                placeholder = { Text(stringResource(R.string.hint_description)) },
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

            // Photo section — available for both new reports and edits
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            Text(
                text = stringResource(R.string.label_photo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                OutlinedButton(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            try {
                                val (file, uri) = createCameraOutputUri(context)
                                cameraOutputFile.value = file
                                cameraOutputUri.value = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                android.util.Log.e("ReportFormScreen", "Camera launch failed: ${e.message}")
                            }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xs)))
                    Text(stringResource(R.string.button_camera))
                }

                OutlinedButton(
                    onClick = {
                        // PickVisualMediaRequest wraps the media type filter.
                        // Passing ImageOnly directly (without the wrapper) is a compile error.
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small))
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_xs)))
                    Text(stringResource(R.string.button_gallery))
                }
            }

            // Preview logic:
            // 1. A newly picked local URI takes priority.
            // 2. If no new URI and the user has not explicitly removed the photo,
            //    show the existing remote URL (edit mode only).
            // 3. Otherwise, no preview.
            val showingNewImage = selectedImageUri != null
            val showingExistingImage = !showingNewImage && !photoExplicitlyRemoved && existingReport?.imageUrl != null

            if (showingNewImage || showingExistingImage) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
                AsyncImage(
                    model = if (showingNewImage) selectedImageUri else existingReport?.imageUrl,
                    contentDescription = stringResource(R.string.cd_report_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.map_embed_height))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                )
                TextButton(onClick = {
                    photoExplicitlyRemoved = true
                    // Also clear the camera temp file reference so it doesn't leak if the
                    // user had taken a photo and then removed it without cancelling the camera.
                    cameraOutputFile.value?.delete()
                    cameraOutputFile.value = null
                    cameraOutputUri.value = null
                    onImageSelected(null)
                }) {
                    Text(stringResource(R.string.button_remove_photo))
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_large)))

            // Submit button
            val submitEnabled = isEditMode || (locationLat != null && locationLng != null)
            Button(
                onClick = {
                    val trimmed = description.trim()
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
                                createdAt = existingReport?.createdAt ?: Instant.now().toEpochMilli(),
                                // Determine the imageUrl to pass to the ViewModel:
                                // - new URI selected → null here; ViewModel uploads and sets the URL
                                // - user explicitly removed → null (clears the image)
                                // - no change → preserve the existing URL
                                imageUrl = when {
                                    selectedImageUri != null -> null
                                    photoExplicitlyRemoved -> null
                                    else -> existingReport?.imageUrl
                                },
                            )
                        )
                    }
                },
                enabled = submitEnabled && !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(dimensionResource(R.dimen.button_height_primary)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_small)),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.button_submit), fontWeight = FontWeight.SemiBold)
                }
            }
        } // end inner Column
        } // end outer Column

        if (speechAvailable) {
            FloatingActionButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.cd_voice_input)
                )
            }
        }

    } // end Box
}
