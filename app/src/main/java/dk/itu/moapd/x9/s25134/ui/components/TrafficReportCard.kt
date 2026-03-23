package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

// Reusable UI component that takes in a TrafficReport class and displays it on the UI
// Similar to components in React.
@Composable
fun TrafficReportCard(report: TrafficReport) {
    // colorResource() must be called inside a composable, not as top-level vals
    val severityColor: Color = when {
        report.severity.level <= 2 -> colorResource(R.color.severity_low)
        report.severity.level <= 3 -> colorResource(R.color.severity_medium)
        else                       -> colorResource(R.color.severity_high)
    }
    val severityLabel = when {
        report.severity.level <= 2 -> stringResource(R.string.severity_label_low)
        report.severity.level <= 3 -> stringResource(R.string.severity_label_medium)
        else                       -> stringResource(R.string.severity_label_high)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.card_padding_horizontal),
                vertical = dimensionResource(R.dimen.card_padding_vertical)
            ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.card_content_padding))) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.type,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
                    color = severityColor
                ) {
                    Text(
                        text = stringResource(
                            R.string.chip_severity_format,
                            severityLabel,
                            report.severity.level
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorResource(R.color.on_severity_chip),
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.card_severity_chip_padding_horizontal),
                            vertical = dimensionResource(R.dimen.card_severity_chip_padding_vertical)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrafficReportCardPreview() {
    X9ComposeTheme(darkTheme = false) {
        TrafficReportCard(
            report = TrafficReport(
                type = "Speed Camera",
                description = "Fixed speed camera active at Folehaven 60 km/h zone",
                severity = Severity.LOW
            )
        )
    }
}
