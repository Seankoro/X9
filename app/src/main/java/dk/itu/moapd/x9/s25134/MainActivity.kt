package dk.itu.moapd.x9.s25134

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")

        val viewModel = ViewModelProvider(this)[ReportListViewModel::class.java]

        setContent {
            X9ComposeTheme {
                X9App(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun X9App(viewModel: ReportListViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentUser = authState
    val currentUserId = currentUser?.uid
    val userDisplayName = currentUser?.displayName ?: currentUser?.email

    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogin by remember { mutableStateOf(false) }
    var showAuthModal by remember { mutableStateOf(false) }
    var reportToEdit by remember { mutableStateOf<TrafficReport?>(null) }

    val tabs = listOf(
        TabItem("Home",    "🏠", enabled = true),
        TabItem("Reports", "📋", enabled = true),
        TabItem("Add",     "➕", enabled = true),
        TabItem("Map",     "🗺️", enabled = false),
        TabItem("Profile", "👤", enabled = false)
    )

    if (showAuthModal) {
        AuthModal(
            onSignIn = { showAuthModal = false; showLogin = true },
            onDismiss = { showAuthModal = false }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!showLogin) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    if (!tab.enabled) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Coming soon")
                                        }
                                        return@NavigationBarItem
                                    }
                                    if (index == 2 && currentUserId == null) {
                                        showAuthModal = true
                                        return@NavigationBarItem
                                    }
                                    selectedTab = index
                                },
                                icon = {
                                    Text(
                                        text = tab.icon,
                                        color = if (tab.enabled) Color.Unspecified
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        color = if (tab.enabled) Color.Unspecified
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                },
                                enabled = tab.enabled
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                            viewModel = viewModel,
                            currentUserId = currentUserId,
                            userDisplayName = userDisplayName,
                            onSwitchToAdd = {
                                if (currentUserId != null) selectedTab = 2
                                else showAuthModal = true
                            },
                            onSwitchToReports = { selectedTab = 1 },
                            onShowSignIn = { showLogin = true },
                            onEditReport = { report ->
                                reportToEdit = report
                                selectedTab = 2
                            },
                            onComingSoon = {
                                scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                            }
                         )
                    1 -> ComposeReportScreen(
                            viewModel = viewModel,
                            currentUserId = currentUserId,
                            onEditReport = { report ->
                                reportToEdit = report
                                selectedTab = 2
                            }
                         )
                    2 -> ReportFormScreen(
                            existingReport = reportToEdit,
                            onSubmit = { type, location, description, severity ->
                                if (reportToEdit != null) {
                                    viewModel.updateReport(
                                        reportToEdit!!.copy(
                                            type = type, location = location,
                                            description = description, severity = severity
                                        )
                                    )
                                } else {
                                    viewModel.addReport(
                                        TrafficReport(
                                            type = type, location = location,
                                            description = description, severity = severity,
                                            userId = currentUserId ?: "",
                                            userName = userDisplayName ?: "",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                                reportToEdit = null
                                selectedTab = 0
                            },
                            onBack = {
                                val wasEditing = reportToEdit != null
                                reportToEdit = null
                                selectedTab = if (wasEditing) 1 else 0
                            }
                         )
                }
            }
        }

        if (showLogin) {
            LoginScreen(
                onSignInSuccess = { showLogin = false }
            )
        }
    }
}

data class TabItem(val label: String, val icon: String, val enabled: Boolean)
