package dk.itu.moapd.x9.s25134

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

        // Step values for the permission onboarding state machine.
        private const val PERM_STEP_IDLE = 0
        private const val PERM_STEP_NOTIFICATIONS = 1
        private const val PERM_STEP_FINE_LOCATION = 2
        private const val PERM_STEP_BACKGROUND_LOCATION = 3
        private const val PERM_STEP_DONE = 4
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

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val signInRequiredText = stringResource(R.string.msg_sign_in_required)

            val startDestination = "home"

            // ---- Permission onboarding ----
            // Three permissions are required for proximity alerts, each requested in sequence:
            //   1. POST_NOTIFICATIONS (Android 13+)
            //   2. ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION (together, single dialog)
            //   3. ACCESS_BACKGROUND_LOCATION (must be separate and after fine location)
            // Each step shows a rationale AlertDialog before the system prompt so the user
            // understands why the permission is needed and what to choose.
            // The flow reruns on every launch — steps that are already granted are skipped
            // automatically. Android's own "don't ask again" mechanism handles permanent denial.
            var proximityPermStep by remember { mutableIntStateOf(PERM_STEP_IDLE) }
            var showNotifRationale by remember { mutableStateOf(false) }
            var showFineLocRationale by remember { mutableStateOf(false) }
            var showBgLocRationale by remember { mutableStateOf(false) }

            val notifPermLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { proximityPermStep = PERM_STEP_FINE_LOCATION }

            // Fine + coarse must be requested together; Android shows a single
            // "Precise / Approximate" dialog on API 31+.
            val fineLocPermLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { proximityPermStep = PERM_STEP_BACKGROUND_LOCATION }

            val bgLocPermLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { proximityPermStep = PERM_STEP_DONE }

            // On each launch, determine which step to start from.
            LaunchedEffect(Unit) {
                proximityPermStep = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) PERM_STEP_NOTIFICATIONS else PERM_STEP_FINE_LOCATION
            }

            // Advance through permission steps, showing a rationale dialog before each request.
            LaunchedEffect(proximityPermStep) {
                when (proximityPermStep) {
                    PERM_STEP_NOTIFICATIONS -> showNotifRationale = true
                    PERM_STEP_FINE_LOCATION -> {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted) proximityPermStep = PERM_STEP_BACKGROUND_LOCATION
                        else showFineLocRationale = true
                    }
                    PERM_STEP_BACKGROUND_LOCATION -> {
                        // ACCESS_BACKGROUND_LOCATION only exists on API 29+. On API 28 foreground
                        // location implicitly grants background access — nothing to request.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            proximityPermStep = PERM_STEP_DONE
                            return@LaunchedEffect
                        }
                        val fineGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val bgGranted = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        when {
                            bgGranted -> proximityPermStep = PERM_STEP_DONE
                            fineGranted -> showBgLocRationale = true
                            // Fine location was denied — background location cannot be requested.
                            else -> proximityPermStep = PERM_STEP_DONE
                        }
                    }
                    PERM_STEP_DONE -> viewModel.syncGeofences()
                }
            }

            // ---- End permission onboarding ----

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

            // Redirect to home automatically after a successful sign-in while on the login screen.
            LaunchedEffect(currentUser) {
                if (currentUser != null && currentRoute == "login") {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            X9ComposeTheme(darkTheme = isDarkMode) {
                // Rationale dialogs must live inside X9ComposeTheme so Material3 tokens
                // (colors, typography, shapes) are available to AlertDialog.
                if (showNotifRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    AlertDialog(
                        onDismissRequest = {
                            showNotifRationale = false
                            proximityPermStep = PERM_STEP_FINE_LOCATION
                        },
                        title = { Text(stringResource(R.string.perm_rationale_notif_title)) },
                        text = { Text(stringResource(R.string.perm_rationale_notif_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showNotifRationale = false
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) { Text(stringResource(R.string.perm_rationale_continue)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showNotifRationale = false
                                proximityPermStep = PERM_STEP_FINE_LOCATION
                            }) { Text(stringResource(R.string.perm_rationale_not_now)) }
                        }
                    )
                }

                if (showFineLocRationale) {
                    AlertDialog(
                        onDismissRequest = {
                            showFineLocRationale = false
                            proximityPermStep = PERM_STEP_BACKGROUND_LOCATION
                        },
                        title = { Text(stringResource(R.string.perm_rationale_fine_loc_title)) },
                        text = { Text(stringResource(R.string.perm_rationale_fine_loc_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showFineLocRationale = false
                                fineLocPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }) { Text(stringResource(R.string.perm_rationale_continue)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showFineLocRationale = false
                                proximityPermStep = PERM_STEP_BACKGROUND_LOCATION
                            }) { Text(stringResource(R.string.perm_rationale_not_now)) }
                        }
                    )
                }

                if (showBgLocRationale) {
                    AlertDialog(
                        onDismissRequest = {
                            showBgLocRationale = false
                            proximityPermStep = PERM_STEP_DONE
                        },
                        title = { Text(stringResource(R.string.perm_rationale_bg_loc_title)) },
                        text = { Text(stringResource(R.string.perm_rationale_bg_loc_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showBgLocRationale = false
                                bgLocPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }) { Text(stringResource(R.string.perm_rationale_continue)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showBgLocRationale = false
                                proximityPermStep = PERM_STEP_DONE
                            }) { Text(stringResource(R.string.perm_rationale_not_now)) }
                        }
                    )
                }

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
                                onSignInWithGoogle = { context -> authViewModel.signInWithGoogleCredential(context) },
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
                            // Self-protecting auth guard: redirect to log in regardless of how
                            // this destination is reached (deep link, back stack, etc.).
                            if (currentUser == null) {
                                LaunchedEffect(Unit) {
                                    navController.navigate("login") {
                                        popUpTo("add") { inclusive = true }
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
