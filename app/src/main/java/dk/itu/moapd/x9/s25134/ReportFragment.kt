package dk.itu.moapd.x9.s25134

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    existingReport: TrafficReport? = null,
    onSubmit: (type: String, location: String, description: String, severity: Int) -> Unit,
    onBack: () -> Unit
) {
    val trafficTypes = listOf("Speed Camera", "Heavy Traffic", "Accident", "Road Work")

    var selectedType by remember { mutableStateOf(existingReport?.type ?: trafficTypes[0]) }
    var location by remember { mutableStateOf(existingReport?.location ?: "") }
    var description by remember { mutableStateOf(existingReport?.description ?: "") }
    var severity by remember { mutableFloatStateOf((existingReport?.severity ?: 1).toFloat()) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasInput = description.isNotBlank() || location.isNotBlank()
    val isEditMode = existingReport != null

    BackHandler(enabled = hasInput) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (isEditMode) "Discard Changes?" else "Discard Report?") },
            text = {
                Text(
                    if (isEditMode) "Your edits will not be saved."
                    else "You have unsaved changes. Go back?"
                )
            },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = if (isEditMode) "Edit Report" else "New Report",
            onBack = { if (hasInput) showDiscardDialog = true else onBack() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Type of report", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType, onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }) {
                    trafficTypes.forEach { type ->
                        DropdownMenuItem(text = { Text(type) }, onClick = {
                            selectedType = type; typeDropdownExpanded = false
                        })
                    }
                }
            }

            Text("Location", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("e.g. Highway E45") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Text("Description", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; if (it.isNotEmpty()) descriptionError = null },
                placeholder = { Text("Situation description…") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                isError = descriptionError != null,
                supportingText = descriptionError?.let { { Text(it) } }
            )

            val severityInt = severity.toInt()
            val (_, severityColor) = severityInfo(severityInt)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Severity Level (1–5)", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(14.dp), color = severityColor) {
                    Text("$severityInt / 5", style = MaterialTheme.typography.labelLarge,
                        color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Low", style = MaterialTheme.typography.bodySmall, color = SeverityLow,
                    modifier = Modifier.weight(1f))
                Text("Medium", style = MaterialTheme.typography.bodySmall, color = SeverityMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("High", style = MaterialTheme.typography.bodySmall, color = SeverityHigh,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Slider(value = severity, onValueChange = { severity = it },
                valueRange = 1f..5f, steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                ))

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (description.trim().isEmpty()) {
                        descriptionError = "Description cannot be empty!"
                        return@Button
                    }
                    onSubmit(selectedType, location.trim(), description.trim(), severityInt)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(if (isEditMode) "Save Changes" else "Submit Report",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
