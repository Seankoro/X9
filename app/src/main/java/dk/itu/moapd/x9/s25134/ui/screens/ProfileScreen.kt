package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.model.User
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.components.userInitials
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

// Profile screen to show user details and relevant setting, mostly empty but open for future extensions
@Composable
fun ProfileScreen(
    currentUser: User?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSignOut: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    val showComingSoon = remember { mutableStateOf(false) }

    if (showComingSoon.value) {
        AlertDialog(
            onDismissRequest = { showComingSoon.value = false },
            title = { Text(stringResource(R.string.coming_soon_title)) },
            text = { Text(stringResource(R.string.coming_soon_message)) },
            confirmButton = {
                TextButton(onClick = { showComingSoon.value = false }) {
                    Text(stringResource(R.string.ok_label))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        ScreenHeader(title = stringResource(R.string.nav_profile))

        if (currentUser == null) {
            // Unauthenticated state — prompt to sign in
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                Text(
                    text = stringResource(R.string.btn_sign_in_register),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.button_height_primary)),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.btn_sign_in_register),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            // Authenticated state — user info header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.screen_header_vertical_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Initials avatar
                val initials = userInitials(currentUser.displayName, currentUser.email)

                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_box_size_large))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentUser.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)))
        }

        // Dark mode toggle — always shown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.spacing_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.profile_dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.profile_dark_mode_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_medium)))
            Switch(
                checked = isDarkMode,
                onCheckedChange = { onToggleDarkMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)))

        // Coming-soon items
        listOf(
            stringResource(R.string.profile_notifications),
            stringResource(R.string.profile_about)
        ).forEach { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showComingSoon.value = true }
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = dimensionResource(R.dimen.profile_item_row_vertical_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.chevron_right),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding)))
        }

        // Sign Out button — only when authenticated
        if (currentUser != null) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding))
                    .height(dimensionResource(R.dimen.button_height_secondary)),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_corner_radius)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(dimensionResource(R.dimen.card_border_width), MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = stringResource(R.string.btn_sign_out),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenAuthenticatedPreview() {
    X9ComposeTheme(darkTheme = true) {
        ProfileScreen(
            currentUser = User("uid1", "Jane Doe", "jane@example.com"),
            isDarkMode = true,
            onToggleDarkMode = {},
            onNavigateToLogin = {},
            onSignOut = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenUnauthenticatedPreview() {
    X9ComposeTheme(darkTheme = true) {
        ProfileScreen(
            currentUser = null,
            isDarkMode = false,
            onToggleDarkMode = {},
            onNavigateToLogin = {},
            onSignOut = {}
        )
    }
}
