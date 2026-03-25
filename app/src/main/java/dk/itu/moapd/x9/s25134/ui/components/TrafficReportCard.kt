package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// typeEmoji() and severityBgColor() live in ReportTypeUtils.kt (shared with ReportDetailScreen)

@Composable
private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> colorResource(R.color.severity_critical)
    Severity.HIGH     -> colorResource(R.color.severity_high)
    Severity.MODERATE -> colorResource(R.color.severity_moderate)
    Severity.LOW      -> colorResource(R.color.severity_low)
    Severity.MINOR    -> colorResource(R.color.severity_minor)
}

@Composable
fun TrafficReportCard(
    report: TrafficReport,
    onClick: () -> Unit = {}
) {
    val timeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(report.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.card_padding_horizontal), vertical = dimensionResource(R.dimen.card_padding_vertical))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.card_content_padding), vertical = dimensionResource(R.dimen.card_content_padding_vertical)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji icon in tinted box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(severityBgColor(report.severity)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = typeEmoji(report.type), fontSize = 20.sp)
            }

            // Centre: title + location/time row
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (report.location.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = report.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: severity badge + active dot
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityBgColor(report.severity))
                        .padding(horizontal = dimensionResource(R.dimen.card_severity_chip_padding_horizontal), vertical = dimensionResource(R.dimen.card_severity_chip_padding_vertical))
                ) {
                    Text(
                        text = severityLabel(report.severity),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor(report.severity),
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrafficReportCardPreview() {
    X9ComposeTheme(darkTheme = true) {
        TrafficReportCard(
            report = TrafficReport(
                type = "Accident",
                description = "Multi-car collision blocking two lanes on E45",
                severity = Severity.CRITICAL,
                location = "Highway E45"
            )
        )
    }
}
