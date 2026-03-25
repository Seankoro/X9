package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity

fun typeEmoji(type: String): String = when (type) {
    "Accident"      -> "🚨"
    "Heavy Traffic" -> "🚗"
    "Speed Camera"  -> "📸"
    "Road Work"     -> "🚧"
    else            -> "⚠️"
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
