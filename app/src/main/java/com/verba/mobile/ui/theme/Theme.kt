package com.verba.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = VerbaPrimary,
    onPrimary = VerbaOnPrimary,
    surface = VerbaSurface,
    onSurface = VerbaOnSurface,
    surfaceVariant = VerbaSurfaceVariant,
    onSurfaceVariant = VerbaOnSurfaceVariant,
    error = VerbaError,
)

private val DarkColors = darkColorScheme(
    primary = VerbaDarkPrimary,
    onPrimary = VerbaDarkOnPrimary,
    surface = VerbaDarkSurface,
    onSurface = VerbaDarkOnSurface,
    surfaceVariant = VerbaDarkSurfaceVariant,
    onSurfaceVariant = VerbaDarkOnSurfaceVariant,
    error = VerbaDarkError,
)

@Composable
fun VerbaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VerbaTypography,
        content = content,
    )
}
