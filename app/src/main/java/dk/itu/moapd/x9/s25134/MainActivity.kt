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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
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
import dk.itu.moapd.x9.s25134.viewmodel.AuthViewModel
import dk.itu.moapd.x9.s25134.viewmodel.ReportListViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ReportListViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            val isDarkModePreference by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val reports by viewModel.reports.observeAsState(initial = emptyList())
            val isDarkMode = isDarkModePreference ?: isSystemInDarkTheme()
            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val signInRequiredText = stringResource(R.string.msg_sign_in_required)
            val comingSoonText = stringResource(R.string.snackbar_coming_soon)

            // Always start at home — unauthenticated users can browse read-only content.
            // Sign-in is initiated from the Profile screen when needed.
            val startDestination = "home"

            // Collect auth errors from ViewModel and display as Snackbar.
            // Using SharedFlow avoids passing Compose-scoped lambdas into the ViewModel.
            LaunchedEffect(Unit) {
                authViewModel.authError.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.error.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            // React to auth state: navigate home when user signs in while on login screen
            LaunchedEffect(currentUser) {
                if (currentUser != null && currentRoute == "login") {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

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
                                onAddClick = {
                                    if (currentUser != null) {
                                        navController.navigate("add")
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(signInRequiredText)
                                        }
                                    }
                                },
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
                        startDestination = startDestination
                    ) {
                        composable("login") {
                            val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
                            LoginScreen(
                                isLoading = isLoading,
                                onSignInWithEmail = { e, p ->
                                    authViewModel.signInWithEmail(e, p)
                                },
                                onRegisterWithEmail = { name, e, p ->
                                    authViewModel.registerWithEmail(name, e, p)
                                },
                                onSignInWithGoogle = { idToken ->
                                    authViewModel.signInWithGoogle(idToken)
                                },
                                // Credential Manager errors (before Firebase is called) still
                                // surface via this lambda since they occur in the composable scope
                                onAuthError = { scope.launch { snackbarHostState.showSnackbar(it) } },
                                onContinueAsGuest = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable("home") {
                            DashboardScreen(
                                reports = reports,
                                currentUser = currentUser,
                                onNavigateToAdd = {
                                    if (currentUser != null) {
                                        navController.navigate("add")
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(signInRequiredText)
                                        }
                                    }
                                },
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
                                onNavigateToProfile = {
                                    navController.navigate("profile") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                paddingValues = paddingValues
                            )
                        }
                        composable("reports") {
                            ReportListScreen(
                                reports = reports,
                                currentUser = currentUser,
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                                onDeleteReport = { viewModel.deleteReport(it, currentUser?.uid) },
                                paddingValues = paddingValues
                            )
                        }
                        composable("add") {
                            ReportFormScreen(
                                reportId = null,
                                reports = reports,
                                currentUser = currentUser,
                                onSubmit = { report ->
                                    viewModel.addReport(report, currentUser?.uid)
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
                                currentUser = currentUser,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate("edit/$id") },
                                onDelete = { id ->
                                    viewModel.deleteReport(id, currentUser?.uid)
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
                                currentUser = currentUser,
                                onSubmit = { report ->
                                    viewModel.updateReport(report, currentUser?.uid)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                currentUser = currentUser,
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { viewModel.setDarkMode(!isDarkMode) },
                                onNavigateToLogin = { navController.navigate("login") },
                                onSignOut = {
                                    authViewModel.signOut()
                                    // Navigate back to home — ProfileScreen shows the sign-in
                                    // prompt automatically for unauthenticated users
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                paddingValues = paddingValues
                            )
                        }
                    }
                }
            }
        }
    }
}
