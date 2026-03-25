package dk.itu.moapd.x9.s25134.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dk.itu.moapd.x9.s25134.R
import dk.itu.moapd.x9.s25134.ui.components.ScreenHeader
import dk.itu.moapd.x9.s25134.ui.theme.X9ComposeTheme

@Composable
fun ProfileScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
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

        // Dark mode toggle — functional
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = 16.dp),
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
            Spacer(modifier = Modifier.width(16.dp))
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
            stringResource(R.string.profile_account),
            stringResource(R.string.profile_notifications),
            stringResource(R.string.profile_about)
        ).forEach { label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showComingSoon.value = true }
                    .padding(horizontal = dimensionResource(R.dimen.screen_horizontal_padding), vertical = 18.dp),
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
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    X9ComposeTheme(darkTheme = true) {
        ProfileScreen(isDarkMode = true, onToggleDarkMode = {})
    }
}
