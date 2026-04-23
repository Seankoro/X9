package dk.itu.moapd.x9.s25134.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.LocationPreviewSection
import dk.itu.moapd.x9.s25134.ui.components.PhotoPickerSection
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.severityLabel
import java.time.Instant
import kotlin.math.roundToInt

private const val TAG = "ReportFormScreen"

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
                    LocationPreviewSection(
                        isLoadingLocation = isLoadingLocation,
                        locationLat = locationLat,
                        locationLng = locationLng,
                        locationDisplayName = locationDisplayName
                    )
                }

                // Photo section — available for both new reports and edits
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
                Text(
                    text = stringResource(R.string.label_photo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))
                PhotoPickerSection(
                    selectedImageUri = selectedImageUri,
                    existingImageUrl = existingReport?.imageUrl,
                    photoExplicitlyRemoved = photoExplicitlyRemoved,
                    onPhotoSelected = { uri ->
                        photoExplicitlyRemoved = false
                        onImageSelected(uri)
                    },
                    onRemovePhoto = {
                        photoExplicitlyRemoved = true
                        onImageSelected(null)
                    }
                )

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
            }
        }

        if (speechAvailable) {
            FloatingActionButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimensionResource(R.dimen.spacing_medium)),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.cd_voice_input)
                )
            }
        }
    }
}
