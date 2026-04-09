package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.StatCard
import dk.itu.moapd.x9.s25134.ui.components.TrafficReportCard
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import java.util.Calendar

@Composable
private fun timeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> stringResource(R.string.greeting_morning)
        hour < 17 -> stringResource(R.string.greeting_afternoon)
        else      -> stringResource(R.string.greeting_evening)
    }
}

@Composable
fun DashboardScreen(
    reports: List<TrafficReport>,
    currentUser: User?,
    onNavigateToAdd: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCriticalReports: () -> Unit,
    onNavigateToMyReports: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val showComingSoon = rememberSaveable { mutableStateOf(false) }

    if (showComingSoon.value) {
        AlertDialog(
            onDismissRequest = { showComingSoon.value = false },
            title = { Text(stringResource(R.string.coming_soon_title)) },
            text = { Text(stringResource(R.string.coming_soon_message)) },
            confirmButton = {
                TextButton(onClick = { showComingSoon.value = false }) {
                    Text(stringResource(R.string.ok_label))
                }
            }
        )
    }

    val criticalCount = reports.count { it.severity.level >= 4 }
    val recentReports = reports.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_medium))
    ) {
        // Greeting header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.screen_header_vertical_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeGreeting(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.greeting_welcome_back_user,
                            currentUser?.displayName ?: stringResource(R.string.label_guest)
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                // Avatar — tappable shortcut to Profile
                val avatarInitials = currentUser?.displayName
                    ?.split(" ")
                    ?.filter { it.isNotBlank() }
                    ?.take(2)
                    ?.joinToString("") { it.first().uppercase() }
                    ?.ifEmpty { currentUser.email.first().uppercase() }
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_box_size))
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarInitials != null) {
                        Text(
                            text = avatarInitials,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = "👤", fontSize = 18.sp)
                    }
                }
            }
        }

        // 2×2 Stat card grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                StatCard(
                    emoji = "📡",
                    count = "${reports.size}",
                    label = stringResource(R.string.stat_active_reports),
                    accentColor = colorResource(R.color.accent_teal),
                    modifier = Modifier.weight(1f).aspectRatio(1.45f)
                )
                StatCard(
                    emoji = "🔴",
                    count = "$criticalCount",
                    label = stringResource(R.string.stat_critical_alerts),
                    accentColor = colorResource(R.color.accent_pink),
                    modifier = Modifier.weight(1f).aspectRatio(1.45f),
                    onClick = onNavigateToCriticalReports
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.item_spacing)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                StatCard(
                    emoji = "📋",
                    count = "${reports.count { it.creatorId == currentUser?.uid }}",
                    label = stringResource(R.string.stat_your_reports),
                    accentColor = colorResource(R.color.accent_blue),
                    modifier = Modifier.weight(1f).aspectRatio(1.45f),
                    onClick = if (currentUser != null) onNavigateToMyReports else null
                )
                StatCard(
                    emoji = "✅",
                    count = "0",
                    label = stringResource(R.string.stat_resolved_today),
                    accentColor = colorResource(R.color.accent_yellow),
                    modifier = Modifier.weight(1f).aspectRatio(1.45f)
                )
            }
        }

        // Quick Actions
        item {
            Text(
                text = stringResource(R.string.section_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.screen_horizontal_padding), top = dimensionResource(R.dimen.spacing_large), bottom = dimensionResource(R.dimen.item_spacing))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_spacing))
            ) {
                QuickActionButton(
                    icon = Icons.Default.PostAdd,
                    label = stringResource(R.string.quick_action_new_report),
                    onClick = onNavigateToAdd,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Map,
                    label = stringResource(R.string.quick_action_view_map),
                    onClick = onNavigateToMap,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(R.string.quick_action_calendar),
                    onClick = { showComingSoon.value = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Reports header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.spacing_medium)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_recent_reports),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToReports) {
                    Text(
                        text = stringResource(R.string.btn_view_all),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Recent report cards (tap → detail, no swipe gestures)
        items(items = recentReports, key = { it.id }) { report ->
            TrafficReportCard(
                report = report,
                onClick = { onNavigateToDetail(report.id) }
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(dimensionResource(R.dimen.card_corner_radius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.quick_action_icon_size))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    X9ComposeTheme(darkTheme = true) {
        DashboardScreen(
            reports = listOf(
                TrafficReport("Accident", "Multi-car collision on E45", Severity.CRITICAL),
                TrafficReport("Heavy Traffic", "Slow traffic on Lyngbyvejen", Severity.MODERATE)
            ),
            currentUser = null,
            onNavigateToAdd = {},
            onNavigateToMap = {},
            onNavigateToReports = {},
            onNavigateToCriticalReports = {},
            onNavigateToMyReports = {},
            onNavigateToDetail = {},
            onNavigateToProfile = {}
        )
    }
}
