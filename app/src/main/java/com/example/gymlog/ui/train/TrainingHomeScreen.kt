package com.example.gymlog.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.ui.settings.SettingsTopBarAction
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing

data class ActiveWorkoutUi(
    val durationMillis: Long,
    val currentExerciseName: String?,
    val timerText: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHomeScreen(
    hasActiveWorkout: Boolean,
    onStartTraining: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_train)) },
                actions = { SettingsTopBarAction(onOpenSettings) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = GymLogSpacing.md,
                end = GymLogSpacing.md,
                top = GymLogSpacing.sm,
                bottom = GymLogSpacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            if (!hasActiveWorkout) {
                item(key = "start") {
                    StartTrainingCard(onStartTraining = onStartTraining)
                }
            }
            item(key = "library") {
                LibraryCard(onOpenLibrary = onOpenLibrary)
            }
            item(key = "timer-tool") {
                TrainingTimerToolCard(onOpenTimer = onOpenTimer)
            }
        }
    }
}

@Composable
private fun TrainingTimerToolCard(onOpenTimer: () -> Unit) {
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTimer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymLogSpacing.md, vertical = GymLogSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = GymLogSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = stringResource(R.string.training_timer_tool),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.training_timer_tool_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ActiveWorkoutMiniBar(
    workout: ActiveWorkoutUi,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                ),
            )
            .clickable(onClick = onResume)
            .padding(start = GymLogSpacing.md, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF6EE7A8)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = GymLogSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildString {
                        append(stringResource(R.string.training_active_workout))
                        append(" · ")
                        append(TrainingTimerLogic.formatDurationChinese(workout.durationMillis))
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.Normal,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                workout.timerText?.let { timer ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = timer,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                text = workout.currentExerciseName
                    ?: stringResource(R.string.training_active_next_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBBD0EC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDiscard, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.training_discard),
                tint = Color(0xFFBBD0EC),
                modifier = Modifier.size(18.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.training_resume),
            tint = Color.White,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp),
        )
    }
}

@Composable
private fun StartTrainingCard(onStartTraining: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.train_hero_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFBBD0EC),
        )
        Text(
            text = stringResource(R.string.train_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD6E4F5),
        )
        GymLogActionButton(
            text = stringResource(R.string.home_start_training),
            onClick = onStartTraining,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.PlayArrow,
        )
    }
}

@Composable
private fun LibraryCard(onOpenLibrary: () -> Unit) {
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenLibrary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymLogSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = GymLogSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.exercise_library),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.train_library_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
