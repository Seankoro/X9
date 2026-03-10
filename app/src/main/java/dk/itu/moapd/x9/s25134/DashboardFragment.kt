package dk.itu.moapd.x9.s25134

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

/**
 * Dashboard — shows the report list, buttons to file a new report
 * or filter existing ones, and a dark mode toggle. Built with Jetpack Compose.
 */
class DashboardFragment : Fragment() {

    companion object {
        private const val TAG = "DashboardFragment"
    }

    private lateinit var viewModel: ReportListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        viewModel = ViewModelProvider(requireActivity())[ReportListViewModel::class.java]

        // Listen for reports coming back from ReportFragment
        parentFragmentManager.setFragmentResultListener("report_result", this) { _, bundle ->
            val report = BundleCompat.getParcelable(bundle, "report", TrafficReport::class.java)
            report?.let {
                viewModel.addReport(it)
                Log.d(TAG, "Report received from ReportFragment: $it")
                view?.let { root ->
                    Snackbar.make(root, R.string.report_submitted_toast, Snackbar.LENGTH_LONG).show()
                }
            }
        }
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
                    DashboardScreen(
                        viewModel = viewModel,
                        onOpenReporter = {
                            Log.d(TAG, "Opening ReportFragment")
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, ReportFragment())
                                .addToBackStack("report")
                                .commit()
                        },
                        onOpenFilter = {
                            Log.d(TAG, "Opening ComposeReportFragment")
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, ComposeReportFragment())
                                .addToBackStack("compose_reports")
                                .commit()
                        },
                        onToggleDarkMode = {
                            val currentlyDark = isCurrentlyInDarkMode()
                            val newDark = !currentlyDark

                            requireActivity()
                                .getSharedPreferences(
                                    MainActivity.PREFS_NAME,
                                    android.content.Context.MODE_PRIVATE
                                )
                                .edit()
                                .putBoolean(MainActivity.KEY_DARK_MODE, newDark)
                                .apply()

                            Log.d(TAG, "Dark mode toggled — dark=$newDark (was $currentlyDark)")

                            AppCompatDelegate.setDefaultNightMode(
                                if (newDark) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView() called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called")
    }

    private fun isCurrentlyInDarkMode(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO  -> false
            else -> resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: ReportListViewModel,
    onOpenReporter: () -> Unit,
    onOpenFilter: () -> Unit,
    onToggleDarkMode: () -> Unit
) {
    val reports by viewModel.reports.observeAsState(initial = emptyList())
    var selectedReport by remember { mutableStateOf<TrafficReport?>(null) }

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
        // Header band
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_traffic_alert),
                        contentDescription = "Traffic alert icon",
                        modifier = Modifier
                            .size(34.dp)
                            .padding(end = 12.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "X9",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dark_mode_toggle),
                            contentDescription = "Toggle dark mode",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Community traffic reporting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // Action button (overlaps header with negative offset)
        Button(
            onClick = onOpenReporter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-28).dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Text(
                text = "Report an Incident",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Filter button
        Button(
            onClick = onOpenFilter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-20).dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Filter Reports",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Section label
        Text(
            text = "TRAFFIC REPORTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp)
                .offset(y = (-8).dp),
            letterSpacing = 0.1.sp
        )

        // Report list with swipe-to-delete and click-to-view
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-4).dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            items(
                items = reports,
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