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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.SeverityBadge
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.severityBgColor
import dk.itu.moapd.x9.s25134.ui.components.typeEmoji
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Screen to show detailed report information, every report card leads to this screen
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
    val showDeleteDialog = rememberSaveable { mutableStateOf(false) }

    if (report == null) {
        // Report was deleted — go back
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val dateFormatted = remember(report.createdAt) {
        dateFormatter.format(Instant.ofEpochMilli(report.createdAt))
    }

    if (showDeleteDialog.value) {
        // Alert to ask users for delete confirmation
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
                .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.spacing_medium))
        ) {
            // Emoji + type + severity badge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_box_size_large))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                        .background(severityBgColor(report.severity)),
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
                    SeverityBadge(severity = report.severity)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_medium)))

            // Location row
            val locationText = report.locationName.ifBlank {
                if (report.latitude != null && report.longitude != null)
                    stringResource(R.string.format_lat_lng, report.latitude, report.longitude)
                else null
            }
            if (locationText != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(dimensionResource(R.dimen.icon_size_location_pin)))
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
            }

            // Timestamp
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(dimensionResource(R.dimen.icon_size_location_pin)))
                Text(text = dateFormatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_medium)))

            // Full description
            Text(
                text = stringResource(R.string.label_description),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (report.imageUrl != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_medium)))

                Text(
                    text = stringResource(R.string.label_photo_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xs)))

                // AsyncImage handles downloading, decoding, and caching via Coil.
                // ContentScale.Fit preserves the original aspect ratio so portrait and landscape
                // photos display without cropping or distortion.
                // The placeholder shows a loading indicator while the image fetches;
                // the error parameter prevents a blank box on network failure.
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = stringResource(R.string.cd_report_photo),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                    error = painterResource(android.R.drawable.ic_menu_report_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                )
            }
        }

        // Edit + Delete buttons — only shown to the report's creator
        if (report.creatorId == currentUser?.uid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.spacing_medium)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                OutlinedButton(
                    onClick = { onEdit(report.id) },
                    modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.button_height_secondary)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius))
                ) {
                    Text(stringResource(R.string.edit_label), fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog.value = true },
                    modifier = Modifier.weight(1f).height(dimensionResource(R.dimen.button_height_secondary)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
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
            reports = listOf(TrafficReport("Accident", "Multi-car collision blocking two lanes on E45", Severity.CRITICAL, id = "preview")),
            currentUser = null,
            onBack = {}, onEdit = {}, onDelete = {}
        )
    }
}
