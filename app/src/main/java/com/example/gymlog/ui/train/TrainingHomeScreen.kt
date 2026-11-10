package com.example.gymlog.ui.train

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.gymlog.data.MuscleHeatRange
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.ui.progress.MuscleHeatUiState
import com.example.gymlog.ui.progress.MuscleHeatmapCard
import com.example.gymlog.ui.settings.SettingsTopBarAction
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import com.example.gymlog.ui.theme.LiquidGlassCard
import com.example.gymlog.ui.theme.ceramicSurface
import com.example.gymlog.ui.theme.LiquidGlassColors

data class ActiveWorkoutUi(
    val durationMillis: Long,
    val currentExerciseName: String?,
    val timerText: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHomeScreen(
    hasActiveWorkout: Boolean,
    heatState: MuscleHeatUiState,
    onHeatRange: (MuscleHeatRange) -> Unit,
    onStartTraining: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_train)) },
                actions = { SettingsTopBarAction(onOpenSettings) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                // 肌群热力图从统计页挪到这里，紧跟在「今天练什么？」下面
                item(key = "muscle-heat") {
                    MuscleHeatmapCard(state = heatState, onRange = onHeatRange)
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
            Text(
                text = stringResource(R.string.training_timer_tool),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = GymLogSpacing.sm),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 悬浮在内容之上的「锻炼中」卡片。
 *
 * - 展开态：整张卡片点一下就进训练；右侧垃圾桶是取消训练，箭头是收起。
 * - 收起态：缩成右下角一颗只显示时长的小胶囊，点一下重新展开。
 */
@Composable
fun ActiveWorkoutMiniBar(
    workout: ActiveWorkoutUi,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = collapsed,
        animationSpec = tween(durationMillis = 180),
        label = "active-workout-bar",
        modifier = modifier.fillMaxWidth(),
    ) { isCollapsed ->
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (isCollapsed) {
                CollapsedActiveWorkoutPill(
                    durationMillis = workout.durationMillis,
                    onClick = onToggleCollapse,
                )
            } else {
                ExpandedActiveWorkoutCard(
                    workout = workout,
                    onResume = onResume,
                    onDiscard = onDiscard,
                    onCollapse = onToggleCollapse,
                )
            }
        }
    }
}

@Composable
private fun CollapsedActiveWorkoutPill(
    durationMillis: Long,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .ceramicSurface(RoundedCornerShape(999.dp), floatingElevation = 10.dp)
            .clickable(
                onClickLabel = stringResource(R.string.training_expand),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF40B98A)),
        )
        Text(
            text = TrainingTimerLogic.formatDurationChinese(durationMillis),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.Medium,
            ),
            color = LiquidGlassColors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExpandedActiveWorkoutCard(
    workout: ActiveWorkoutUi,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onCollapse: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ceramicSurface(RoundedCornerShape(20.dp), floatingElevation = 12.dp)
            .clickable(onClick = onResume)
            .padding(start = GymLogSpacing.md, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF40B98A)),
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
                    color = LiquidGlassColors.ink,
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
                            tint = LiquidGlassColors.inkSoft,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = timer,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Medium,
                            ),
                            color = LiquidGlassColors.ink,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                text = workout.currentExerciseName
                    ?: stringResource(R.string.training_active_next_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LiquidGlassColors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDiscard, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.training_discard),
                tint = LiquidGlassColors.inkSoft,
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onCollapse, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.training_collapse),
                tint = LiquidGlassColors.inkMid,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun StartTrainingCard(onStartTraining: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.train_hero_title),
                style = MaterialTheme.typography.headlineSmall,
                color = LiquidGlassColors.inkMid,
            )
            GymLogActionButton(
                text = stringResource(R.string.home_start_training),
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow,
            )
        }
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
            Text(
                text = stringResource(R.string.exercise_library),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = GymLogSpacing.sm),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
