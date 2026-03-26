package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background glow circle (top-right)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .offset(x = 8.dp, y = (-10).dp)
                    .clip(CircleShape)
                    .align(Alignment.TopEnd)
                    .then(
                        Modifier.padding(0.dp) // clip container; color applied via background
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.08f))
                )
            }
            Column(
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing_medium), top = 18.dp, bottom = dimensionResource(R.dimen.spacing_medium), end = dimensionResource(R.dimen.spacing_medium))
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
                    modifier = Modifier.padding(top = 4.dp)
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
