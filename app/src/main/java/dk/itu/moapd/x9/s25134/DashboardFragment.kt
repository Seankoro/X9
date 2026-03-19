package dk.itu.moapd.x9.s25134

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: ReportListViewModel,
    currentUserId: String?,
    userDisplayName: String?,
    onSwitchToAdd: () -> Unit,
    onSwitchToReports: () -> Unit,
    onShowSignIn: () -> Unit,
    onEditReport: (TrafficReport) -> Unit = {},
    onComingSoon: () -> Unit = {}
) {
    val reports by viewModel.reports.observeAsState(initial = emptyList())
    val selectedReport = remember { mutableStateOf<TrafficReport?>(null) }

    val activeCount = reports.size
    val criticalCount = reports.count { it.severity >= 4 }
    val yourCount = if (currentUserId != null) reports.count { it.userId == currentUserId } else 0
    val todayCount = remember(reports) {
        val cal = Calendar.getInstance()
        val todayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        reports.count { it.timestamp >= todayStart }
    }

    selectedReport.value?.let { report ->
        ReportDetailDialog(
            report = report,
            currentUserId = currentUserId,
            onEdit = {
                selectedReport.value = null
                onEditReport(report)
            },
            onDismiss = { selectedReport.value = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (userDisplayName != null) "Hello, $userDisplayName 👋"
                               else "Welcome back 👋",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFF06D6A0), Color(0xFF118AB2))
                                ),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userDisplayName?.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A0E1A)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFEF476F), RoundedCornerShape(6.dp))
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("📡", activeCount, "Active Reports", MaterialTheme.colorScheme.primary)
                    StatCard("📋", yourCount, "Your Reports", Color(0xFF3B82F6))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("🔴", criticalCount, "Critical Alerts", MaterialTheme.colorScheme.error)
                    StatCard("📅", todayCount, "Today's Reports", Color(0xFFFFD166))
                }
            }
        }

        item {
            Text("Quick Actions",
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton("➕", "New Report", modifier = Modifier.weight(1f),
                    onClick = onSwitchToAdd)
                QuickActionButton("🗺️", "View Map", modifier = Modifier.weight(1f),
                    onClick = { onComingSoon() })
                QuickActionButton("📅", "Calendar", modifier = Modifier.weight(1f),
                    onClick = { onComingSoon() })
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Reports", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = onSwitchToReports) {
                    Text("View all →", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        val recent = reports.take(4)
        items(recent, key = { it.id.ifBlank { "${it.type}_${it.timestamp}" } }) { report ->
            TrafficReportCard(report = report, onClick = { selectedReport.value = report })
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
