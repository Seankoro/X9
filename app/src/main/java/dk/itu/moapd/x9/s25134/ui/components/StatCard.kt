package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import dk.itu.moapd.x9.s25134.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

// Component to display total number of reports in the dashboard
@Composable
fun StatCard(
    emoji: String,
    count: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background glow circle (top-right)
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.stat_card_glow_size))
                    .offset(x = dimensionResource(R.dimen.spacing_small), y = -dimensionResource(R.dimen.stat_card_glow_y_offset))
                    .clip(CircleShape)
                    .align(Alignment.TopEnd)
                    .then(
                        Modifier.padding(0.dp) // clip container; color applied via background
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.stat_card_glow_size))
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.08f))
                )
            }
            Column(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_medium), top = dimensionResource(R.dimen.stat_card_top_padding), bottom = dimensionResource(R.dimen.spacing_medium), end = dimensionResource(R.dimen.spacing_medium))
            ) {
                Text(text = emoji, fontSize = 22.sp)
                Text(
                    text = count,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small))
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xs))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatCardPreview() {
    X9ComposeTheme(darkTheme = true) {
        StatCard(
            emoji = "📡",
            count = "8",
            label = "Active Reports",
            accentColor = Color(0xFF06d6a0)
        )
    }
}
