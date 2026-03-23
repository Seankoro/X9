package dk.itu.moapd.x9.s25134.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import dk.itu.moapd.x9.s25134.R

// Think of it like a global CSS-variables file
@Composable
fun X9ComposeTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.x9_primary_dark),
            onPrimary = colorResource(R.color.x9_on_primary_dark),
            primaryContainer = colorResource(R.color.x9_primary_container_dark),
            onPrimaryContainer = colorResource(R.color.x9_on_primary_container_dark),
            secondary = colorResource(R.color.x9_secondary_dark),
            onSecondary = colorResource(R.color.x9_on_secondary_dark),
            secondaryContainer = colorResource(R.color.x9_secondary_container_dark),
            onSecondaryContainer = colorResource(R.color.x9_on_secondary_container_dark),
            background = colorResource(R.color.x9_background_dark),
            onBackground = colorResource(R.color.x9_on_background_dark),
            surface = colorResource(R.color.x9_surface_dark),
            onSurface = colorResource(R.color.x9_on_surface_dark),
            surfaceVariant = colorResource(R.color.x9_surface_variant_dark),
            onSurfaceVariant = colorResource(R.color.x9_on_surface_variant_dark),
            outline = colorResource(R.color.x9_outline_dark),
            outlineVariant = colorResource(R.color.x9_outline_variant_dark),
            error = colorResource(R.color.x9_error),
            onError = colorResource(R.color.x9_on_error)
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.x9_primary),
            onPrimary = colorResource(R.color.x9_on_primary),
            primaryContainer = colorResource(R.color.x9_primary_container),
            onPrimaryContainer = colorResource(R.color.x9_on_primary_container),
            secondary = colorResource(R.color.x9_secondary),
            onSecondary = colorResource(R.color.x9_on_secondary),
            secondaryContainer = colorResource(R.color.x9_secondary_container),
            onSecondaryContainer = colorResource(R.color.x9_on_secondary_container),
            background = colorResource(R.color.x9_background),
            onBackground = colorResource(R.color.x9_on_background),
            surface = colorResource(R.color.x9_surface),
            onSurface = colorResource(R.color.x9_on_surface),
            surfaceVariant = colorResource(R.color.x9_surface_variant),
            onSurfaceVariant = colorResource(R.color.x9_on_surface_variant),
            outline = colorResource(R.color.x9_outline),
            outlineVariant = colorResource(R.color.x9_outline_variant),
            error = colorResource(R.color.x9_error),
            onError = colorResource(R.color.x9_on_error)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}