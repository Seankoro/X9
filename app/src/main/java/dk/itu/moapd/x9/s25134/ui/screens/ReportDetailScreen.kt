package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.severityLabel
import dk.itu.moapd.x9.s25134.ui.components.typeEmoji
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportDetailScreen(
    reportId: String,
    reports: List<TrafficReport>,
    currentUser: User?,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val report = reports.find { it.id == reportId }
    val showDeleteDialog = remember { mutableStateOf(false) }

    if (report == null) {
        // Report was deleted — go back
        onBack()
        return
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog.value = false; onDelete(report.id) }) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }

    val severityColor = when (report.severity) {
        Severity.CRITICAL -> colorResource(R.color.severity_critical)
        Severity.HIGH     -> colorResource(R.color.severity_high)
        Severity.MODERATE -> colorResource(R.color.severity_moderate)
        Severity.LOW      -> colorResource(R.color.severity_low)
        Severity.MINOR    -> colorResource(R.color.severity_minor)
    }
    val severityBgColor = when (report.severity) {
        Severity.CRITICAL -> colorResource(R.color.severity_critical_bg)
        Severity.HIGH     -> colorResource(R.color.severity_high_bg)
        Severity.MODERATE -> colorResource(R.color.severity_moderate_bg)
        Severity.LOW      -> colorResource(R.color.severity_low_bg)
        Severity.MINOR    -> colorResource(R.color.severity_minor_bg)
    }

    val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(report.createdAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ScreenHeader(title = stringResource(R.string.title_report_detail), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = 16.dp)
        ) {
            // Emoji + type + severity badge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(severityBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = typeEmoji(report.type), fontSize = 26.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.type,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(severityBgColor)
                            .padding(horizontal = dimensionResource(R.dimen.card_severity_chip_padding_horizontal), vertical = dimensionResource(R.dimen.card_severity_chip_padding_vertical))
                    ) {
                        Text(
                            text = severityLabel(report.severity),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Location row
            if (report.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(text = report.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Timestamp
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(text = dateFormatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Full description
            Text(
                text = stringResource(R.string.label_description),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Edit + Delete buttons — only shown to the report's creator
        if (report.creatorId == currentUser?.uid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onEdit(report.id) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.edit_label), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog.value = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_label), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportDetailScreenPreview() {
    X9ComposeTheme(darkTheme = true) {
        ReportDetailScreen(
            reportId = "preview",
            reports = listOf(TrafficReport("Accident", "Multi-car collision blocking two lanes on E45", Severity.CRITICAL, location = "Highway E45", id = "preview")),
            currentUser = null,
            onBack = {}, onEdit = {}, onDelete = {}
        )
    }
}
