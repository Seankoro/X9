package dk.itu.moapd.x9.s25134.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    onSubmit: (TrafficReport) -> Unit,
    onBack: () -> Unit
) {
    val trafficTypes = stringArrayResource(R.array.traffic_types)

    // expanded doesn't need to survive rotation — dropdown can close on config change
    var expanded by remember { mutableStateOf(false) }
    // rememberSaveable for form fields so the user doesn't lose input on screen rotation
    var selectedType by rememberSaveable { mutableStateOf(trafficTypes[0]) }
    var description by rememberSaveable { mutableStateOf("") }
    var descriptionError by rememberSaveable { mutableStateOf<String?>(null) }
    var severityFloat by rememberSaveable { mutableFloatStateOf(1f) }

    // Map the slider float (1.0–5.0) to the corresponding Severity enum entry.
    // Severity.entries is ordered MINOR..CRITICAL so subtracting 1 gives the correct index.
    val severity: Severity = Severity.entries[severityFloat.roundToInt() - 1]

    // Severity colour — colorResource() must be inside the composable body
    val severityColor: Color = when {
        severity.level <= 2 -> colorResource(R.color.severity_low)
        severity.level <= 3 -> colorResource(R.color.severity_medium)
        else                -> colorResource(R.color.severity_high)
    }
    val severityLabel = when {
        severity.level <= 2 -> stringResource(R.string.severity_label_low)
        severity.level <= 3 -> stringResource(R.string.severity_label_medium)
        else                -> stringResource(R.string.severity_label_high)
    }

    // Hoisted here because stringResource() cannot be called inside a lambda (onClick, etc.)
    val errorEmptyDescription = stringResource(R.string.error_empty_description)

    val showDiscardDialog = rememberSaveable { mutableStateOf(false) }

    // Intercept the device in-built back button when the users have filled up something
    BackHandler(enabled = description.isNotEmpty()) {
        showDiscardDialog.value = true
    }

    // Await users confirmation before discarding all values
    if (showDiscardDialog.value) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog.value = false },
            title = { Text(stringResource(R.string.discard_report_title)) },
            text = { Text(stringResource(R.string.discard_report_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog.value = false
                    onBack()
                }) {
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.header_padding_horizontal))
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.title_text),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Report type dropdown
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
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                trafficTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description field
        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                // Clear the error as soon as the user types something meaningful
                if (it.trim().isNotEmpty()) descriptionError = null
            },
            label = { Text(stringResource(R.string.label_description)) },
            placeholder = { Text(stringResource(R.string.hint_description)) },
            isError = descriptionError != null,
            supportingText = descriptionError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
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
            steps = 3, // 3 intermediate steps between endpoints → 5 total positions (1–5)
            modifier = Modifier.fillMaxWidth()
        )

        // Severity chip
        Surface(
            shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
            color = severityColor
        ) {
            Text(
                text = stringResource(R.string.chip_severity_format, severityLabel, severity.level),
                style = MaterialTheme.typography.labelSmall,
                color = colorResource(R.color.on_severity_chip),
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.card_severity_chip_padding_horizontal),
                    vertical = dimensionResource(R.dimen.card_severity_chip_padding_vertical)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = {
                val trimmed = description.trim()
                if (trimmed.isEmpty()) {
                    descriptionError = errorEmptyDescription
                    return@Button
                }
                onSubmit(TrafficReport(type = selectedType, description = trimmed, severity = severity))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.button_submit))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReportFormScreenPreview() {
    X9ComposeTheme(darkTheme = false) {
        ReportFormScreen(onSubmit = {}, onBack = {})
    }
}
