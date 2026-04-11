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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.livedata.observeAsState
import dk.itu.moapd.x9.s25134.ui.components.X9BottomBar
import dk.itu.moapd.x9.s25134.ui.screens.DashboardScreen
import dk.itu.moapd.x9.s25134.ui.screens.LoginScreen
import dk.itu.moapd.x9.s25134.ui.screens.MapScreen
import dk.itu.moapd.x9.s25134.ui.screens.ProfileScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportDetailScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportFormScreen
import dk.itu.moapd.x9.s25134.ui.screens.ReportListScreen
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme
import dk.itu.moapd.x9.s25134.viewmodel.AuthViewModel
import dk.itu.moapd.x9.s25134.viewmodel.MapViewModel
import dk.itu.moapd.x9.s25134.viewmodel.ReportFormViewModel
import dk.itu.moapd.x9.s25134.viewmodel.ReportListViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ReportListViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private val reportFormViewModel: ReportFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            val isDarkModePreference by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val reports by viewModel.reports.observeAsState(initial = emptyList())
            val isDarkMode = isDarkModePreference ?: isSystemInDarkTheme()
            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

            // ReportFormViewModel state
            val locationDisplayName by reportFormViewModel.locationDisplayName.collectAsStateWithLifecycle()
            val locationLat by reportFormViewModel.latitude.collectAsStateWithLifecycle()
            val locationLng by reportFormViewModel.longitude.collectAsStateWithLifecycle()
            val isLoadingLocation by reportFormViewModel.isLoadingLocation.collectAsStateWithLifecycle()
            val selectedImageUri by reportFormViewModel.selectedImageUri.collectAsStateWithLifecycle()
            val isSubmitting by reportFormViewModel.isSubmitting.collectAsStateWithLifecycle()
            // MapViewModel state
            val userLocation by mapViewModel.userLocation.collectAsStateWithLifecycle()
            val mapType by mapViewModel.mapType.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val signInRequiredText = stringResource(R.string.msg_sign_in_required)

            val startDestination = "home"

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

            LaunchedEffect(Unit) {
                reportFormViewModel.submissionComplete.collect {
                    navController.popBackStack()
                }
            }

            LaunchedEffect(Unit) {
                reportFormViewModel.submissionError.collect { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

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
                                    navController.popBackStack("home", inclusive = false)
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
                                        reportFormViewModel.reset()
                                        navController.navigate("add")
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(signInRequiredText)
                                        }
                                    }
                                },
                                onMapClick = {
                                    navController.navigate("map") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
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
                                onSignInWithEmail = { e, p -> authViewModel.signInWithEmail(e, p) },
                                onRegisterWithEmail = { name, e, p -> authViewModel.registerWithEmail(name, e, p) },
                                onSignInWithGoogle = { idToken -> authViewModel.signInWithGoogle(idToken) },
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
                                        reportFormViewModel.reset()
                                        navController.navigate("add")
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(signInRequiredText) }
                                    }
                                },
                                onNavigateToMap = {
                                    navController.navigate("map") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToReports = {
                                    navController.navigate("reports") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToCriticalReports = {
                                    navController.navigate("reports?criticalOnly=true")
                                },
                                onNavigateToMyReports = {
                                    navController.navigate("reports?myReportsOnly=true")
                                },
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
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
                        composable(
                            route = "reports?myReportsOnly={myReportsOnly}&criticalOnly={criticalOnly}",
                            arguments = listOf(
                                navArgument("myReportsOnly") { type = NavType.BoolType; defaultValue = false },
                                navArgument("criticalOnly") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { back ->
                            ReportListScreen(
                                reports = reports,
                                currentUser = currentUser,
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                onNavigateToEdit = { id -> navController.navigate("edit/$id") },
                                onDeleteReport = { viewModel.deleteReport(it, currentUser?.uid) },
                                initialMyReportsOnly = back.arguments?.getBoolean("myReportsOnly") ?: false,
                                initialCriticalOnly = back.arguments?.getBoolean("criticalOnly") ?: false,
                                paddingValues = paddingValues
                            )
                        }
                        composable("add") {
                            ReportFormScreen(
                                reportId = null,
                                reports = reports,
                                currentUser = currentUser,
                                locationDisplayName = locationDisplayName,
                                locationLat = locationLat,
                                locationLng = locationLng,
                                isLoadingLocation = isLoadingLocation,
                                selectedImageUri = selectedImageUri,
                                isSubmitting = isSubmitting,
                                onRequestLocation = { reportFormViewModel.loadCurrentLocation() },
                                onImageSelected = { uri -> reportFormViewModel.setImageUri(uri) },
                                onSubmit = { report -> reportFormViewModel.submitNewReport(report) },
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
                            LaunchedEffect(reportId) {
                                reportFormViewModel.reset()
                                reports.find { it.id == reportId }?.let {
                                    reportFormViewModel.loadFromExistingReport(it)
                                }
                            }
                            ReportFormScreen(
                                reportId = reportId,
                                reports = reports,
                                currentUser = currentUser,
                                locationDisplayName = locationDisplayName,
                                locationLat = locationLat,
                                locationLng = locationLng,
                                isLoadingLocation = isLoadingLocation,
                                selectedImageUri = selectedImageUri,
                                isSubmitting = isSubmitting,
                                onRequestLocation = { reportFormViewModel.loadCurrentLocation() },
                                onImageSelected = { uri -> reportFormViewModel.setImageUri(uri) },
                                onSubmit = { report ->
                                    reportFormViewModel.submitUpdatedReport(report, currentUser?.uid)
                                },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable("map") {
                            MapScreen(
                                reports = reports,
                                userLocation = userLocation,
                                mapType = mapType,
                                onMapTypeChange = { mapViewModel.setMapType(it) },
                                onLoadUserLocation = { mapViewModel.loadUserLocation() },
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
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
