package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.Severity
import dk.itu.moapd.x9.s25134.model.TrafficReport
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.TrafficReportCard
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    reports: List<TrafficReport>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteReport: (String) -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val allLabel = stringResource(R.string.filter_all)
    val trafficTypes = stringArrayResource(R.array.traffic_types)
    val filterOptions = listOf(allLabel) + trafficTypes

    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val reportToDelete = remember { mutableStateOf<TrafficReport?>(null) }

    val filteredReports = remember(reports, selectedFilter, searchQuery) {
        reports
            .let { list -> if (selectedFilter == null) list else list.filter { it.type == selectedFilter } }
            .let { list ->
                if (searchQuery.isBlank()) list
                else list.filter {
                    it.type.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }
            }
    }

    if (reportToDelete.value != null) {
        AlertDialog(
            onDismissRequest = { reportToDelete.value = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    reportToDelete.value?.let { onDeleteReport(it.id) }
                    reportToDelete.value = null
                }) { Text(stringResource(R.string.delete_label)) }
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
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(title = stringResource(R.string.nav_reports))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_reports_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.filter_chips_padding_horizontal), vertical = 8.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.filter_chips_padding_horizontal)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.filter_chips_spacing))
        ) {
            filterOptions.forEach { filter ->
                val isAll = filter == allLabel
                FilterChip(
                    selected = if (isAll) selectedFilter == null else selectedFilter == filter,
                    onClick = { selectedFilter = if (isAll) null else filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Count
        Text(
            text = stringResource(R.string.report_count, filteredReports.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.filter_label_padding_start), top = 8.dp, bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(items = filteredReports, key = { it.id }) { report ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                onNavigateToEdit(report.id)
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                reportToDelete.value = report
                            }
                            else -> Unit
                        }
                        false // always snap back
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        val isEditSwipe = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                        val isDeleteSwipe = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    when {
                                        isEditSwipe   -> MaterialTheme.colorScheme.primary
                                        isDeleteSwipe -> MaterialTheme.colorScheme.error
                                        else          -> MaterialTheme.colorScheme.background
                                    }
                                )
                                .padding(horizontal = 20.dp),
                            contentAlignment = if (isEditSwipe) Alignment.CenterStart else Alignment.CenterEnd
                        ) {
                            when {
                                isEditSwipe -> Text(
                                    text = stringResource(R.string.edit_label),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                isDeleteSwipe -> Text(
                                    text = stringResource(R.string.delete_label),
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    TrafficReportCard(
                        report = report,
                        onClick = { onNavigateToDetail(report.id) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportListScreenPreview() {
    X9ComposeTheme(darkTheme = true) {
        ReportListScreen(
            reports = listOf(
                TrafficReport("Accident", "Multi-car collision on E45", Severity.CRITICAL, location = "Highway E45"),
                TrafficReport("Heavy Traffic", "Slow traffic on Lyngbyvejen", Severity.MODERATE, location = "Lyngbyvejen")
            ),
            onNavigateToDetail = {},
            onNavigateToEdit = {},
            onDeleteReport = {}
        )
    }
}
