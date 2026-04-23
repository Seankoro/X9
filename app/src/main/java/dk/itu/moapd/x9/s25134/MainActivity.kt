package dk.itu.moapd.x9.s25134

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dk.itu.moapd.x9.s25134.speech.SpeechUiState
import dk.itu.moapd.x9.s25134.ui.components.PermissionOnboardingEffect
import dk.itu.moapd.x9.s25134.ui.components.SpeechOverlay
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
import dk.itu.moapd.x9.s25134.viewmodel.SettingsViewModel
import dk.itu.moapd.x9.s25134.viewmodel.SpeechViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel: ReportListViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private val reportFormViewModel: ReportFormViewModel by viewModels()
    private val speechViewModel: SpeechViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            val isDarkModePreference by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
            val reports by viewModel.reports.collectAsStateWithLifecycle()
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
            // ReportFormViewModel preFill state
            val preFillType by reportFormViewModel.preFillType.collectAsStateWithLifecycle()
            val preFillSeverity by reportFormViewModel.preFillSeverity.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val signInRequiredText = stringResource(R.string.msg_sign_in_required)

            // SpeechViewModel state
            val speechUiState by speechViewModel.uiState.collectAsStateWithLifecycle()
            val showSpeechOverlay = speechUiState != SpeechUiState.Idle
            val isSpeechAvailable = remember { speechViewModel.isAvailable() }

            val recordAudioLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) speechViewModel.startListening()
                else scope.launch {
                    snackbarHostState.showSnackbar(getString(R.string.error_mic_permission))
                }
            }

            fun onVoiceClick() {
                if (currentUser == null) {
                    scope.launch { snackbarHostState.showSnackbar(signInRequiredText) }
                    return
                }
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) speechViewModel.startListening()
                else recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            // SharedFlow collectors for one-shot events. Each runs in its own LaunchedEffect
            // so an error from one flow never blocks delivery from another.
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

            // Navigate back when the form signals a successful submission.
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

            val currentRouteState = androidx.compose.runtime.rememberUpdatedState(currentRoute)
            LaunchedEffect(Unit) {
                speechViewModel.speechResult.collect { result ->
                    if (currentRouteState.value == NavRoutes.ADD) {
                        reportFormViewModel.preFill(result)
                    } else {
                        reportFormViewModel.reset()
                        reportFormViewModel.preFill(result)
                        navController.navigate(NavRoutes.ADD)
                    }
                }
            }

            // Redirect to home automatically after a successful sign-in while on the login screen.
            LaunchedEffect(currentUser) {
                if (currentUser != null && currentRoute == NavRoutes.LOGIN) {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            }

            X9ComposeTheme(darkTheme = isDarkMode) {
                // Rationale dialogs must live inside X9ComposeTheme so Material3 tokens
                // (colors, typography, shapes) are available to AlertDialog.
                PermissionOnboardingEffect(onPermissionsReady = { viewModel.syncGeofences() })

                if (showSpeechOverlay) {
                    SpeechOverlay(
                        uiState = speechUiState,
                        onRetry = { speechViewModel.startListening() },
                        onDismiss = { speechViewModel.cancel() }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (currentRoute != NavRoutes.LOGIN) {
                            X9BottomBar(
                                currentRoute = currentRoute,
                                onHomeClick = {
                                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                                },
                                onReportsClick = {
                                    navController.navigate(NavRoutes.REPORTS) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onAddClick = {
                                    if (currentUser != null) {
                                        reportFormViewModel.reset()
                                        navController.navigate(NavRoutes.ADD)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(signInRequiredText)
                                        }
                                    }
                                },
                                onVoiceClick = { onVoiceClick() },
                                speechAvailable = isSpeechAvailable,
                                onMapClick = {
                                    navController.navigate(NavRoutes.MAP) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onProfileClick = {
                                    navController.navigate(NavRoutes.PROFILE) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
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
                        startDestination = NavRoutes.HOME
                    ) {
                        composable(NavRoutes.LOGIN) {
                            val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
                            LoginScreen(
                                isLoading = isLoading,
                                onSignInWithEmail = { e, p -> authViewModel.signInWithEmail(e, p) },
                                onRegisterWithEmail = { name, e, p -> authViewModel.registerWithEmail(name, e, p) },
                                onSignInWithGoogle = { context -> authViewModel.signInWithGoogleCredential(context) },
                                onContinueAsGuest = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.HOME) {
                            DashboardScreen(
                                reports = reports,
                                currentUser = currentUser,
                                onNavigateToAdd = {
                                    if (currentUser != null) {
                                        reportFormViewModel.reset()
                                        navController.navigate(NavRoutes.ADD)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(signInRequiredText) }
                                    }
                                },
                                onNavigateToMap = {
                                    navController.navigate(NavRoutes.MAP) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToReports = {
                                    navController.navigate(NavRoutes.REPORTS) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToCriticalReports = {
                                    navController.navigate(NavRoutes.reports(criticalOnly = true))
                                },
                                onNavigateToMyReports = {
                                    navController.navigate(NavRoutes.reports(myReportsOnly = true))
                                },
                                onNavigateToDetail = { id -> navController.navigate(NavRoutes.detail(id)) },
                                onNavigateToProfile = {
                                    navController.navigate(NavRoutes.PROFILE) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                paddingValues = paddingValues
                            )
                        }
                        composable(
                            route = NavRoutes.REPORTS_ROUTE,
                            arguments = listOf(
                                navArgument("myReportsOnly") { type = NavType.BoolType; defaultValue = false },
                                navArgument("criticalOnly") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { back ->
                            ReportListScreen(
                                reports = reports,
                                currentUser = currentUser,
                                onNavigateToDetail = { id -> navController.navigate(NavRoutes.detail(id)) },
                                onNavigateToEdit = { id -> navController.navigate(NavRoutes.edit(id)) },
                                onDeleteReport = { viewModel.deleteReport(it, currentUser?.uid) },
                                initialMyReportsOnly = back.arguments?.getBoolean("myReportsOnly") ?: false,
                                initialCriticalOnly = back.arguments?.getBoolean("criticalOnly") ?: false,
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.ADD) {
                            // Self-protecting auth guard: redirect to log in regardless of how
                            // this destination is reached (deep link, back stack, etc.).
                            if (currentUser == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate(NavRoutes.LOGIN) {
                                        popUpTo(NavRoutes.ADD) { inclusive = true }
                                    }
                                }
                                return@composable
                            }
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
                                preFillType = preFillType,
                                preFillSeverity = preFillSeverity,
                                onPreFillConsumed = { reportFormViewModel.clearPreFill() },
                                onVoiceClick = { onVoiceClick() },
                                speechAvailable = isSpeechAvailable,
                                onRequestLocation = { reportFormViewModel.loadCurrentLocation() },
                                onImageSelected = { uri -> reportFormViewModel.setImageUri(uri) },
                                onSubmit = { report -> reportFormViewModel.submitNewReport(report) },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.DETAIL_ROUTE) { back ->
                            val reportId = back.arguments?.getString("reportId") ?: return@composable
                            ReportDetailScreen(
                                reportId = reportId,
                                reports = reports,
                                currentUser = currentUser,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate(NavRoutes.edit(id)) },
                                onDelete = { id ->
                                    viewModel.deleteReport(id, currentUser?.uid)
                                },
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.EDIT_ROUTE) { back ->
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
                                preFillType = preFillType,
                                preFillSeverity = preFillSeverity,
                                onPreFillConsumed = { reportFormViewModel.clearPreFill() },
                                onVoiceClick = { onVoiceClick() },
                                onRequestLocation = { reportFormViewModel.loadCurrentLocation() },
                                onImageSelected = { uri -> reportFormViewModel.setImageUri(uri) },
                                onSubmit = { report ->
                                    reportFormViewModel.submitUpdatedReport(report, currentUser?.uid)
                                },
                                onBack = { navController.popBackStack() },
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.MAP) {
                            MapScreen(
                                reports = reports,
                                userLocation = userLocation,
                                mapType = mapType,
                                onMapTypeChange = { mapViewModel.setMapType(it) },
                                onLoadUserLocation = { mapViewModel.loadUserLocation() },
                                onNavigateToDetail = { id -> navController.navigate(NavRoutes.detail(id)) },
                                paddingValues = paddingValues
                            )
                        }
                        composable(NavRoutes.PROFILE) {
                            ProfileScreen(
                                currentUser = currentUser,
                                isDarkMode = isDarkMode,
                                onToggleDarkMode = { settingsViewModel.setDarkMode(!isDarkMode) },
                                onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
                                onSignOut = {
                                    authViewModel.signOut()
                                    navController.navigate(NavRoutes.HOME) {
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
