package dk.itu.moapd.x9.s25134.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.severityLabel
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    reportId: String? = null,
    reports: List<TrafficReport> = emptyList(),
    onSubmit: (TrafficReport) -> Unit,
    onBack: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val existingReport = remember(reportId) { reports.find { it.id == reportId } }
    val isEditMode = existingReport != null

    var expanded by remember { mutableStateOf(false) }
    var selectedType by rememberSaveable(reportId) { mutableStateOf(existingReport?.type ?: trafficTypes[0]) }
    var location by rememberSaveable(reportId) { mutableStateOf(existingReport?.location ?: "") }
    var description by rememberSaveable(reportId) { mutableStateOf(existingReport?.description ?: "") }
    var descriptionError by rememberSaveable(reportId) { mutableStateOf<String?>(null) }
    var severityFloat by rememberSaveable(reportId) {
        mutableFloatStateOf(existingReport?.severity?.level?.toFloat() ?: 1f)
    }

    val severity: Severity = Severity.entries[severityFloat.roundToInt() - 1]
    val hasInput = description.isNotBlank() || location.isNotBlank()
    val showDiscardDialog = rememberSaveable { mutableStateOf(false) }
    val errorEmptyDescription = stringResource(R.string.error_empty_description)

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
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            // Type dropdown
            Text(
                text = stringResource(R.string.label_type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    trafficTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = { selectedType = type; expanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location field
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(stringResource(R.string.label_location)) },
                placeholder = { Text(stringResource(R.string.hint_location)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Severity slider
            Text(
                text = stringResource(R.string.label_severity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = severityFloat,
                onValueChange = { severityFloat = it },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = stringResource(R.string.severity_level_display, severityLabel(severity), severity.level),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = {
                    val trimmed = description.trim()
                    if (trimmed.isEmpty()) { descriptionError = errorEmptyDescription; return@Button }
                    val report = if (isEditMode) {
                        existingReport.copy(type = selectedType, location = location.trim(), description = trimmed, severity = severity)
                    } else {
                        TrafficReport(type = selectedType, location = location.trim(), description = trimmed, severity = severity)
                    }
                    onSubmit(report)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(28.dp),
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

@Preview(showBackground = true)
@Composable
private fun ReportFormScreenAddPreview() {
    X9ComposeTheme(darkTheme = true) {
        ReportFormScreen(onSubmit = {}, onBack = {})
    }
}
