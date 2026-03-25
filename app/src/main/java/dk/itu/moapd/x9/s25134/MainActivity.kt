package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import dk.itu.moapd.x9.s25134.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.livedata.observeAsState
import dk.itu.moapd.x9.s25134.ui.components.X9BottomBar
import dk.itu.moapd.x9.s25134.ui.screens.DashboardScreen
import dk.itu.moapd.x9.s25134.ui.screens.LoginScreen
import dk.itu.moapd.x9.s25134.ui.screens.ProfileScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportDetailScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportFormScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportListScreen
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import dk.itu.moapd.x9.s25134.viewmodel.ReportListViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ReportListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            val isDarkModePreference by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val reports by viewModel.reports.observeAsState(initial = emptyList())
            val isDarkMode = isDarkModePreference ?: isSystemInDarkTheme()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val comingSoonText = stringResource(R.string.snackbar_coming_soon)

            X9ComposeTheme(darkTheme = isDarkMode) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (currentRoute != "login") {
                            X9BottomBar(
                                currentRoute = currentRoute,
                                onHomeClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onReportsClick = {
                                    navController.navigate("reports") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onAddClick = { navController.navigate("add") },
                                onMapClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(comingSoonText)
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate("profile") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            DashboardScreen(
                                reports = reports,
                                onNavigateToAdd = { navController.navigate("add") },
                                onNavigateToReports = {
                                    navController.navigate("reports") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                },
                                paddingValues = paddingValues
                            )
                        }
                        composable("reports") {
                            ReportListScreen(
                                reports = reports,
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                                onDeleteReport = { viewModel.deleteReport(it) },
                                paddingValues = paddingValues
                            )
                        }
                        composable("add") {
                            ReportFormScreen(
                                reportId = null,
                                reports = reports,
                                onSubmit = { report ->
                                    viewModel.addReport(report)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable("detail/{reportId}") { back ->
                            val reportId = back.arguments?.getString("reportId") ?: return@composable
                            ReportDetailScreen(
                                reportId = reportId,
                                reports = reports,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate("edit/$id") },
                                onDelete = { id ->
                                    viewModel.deleteReport(id)
                                    navController.popBackStack()
                                },
                                paddingValues = paddingValues
                            )
                        }
                        composable("edit/{reportId}") { back ->
                            val reportId = back.arguments?.getString("reportId") ?: return@composable
                            ReportFormScreen(
                                reportId = reportId,
                                reports = reports,
                                onSubmit = { report ->
                                    viewModel.updateReport(report)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { viewModel.setDarkMode(!isDarkMode) },
                                paddingValues = paddingValues
                            )
                        }
                        composable("login") {
                            LoginScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
