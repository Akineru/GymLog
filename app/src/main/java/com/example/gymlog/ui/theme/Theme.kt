package com.example.gymlog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueInk,
    secondary = Color(0xFF4C6285),
    onSecondary = Color.White,
    secondaryContainer = BrandBlueSoft,
    onSecondaryContainer = BrandBlueInk,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessSoft,
    onTertiaryContainer = Color(0xFF1C5C3A),
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Color(0xFF8C2424),
    background = PageBackground,
    onBackground = Ink,
    surface = PageBackground,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF2F8),
    onSurfaceVariant = InkSecondary,
    outline = Line,
    outlineVariant = Line,
    surfaceTint = BrandBlue,
    surfaceContainerLowest = CardWhite,
    surfaceContainerLow = CardWhite,
    surfaceContainer = CardWhite,
    surfaceContainerHigh = CardWhite,
    surfaceContainerHighest = CardWhite,
    inverseSurface = Ink,
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF9DB7FF),
    scrim = Color(0x99000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8EADFF),
    onPrimary = Color(0xFF08245C),
    primaryContainer = Color(0xFF1C3F86),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFB7C4DA),
    onSecondary = Color(0xFF1C2838),
    secondaryContainer = Color(0xFF24344C),
    onSecondaryContainer = Color(0xFFDCE6FF),
    tertiary = Color(0xFF8ED4AE),
    error = Color(0xFFFFB4AB),
    background = DashboardDark,
    onBackground = DashboardDarkInk,
    surface = DashboardDark,
    onSurface = DashboardDarkInk,
    surfaceVariant = Color(0xFF242B38),
    onSurfaceVariant = DashboardDarkMuted,
    outline = Color(0xFF2C3444),
    outlineVariant = Color(0xFF2C3444),
    surfaceContainerLowest = Color(0xFF0C1016),
    surfaceContainerLow = DashboardDarkCard,
    surfaceContainer = DashboardDarkCard,
    surfaceContainerHigh = Color(0xFF1E2531),
    surfaceContainerHighest = Color(0xFF242B38),
)

val LocalGymLogDarkTheme = staticCompositionLocalOf { false }

val DashboardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GymLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalGymLogDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DashboardShapes,
            content = content,
        )
    }
}
