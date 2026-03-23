package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dk.itu.moapd.x9.s25134.ui.screens.DashboardScreen
import dk.itu.moapd.x9.s25134.ui.screens.FilterReportScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportFormScreen
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import dk.itu.moapd.x9.s25134.viewmodel.ReportListViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ReportListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        setContent {
            // isDarkMode is a StateFlow, so collectAsStateWithLifecycle is used.
            // reports is LiveData, so observeAsState bridges it into Compose state.
            val isDarkModePreference by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val reports by viewModel.reports.observeAsState(initial = emptyList())
            val navController = rememberNavController()

            // Resolve final dark mode value: saved user preference takes priority;
            // fall back to the device system setting on first launch (null preference).
            val isDarkMode = isDarkModePreference ?: isSystemInDarkTheme()

            X9ComposeTheme(darkTheme = isDarkMode) {
                // Initialize the navHost handling compose navigation automatically using the routes
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            reports = reports,
                            onToggleDarkMode = { viewModel.setDarkMode(!isDarkMode) },
                            onNavigateToReport = { navController.navigate("report") },
                            onNavigateToFilter = { navController.navigate("filter") },
                            onDeleteReport = { viewModel.deleteReport(it) }
                        )
                    }
                    composable("report") {
                        ReportFormScreen(
                            onSubmit = { report ->
                                viewModel.addReport(report)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("filter") {
                        // No onBack needed — system back pops this screen via Compose Navigation
                        FilterReportScreen(
                            reports = reports,
                            onDeleteReport = { viewModel.deleteReport(it) }
                        )
                    }
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
}
