package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.components.TrafficReportCard
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    reports: List<TrafficReport>,
    onToggleDarkMode: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToFilter: () -> Unit,
    onDeleteReport: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Capture the count at first composition so we can distinguish new additions from the
    // initial seed data — without this, the snackbar would fire on every recomposition.
    val initialReportCount = remember { reports.size }
    val listState = rememberLazyListState()
    val reportSubmittedMessage = stringResource(R.string.report_submitted_toast)
    // Holds the report pending deletion until the user confirms the dialog.
    val reportToDelete = remember { mutableStateOf<TrafficReport?>(null) }

    LaunchedEffect(reports.size) {
        if (reports.size > initialReportCount) {
            snackbarHostState.showSnackbar(reportSubmittedMessage)
        }
    }

    // Prompt the user for deletion confirmation
    if (reportToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { reportToDelete.value = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    reportToDelete.value?.let { onDeleteReport(it.id) }
                    reportToDelete.value = null
                }) {
                    Text(stringResource(R.string.delete_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete.value = null }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Single LazyColumn for the whole screen so header and buttons scroll with the list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header band
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.header_padding_horizontal))
                            .padding(
                                top = dimensionResource(R.dimen.header_padding_top),
                                bottom = dimensionResource(R.dimen.header_padding_bottom)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_traffic_alert),
                            contentDescription = stringResource(R.string.header_icon_desc),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.subtitle_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = onToggleDarkMode) {
                            Icon(
                                painter = painterResource(R.drawable.ic_dark_mode_toggle),
                                contentDescription = stringResource(R.string.btn_toggle_dark_mode),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Section label
            item {
                Text(
                    text = stringResource(R.string.section_traffic_reports),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.filter_label_padding_start),
                        top = 16.dp,
                        bottom = 8.dp
                    ),
                    letterSpacing = 0.1.sp
                )
            }

            // Navigation buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.card_padding_horizontal)),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToReport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_create_report))
                    }
                    OutlinedButton(
                        onClick = onNavigateToFilter,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_compose_reports))
                    }
                }
            }

            // Report list with swipe-to-delete
            items(items = reports, key = { it.id }) { report ->
                // confirmValueChange always returns false so the card snaps back after
                // the swipe — the actual deletion is deferred to the confirmation dialog.
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            reportToDelete.value = report
                        }
                        false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        // Only show the red background once the swipe passes the dismiss threshold
                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isSwiping) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.background
                                )
                                .padding(end = dimensionResource(R.dimen.card_padding_horizontal)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (isSwiping) {
                                Text(
                                    text = stringResource(R.string.delete_label),
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    TrafficReportCard(report = report)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    X9ComposeTheme(darkTheme = false) {
        DashboardScreen(
            reports = listOf(
                TrafficReport("Accident", "Multi-car collision blocking two lanes on E45", Severity.CRITICAL),
                TrafficReport("Speed Camera", "Fixed speed camera at Folehaven 60 km/h zone", Severity.LOW)
            ),
            onToggleDarkMode = {},
            onNavigateToReport = {},
            onNavigateToFilter = {},
            onDeleteReport = {}
        )
    }
}
