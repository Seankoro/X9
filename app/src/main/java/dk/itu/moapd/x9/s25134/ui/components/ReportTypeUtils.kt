package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity

// Helper function to map report type to emojis
fun typeEmoji(type: String): String = when (type) {
    "Accident"      -> "🚨"
    "Heavy Traffic" -> "🚗"
    "Speed Camera"  -> "📸"
    "Road Work"     -> "🚧"
    else            -> "⚠️"
}

@Composable
fun severityColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> colorResource(R.color.severity_critical)
    Severity.HIGH     -> colorResource(R.color.severity_high)
    Severity.MODERATE -> colorResource(R.color.severity_moderate)
    Severity.LOW      -> colorResource(R.color.severity_low)
    Severity.MINOR    -> colorResource(R.color.severity_minor)
}

@Composable
fun severityBgColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> colorResource(R.color.severity_critical_bg)
    Severity.HIGH     -> colorResource(R.color.severity_high_bg)
    Severity.MODERATE -> colorResource(R.color.severity_moderate_bg)
    Severity.LOW      -> colorResource(R.color.severity_low_bg)
    Severity.MINOR    -> colorResource(R.color.severity_minor_bg)
}

@Composable
fun severityLabel(severity: Severity): String = when (severity) {
    Severity.CRITICAL -> stringResource(R.string.severity_label_critical)
    Severity.HIGH     -> stringResource(R.string.severity_label_high)
    Severity.MODERATE -> stringResource(R.string.severity_label_moderate)
    Severity.LOW      -> stringResource(R.string.severity_label_low)
    Severity.MINOR    -> stringResource(R.string.severity_label_minor)
}

// Shared severity badge used across TrafficReportCard, ReportDetailScreen, and MapScreen
// to keep badge styling consistent and eliminate duplicated layout code.
@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
