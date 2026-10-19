package com.example.gymlog.ui.muscle

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.gymlog.ui.theme.LocalGymLogDarkTheme
import com.example.gymlog.ui.theme.MuscleTrainingPrimaryDark
import com.example.gymlog.ui.theme.MuscleTrainingPrimaryLight
import com.example.gymlog.ui.theme.MuscleTrainingSecondaryDark
import com.example.gymlog.ui.theme.MuscleTrainingSecondaryLight

@Immutable
internal data class MusclePalette(
    val primary: Color,
    val secondary: Color,
    val idle: Color,
    val selected: Color,
    val label: Color,
    val leader: Color,
    val backdrop: Color,
)

/**
 * Training colors stay red so the existing meaning does not change. Selection
 * uses the app's theme primary (blue) and is therefore distinct from a trained
 * muscle. The dark/light choice follows the app theme, not the system theme.
 */
@Composable
internal fun rememberMusclePalette(): MusclePalette {
    val dark = LocalGymLogDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    return MusclePalette(
        primary = if (dark) MuscleTrainingPrimaryDark else MuscleTrainingPrimaryLight,
        secondary = if (dark) MuscleTrainingSecondaryDark else MuscleTrainingSecondaryLight,
        idle = scheme.onSurfaceVariant.copy(alpha = 0.55f),
        selected = scheme.primary,
        label = scheme.onSurface,
        leader = scheme.onSurfaceVariant,
        backdrop = scheme.surfaceContainerLowest,
    )
}
