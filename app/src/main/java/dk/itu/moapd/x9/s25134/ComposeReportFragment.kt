package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

// severity colors used across all screens
val SeverityLow    = Color(0xFF3B82F6)
val SeverityMedium = Color(0xFFF59E0B)
val SeverityHigh   = Color(0xFFEF4444)

// maps a severity int (1-5) to its display label + color
fun severityInfo(level: Int): Pair<String, Color> = when {
    level >= 4 -> "Critical" to Color(0xFFEF4444)
    level == 3 -> "High"     to Color(0xFFF97316)
    level == 2 -> "Medium"   to Color(0xFFF59E0B)
    else       -> "Low"      to Color(0xFF3B82F6)
}

// Filter Reports screen — filters reports by type using chips
class ComposeReportFragment : Fragment() {

    companion object {
        private const val TAG = "ComposeReportFragment"
    }

    private lateinit var viewModel: ReportListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        viewModel = ViewModelProvider(requireActivity())[ReportListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView() called")
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                X9ComposeTheme {
                    ComposeReportScreen(
                        viewModel = viewModel,
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid,
                        onEditReport = { /* handled by MainActivity in Task 9 */ }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop() called")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }
}

// mirrors the XML theme (Theme.X9) so Compose screens get the same colors
@Composable
fun X9ComposeTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF06D6A0),
            onPrimary = Color(0xFF0A0E1A),
            primaryContainer = Color(0xFF0D2A1F),
            onPrimaryContainer = Color(0xFF06D6A0),
            secondary = Color(0xFFFFD166),
            onSecondary = Color(0xFF1C1400),
            secondaryContainer = Color(0xFF3B2000),
            onSecondaryContainer = Color(0xFFFFE8A0),
            background = Color(0xFF0A0E1A),
            onBackground = Color(0xFFF0F4F8),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFF0F4F8),
            surfaceVariant = Color(0xFF1A2236),
            onSurfaceVariant = Color(0xFF8899B4),
            outline = Color(0xFF1E2A45),
            outlineVariant = Color(0xFF1E2A45),
            error = Color(0xFFEF476F),
            onError = Color(0xFFFFFFFF)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF04A07A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD0F5EA),
            onPrimaryContainer = Color(0xFF003D2A),
            secondary = Color(0xFFF59E0B),
            onSecondary = Color(0xFF1C1400),
            secondaryContainer = Color(0xFFFFF3CD),
            onSecondaryContainer = Color(0xFF2A1800),
            background = Color(0xFFF0F4F8),
            onBackground = Color(0xFF0A0E1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0A0E1A),
            surfaceVariant = Color(0xFFE8F5F0),
            onSurfaceVariant = Color(0xFF44685A),
            outline = Color(0xFF8AB4A0),
            outlineVariant = Color(0xFFC4DDD4),
            error = Color(0xFFEF476F),
            onError = Color(0xFFFFFFFF)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 40.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun StatCard(icon: String, count: Int, label: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(30.dp))
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = count.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AuthModal(onSignIn: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign In Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sign in to add a traffic report",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ComposeReportScreen(
    viewModel: ReportListViewModel,
    currentUserId: String?,
    onEditReport: (TrafficReport) -> Unit
) {
    val reports by viewModel.reports.observeAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("All") }

    val selectedReport = remember { mutableStateOf<TrafficReport?>(null) }

    val filterOptions = listOf("All", "Speed Camera", "Heavy Traffic", "Accident", "Road Work")

    val filteredReports = remember(reports, selectedFilter) {
        if (selectedFilter == "All") reports
        else reports.filter { it.type == selectedFilter }
    }

    // Report detail dialog
    selectedReport.value?.let { report ->
        ReportDetailDialog(
            report = report,
            currentUserId = currentUserId,
            onEdit = { onEditReport(report) },
            onDismiss = { selectedReport.value = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Filter Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Narrow down reports by type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // Filter label
        Text(
            text = "FILTER BY TYPE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            letterSpacing = 0.1.sp
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Report count
        Text(
            text = "${filteredReports.size} report(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
        )

        // Report list with swipe-to-delete
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            items(
                items = filteredReports,
                key = { report -> report.id.ifBlank { "${report.type}_${report.timestamp}" } }
            ) { report ->
                if (currentUserId != null && report.userId == currentUserId) {
                    SwipeActionsContainer(
                        onEdit = { onEditReport(report) },
                        onDelete = { viewModel.removeReport(report) }
                    ) {
                        TrafficReportCard(
                            report = report,
                            onClick = { selectedReport.value = report }
                        )
                    }
                } else {
                    TrafficReportCard(
                        report = report,
                        onClick = { selectedReport.value = report }
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeActionsContainer(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                dismissState.reset()
                onEdit()
            }
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            else -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF1A56DB)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                    else -> Color.Transparent
                },
                label = "swipe-bg"
            )
            val (label, alignment, padding) = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd ->
                    Triple("Edit", Alignment.CenterStart, PaddingValues(start = 24.dp))
                SwipeToDismissBoxValue.EndToStart ->
                    Triple("Delete", Alignment.CenterEnd, PaddingValues(end = 24.dp))
                else -> Triple("", Alignment.Center, androidx.compose.foundation.layout.PaddingValues())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(padding),
                contentAlignment = alignment
            ) {
                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        content = { content() }
    )
}

@Composable
fun ReportDetailDialog(
    report: TrafficReport,
    currentUserId: String? = null,
    onEdit: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val (severityLabel, severityColor) = severityInfo(report.severity)
    val isOwner = currentUserId != null && report.userId == currentUserId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = report.type,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Severity:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = severityColor
                    ) {
                        Text(
                            text = "$severityLabel (${report.severity}/5)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                if (!isOwner && report.userName.isNotBlank()) {
                    Text(
                        text = "Reported by ${report.userName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (isOwner) {
                TextButton(onClick = { onDismiss(); onEdit() }) {
                    Text("Edit")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (isOwner) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

private fun typeEmoji(type: String): String = when (type) {
    "Accident"      -> "🚨"
    "Heavy Traffic" -> "🚗"
    "Speed Camera"  -> "📸"
    "Road Work"     -> "🚧"
    else            -> "🚦"
}

private fun typeIconBg(type: String): Color = when (type) {
    "Accident"      -> Color(0xFFEF4444).copy(alpha = 0.09f)
    "Heavy Traffic" -> Color(0xFFF59E0B).copy(alpha = 0.09f)
    "Speed Camera"  -> Color(0xFF8B5CF6).copy(alpha = 0.09f)
    "Road Work"     -> Color(0xFFF97316).copy(alpha = 0.09f)
    else            -> Color(0xFF8899B4).copy(alpha = 0.09f)
}

private fun severityBadge(severity: Int): Triple<String, Color, Color> = when {
    severity >= 4 -> Triple("Critical", Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.13f))
    severity == 3 -> Triple("High",     Color(0xFFF97316), Color(0xFFF97316).copy(alpha = 0.13f))
    severity == 2 -> Triple("Medium",   Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.13f))
    else          -> Triple("Low",      Color(0xFF3B82F6), Color(0xFF3B82F6).copy(alpha = 0.13f))
}

@Composable
fun TrafficReportCard(report: TrafficReport, onClick: () -> Unit = {}) {
    val (badgeLabel, badgeText, badgeBg) = severityBadge(report.severity)
    val timeStr = remember(report.timestamp) {
        if (report.timestamp > 0L) {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(report.timestamp))
        } else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(typeIconBg(report.type), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = typeEmoji(report.type), fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (report.location.isNotBlank() || timeStr.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (report.location.isNotBlank()) {
                            Text(
                                text = "📍 ${report.location}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (report.location.isNotBlank() && timeStr.isNotBlank()) {
                            Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (timeStr.isNotBlank()) {
                            Text(
                                text = "🕐 $timeStr",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeLabel.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        letterSpacing = 0.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF06D6A0), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
