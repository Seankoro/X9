package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
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
fun FilterReportScreen(
    reports: List<TrafficReport>,
    onDeleteReport: (String) -> Unit
) {
    val allLabel = stringResource(R.string.filter_all)
    // null means "All" filters are selected
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val filterOptions = listOf(allLabel) + trafficTypes

    // Recompute the filtered list only when the source list or active filter changes
    val filteredReports = remember(reports, selectedFilter) {
        val filter = selectedFilter
        if (filter == null) reports
        else reports.filter { it.type == filter }
    }

    val reportToDelete = remember { mutableStateOf<TrafficReport?>(null) }

    // Pending delete confirmation from user
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
                    .padding(horizontal = dimensionResource(R.dimen.header_padding_horizontal))
                    .padding(
                        top = dimensionResource(R.dimen.header_padding_top),
                        bottom = dimensionResource(R.dimen.header_padding_bottom)
                    )
            ) {
                Text(
                    text = stringResource(R.string.filter_reports_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.filter_reports_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // Filter label
        Text(
            text = stringResource(R.string.filter_by_type),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.filter_label_padding_start),
                top = 16.dp,
                bottom = 8.dp
            ),
            letterSpacing = 0.1.sp
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.filter_chips_padding_horizontal)),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.filter_chips_spacing)
            )
        ) {
            filterOptions.forEach { filter ->
                val isAll = filter == allLabel
                FilterChip(
                    selected = if (isAll) selectedFilter == null else selectedFilter == filter,
                    onClick = { selectedFilter = if (isAll) null else filter },
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
            text = stringResource(R.string.report_count, filteredReports.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.filter_label_padding_start), top = 12.dp, bottom = 4.dp)
        )

        // weight(1f) gives the LazyColumn a bounded height equal to whatever space remains
        // after the header and chip row — required because Column passes unbounded height
        // to children by default, which breaks LazyColumn's measurement.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            items(items = filteredReports, key = { it.id }) { report ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            reportToDelete.value = report
                        }
                        false // always snap back; dialog handles the actual deletion
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
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
fun FilterReportScreenPreview() {
    X9ComposeTheme(darkTheme = false) {
        FilterReportScreen(
            reports = listOf(
                TrafficReport("Accident", "Multi-car collision blocking two lanes on E45", Severity.CRITICAL),
                TrafficReport("Heavy Traffic", "Slow-moving traffic on Lyngbyvejen", Severity.MODERATE),
                TrafficReport("Speed Camera", "Fixed speed camera at Folehaven 60 km/h zone", Severity.LOW)
            ),
            onDeleteReport = {}
        )
    }
}
