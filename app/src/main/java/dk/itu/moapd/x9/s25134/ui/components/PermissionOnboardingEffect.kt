package dk.itu.moapd.x9.s25134.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dk.itu.moapd.x9.s25134.R

private const val PERM_STEP_IDLE = 0
private const val PERM_STEP_NOTIFICATIONS = 1
private const val PERM_STEP_FINE_LOCATION = 2
private const val PERM_STEP_BACKGROUND_LOCATION = 3
private const val PERM_STEP_DONE = 4

// Orchestrates the three-permission onboarding flow required for proximity alerts:
//   1. POST_NOTIFICATIONS
//   2. ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION (must be requested together)
//   3. ACCESS_BACKGROUND_LOCATION (must follow fine location)
// For each permission, there might be android version mismatches, check the current version
// against the version that requires that permission.
// Each step shows a rationale AlertDialog before the system prompt.
// Already-granted permissions are skipped automatically on every launch.
@Composable
fun PermissionOnboardingEffect(onPermissionsReady: () -> Unit) {
    val context = LocalContext.current
    // rememberUpdatedState ensures the LaunchedEffect below always calls the latest
    // lambda even if the parent recomposes before PERM_STEP_DONE is reached.
    val currentOnPermissionsReady by rememberUpdatedState(onPermissionsReady)
    var permStep by remember { mutableIntStateOf(PERM_STEP_IDLE) }
    var showNotifRationale by remember { mutableStateOf(false) }
    var showFineLocRationale by remember { mutableStateOf(false) }
    var showBgLocRationale by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permStep = PERM_STEP_FINE_LOCATION }

    // Fine + coarse must be requested together
    val fineLocPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permStep = PERM_STEP_BACKGROUND_LOCATION }

    val bgLocPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permStep = PERM_STEP_DONE }

    // Determine the opening step once on first composition.
    LaunchedEffect(Unit) {
        permStep = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) PERM_STEP_NOTIFICATIONS else PERM_STEP_FINE_LOCATION
    }

    LaunchedEffect(permStep) {
        when (permStep) {
            PERM_STEP_NOTIFICATIONS -> showNotifRationale = true
            PERM_STEP_FINE_LOCATION -> {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (fineGranted) permStep = PERM_STEP_BACKGROUND_LOCATION
                else showFineLocRationale = true
            }
            PERM_STEP_BACKGROUND_LOCATION -> {
                // ACCESS_BACKGROUND_LOCATION only exists on API 29+. On API 28 foreground
                // location implicitly grants background access
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permStep = PERM_STEP_DONE
                    return@LaunchedEffect
                }
                val fineGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val bgGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                when {
                    bgGranted -> permStep = PERM_STEP_DONE
                    fineGranted -> showBgLocRationale = true
                    // Fine location was denied — background location cannot be requested.
                    else -> permStep = PERM_STEP_DONE
                }
            }
            PERM_STEP_DONE -> currentOnPermissionsReady()
        }
    }

    if (showNotifRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = {
                showNotifRationale = false
                permStep = PERM_STEP_FINE_LOCATION
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
                    permStep = PERM_STEP_FINE_LOCATION
                }) { Text(stringResource(R.string.perm_rationale_not_now)) }
            }
        )
    }

    if (showFineLocRationale) {
        AlertDialog(
            onDismissRequest = {
                showFineLocRationale = false
                permStep = PERM_STEP_BACKGROUND_LOCATION
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
                    permStep = PERM_STEP_BACKGROUND_LOCATION
                }) { Text(stringResource(R.string.perm_rationale_not_now)) }
            }
        )
    }

    if (showBgLocRationale) {
        AlertDialog(
            onDismissRequest = {
                showBgLocRationale = false
                permStep = PERM_STEP_DONE
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
                    permStep = PERM_STEP_DONE
                }) { Text(stringResource(R.string.perm_rationale_not_now)) }
            }
        )
    }
}
