package eu.monniot.speed.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GreenLightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GreenTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    error = GreenError,
    errorContainer = GreenErrorContainer,
    onErrorContainer = GreenOnErrorContainer,
    surface = GreenSurface,
    surfaceDim = GreenSurfaceDim,
    surfaceContainerLow = GreenSurfaceContainerLow,
    surfaceContainer = GreenSurfaceContainer,
    surfaceContainerHigh = GreenSurfaceContainerHigh,
    surfaceContainerHighest = GreenSurfaceContainerHighest,
    onSurface = GreenOnSurface,
    onSurfaceVariant = GreenOnSurfaceVariant,
    outline = GreenOutline,
    outlineVariant = GreenOutlineVariant,
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = GreenDarkOnPrimary,
    primaryContainer = GreenDarkPrimaryContainer,
    onPrimaryContainer = GreenDarkOnPrimaryContainer,
    secondary = GreenDarkSecondary,
    secondaryContainer = GreenDarkSecondaryContainer,
    onSecondaryContainer = GreenDarkOnSecondaryContainer,
    tertiary = GreenDarkTertiary,
    tertiaryContainer = GreenDarkTertiaryContainer,
    onTertiaryContainer = GreenDarkOnTertiaryContainer,
    error = GreenDarkError,
    errorContainer = GreenDarkErrorContainer,
    onErrorContainer = GreenDarkOnErrorContainer,
    surface = GreenDarkSurface,
    surfaceContainerLow = GreenDarkSurfaceContainerLow,
    surfaceContainer = GreenDarkSurfaceContainer,
    surfaceContainerHigh = GreenDarkSurfaceContainerHigh,
    surfaceContainerHighest = GreenDarkSurfaceContainerHighest,
    onSurface = GreenDarkOnSurface,
    onSurfaceVariant = GreenDarkOnSurfaceVariant,
    outline = GreenDarkOutline,
    outlineVariant = GreenDarkOutlineVariant,
)

// No dynamic color: always green light or dark, driven by explicit darkTheme param.
// MainActivity passes isSystemInDarkTheme() as a temporary source until C3 wires DataStore.
@Composable
fun RaceLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeedTypography,
        shapes = SpeedShapes,
        content = content,
    )
}
