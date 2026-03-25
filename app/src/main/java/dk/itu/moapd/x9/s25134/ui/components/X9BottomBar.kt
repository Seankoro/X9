package dk.itu.moapd.x9.s25134.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

@Composable
fun X9BottomBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onReportsClick: () -> Unit,
    onAddClick: () -> Unit,
    onMapClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val isReportsActive = currentRoute == "reports" ||
        currentRoute == "detail/{reportId}" ||
        currentRoute == "edit/{reportId}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.nav_home),
                selected = currentRoute == "home",
                onClick = onHomeClick
            )
            BottomNavItem(
                icon = Icons.Default.List,
                label = stringResource(R.string.nav_reports),
                selected = isReportsActive,
                onClick = onReportsClick
            )
            // FAB-style Add button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_report),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            BottomNavItem(
                icon = Icons.Default.Map,
                label = stringResource(R.string.nav_map),
                selected = false,
                onClick = onMapClick
            )
            BottomNavItem(
                icon = Icons.Default.Person,
                label = stringResource(R.string.nav_profile),
                selected = currentRoute == "profile",
                onClick = onProfileClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Preview(showBackground = true)
@Composable
private fun X9BottomBarPreview() {
    X9ComposeTheme(darkTheme = true) {
        X9BottomBar(
            currentRoute = "home",
            onHomeClick = {}, onReportsClick = {}, onAddClick = {},
            onMapClick = {}, onProfileClick = {}
        )
    }
}
