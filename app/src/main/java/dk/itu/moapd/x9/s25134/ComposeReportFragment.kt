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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

// Severity colors (matching colors.xml)
private val SeverityLow = Color(0xFF22C55E)
private val SeverityMedium = Color(0xFFF59E0B)
private val SeverityHigh = Color(0xFFEF4444)

/**
 * Filter Reports screen — lets users narrow down the report list by type
 * using filter chips. Built with Jetpack Compose.
 */
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
                    ComposeReportScreen(viewModel = viewModel)
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

@Composable
fun X9ComposeTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFA8C4FF),
            onPrimary = Color(0xFF003180),
            primaryContainer = Color(0xFF0040A8),
            onPrimaryContainer = Color(0xFFD6E4FF),
            secondary = Color(0xFFFFB94A),
            onSecondary = Color(0xFF3B2000),
            secondaryContainer = Color(0xFF593100),
            onSecondaryContainer = Color(0xFFFFDDB3),
            background = Color(0xFF101318),
            onBackground = Color(0xFFE1E3EC),
            surface = Color(0xFF191C20),
            onSurface = Color(0xFFE1E3EC),
            surfaceVariant = Color(0xFF44485A),
            onSurfaceVariant = Color(0xFFC4C8DA),
            outline = Color(0xFF8E92A4),
            outlineVariant = Color(0xFF44485A),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1A56DB),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD6E4FF),
            onPrimaryContainer = Color(0xFF00297A),
            secondary = Color(0xFFF5A623),
            onSecondary = Color(0xFF1C1400),
            secondaryContainer = Color(0xFFFFECD1),
            onSecondaryContainer = Color(0xFF2A1800),
            background = Color(0xFFF8F9FF),
            onBackground = Color(0xFF191C20),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF191C20),
            surfaceVariant = Color(0xFFE1E6F4),
            onSurfaceVariant = Color(0xFF44485A),
            outline = Color(0xFF74788A),
            outlineVariant = Color(0xFFC4C8DA),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun ComposeReportScreen(viewModel: ReportListViewModel) {
    val reports by viewModel.reports.observeAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("All") }

    // Track which report the user tapped to show in a dialog
    var selectedReport by remember { mutableStateOf<TrafficReport?>(null) }

    val filterOptions = listOf("All", "Speed Camera", "Heavy Traffic", "Accident", "Road Work")

    val filteredReports = remember(reports, selectedFilter) {
        if (selectedFilter == "All") reports
        else reports.filter { it.type == selectedFilter }
    }

    // Report detail dialog
    selectedReport?.let { report ->
        ReportDetailDialog(
            report = report,
            onDismiss = { selectedReport = null }
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
                key = { report -> "${report.type}_${report.description}_${report.severity}" }
            ) { report ->
                SwipeToDeleteContainer(
                    onDelete = { viewModel.removeReport(report) }
                ) {
                    TrafficReportCard(
                        report = report,
                        onClick = { selectedReport = report }
                    )
                }
            }
        }
    }
}

/**
 * Wraps a composable in a swipe-to-dismiss container.
 * Swiping from right to left reveals a red background and deletes the item.
 */
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    // Trigger delete when the user completes the swipe
    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
        LaunchedEffect(dismissState) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444)
                    else -> Color.Transparent
                },
                label = "swipe-bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        // Only allow right-to-left swipe
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = { content() }
    )
}

/**
 * Dialog showing full details of a tapped report.
 */
@Composable
fun ReportDetailDialog(report: TrafficReport, onDismiss: () -> Unit) {
    val severityLabel = when {
        report.severity <= 2 -> "Low"
        report.severity <= 3 -> "Medium"
        else -> "High"
    }
    val severityColor = when {
        report.severity <= 2 -> SeverityLow
        report.severity <= 3 -> SeverityMedium
        else -> SeverityHigh
    }

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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TrafficReportCard(report: TrafficReport, onClick: () -> Unit = {}) {
    val severityLabel = when {
        report.severity <= 2 -> "Low"
        report.severity <= 3 -> "Medium"
        else -> "High"
    }
    val severityColor = when {
        report.severity <= 2 -> SeverityLow
        report.severity <= 3 -> SeverityMedium
        else -> SeverityHigh
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
