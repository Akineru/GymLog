package com.example.gymlog.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHeatLevel
import com.example.gymlog.data.MuscleHeatRange
import com.example.gymlog.data.MuscleHeatStat
import com.example.gymlog.data.MuscleRecoveryInfo
import com.example.gymlog.data.MuscleRecoveryState
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.ui.muscle.MuscleFigure
import com.example.gymlog.ui.muscle.muscleKey
import com.example.gymlog.ui.muscle.rememberMuscleSex
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import com.example.gymlog.ui.theme.LocalGymLogDarkTheme
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("M月d日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuscleHeatmapCard(
    state: MuscleHeatUiState,
    onRange: (MuscleHeatRange) -> Unit,
) {
    val sex = rememberMuscleSex()
    val dark = LocalGymLogDarkTheme.current
    val recoveryMode = state.range == MuscleHeatRange.RECOVERY
    val colors = remember(state.stats, state.recovery, recoveryMode, dark) {
        if (recoveryMode) {
            state.recovery.mapNotNull { (id, info) ->
                recoveryColor(info.state, dark)?.let { id to it }
            }.toMap()
        } else {
            state.stats.mapNotNull { (id, stat) ->
                heatColor(stat.level, dark)?.let { id to it }
            }.toMap()
        }
    }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.muscle_heat_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                HeatHelpButton(onClick = { showHelp = true })
            }
            HeatRangeSwitch(selected = state.range, onRange = onRange)
            Text(
                text = if (recoveryMode) {
                    stringResource(R.string.muscle_heat_recovery_asof, state.end.format(dayFormat))
                } else {
                    stringResource(
                        R.string.muscle_heat_range,
                        state.start.format(dayFormat),
                        state.end.format(dayFormat),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MuscleFigure(
                    highlights = emptyList(),
                    onRegionClick = { selectedId = muscleKey(it) },
                    sex = sex,
                    boundedHeight = 280.dp,
                    showCallouts = false,
                    forcedFront = true,
                    showSwitch = false,
                    heatColors = colors,
                    modifier = Modifier.weight(1f),
                )
                MuscleFigure(
                    highlights = emptyList(),
                    onRegionClick = { selectedId = muscleKey(it) },
                    sex = sex,
                    boundedHeight = 280.dp,
                    showCallouts = false,
                    forcedFront = false,
                    showSwitch = false,
                    heatColors = colors,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.muscle_heat_help)) },
            text = {
                Text(
                    stringResource(
                        if (recoveryMode) {
                            R.string.muscle_heat_help_recovery_body
                        } else {
                            R.string.muscle_heat_help_body
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.training_preferences_help_got_it))
                }
            },
        )
    }
    val muscle = selectedId?.let { MuscleCatalog.find(it) }
    if (muscle != null) {
        MuscleHeatSheet(
            name = muscle.nameZh,
            range = state.range,
            stat = state.stats[muscle.id],
            recovery = state.recovery[muscle.id],
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
private fun HeatRangeSwitch(
    selected: MuscleHeatRange,
    onRange: (MuscleHeatRange) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
    ) {
        listOf(
            MuscleHeatRange.WEEK to R.string.muscle_heat_week,
            MuscleHeatRange.MONTH to R.string.muscle_heat_month,
            MuscleHeatRange.RECOVERY to R.string.muscle_heat_recovery,
        ).forEach { (value, label) ->
            val active = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    // 和首页周月切换一致的浅色滑块：白色底 + 品牌蓝文字，不用实心蓝
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable { onRange(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun HeatHelpButton(onClick: () -> Unit) {
    val description = stringResource(R.string.muscle_heat_help)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleHeatSheet(
    name: String,
    range: MuscleHeatRange,
    stat: MuscleHeatStat?,
    recovery: MuscleRecoveryInfo?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rangeLabel = stringResource(
        when (range) {
            MuscleHeatRange.WEEK -> R.string.muscle_heat_week
            MuscleHeatRange.MONTH -> R.string.muscle_heat_month
            MuscleHeatRange.RECOVERY -> R.string.muscle_heat_recovery
        },
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = name, style = MaterialTheme.typography.titleLarge)
            if (recovery != null) {
                RecoverySummary(info = recovery)
            } else {
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (stat == null || stat.sessionCount == 0) {
                Text(
                    text = stringResource(R.string.muscle_heat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                if (recovery == null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HeatFact(stringResource(R.string.muscle_heat_sessions), stat.sessionCount.toString())
                        HeatFact(stringResource(R.string.muscle_heat_exercise_count), stat.exercises.size.toString())
                        HeatFact(stringResource(R.string.muscle_heat_sets), stat.setCount.toString())
                    }
                    val last = stat.lastDateEpochDay
                    if (last != null) {
                        Text(
                            text = stringResource(R.string.muscle_heat_last) + "  " +
                                java.time.LocalDate.ofEpochDay(last).format(dayFormat),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.muscle_heat_actions),
                    style = MaterialTheme.typography.titleSmall,
                )
                stat.exercises.forEach { exercise ->
                    Text(
                        text = exercise.name + "  ·  " + stringResource(
                            if (exercise.role == MuscleRole.PRIMARY) {
                                R.string.muscle_primary
                            } else {
                                R.string.muscle_secondary
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** 恢复模式下底部详情的头部：状态 + 上次训练 + 预计恢复。 */
@Composable
private fun RecoverySummary(info: MuscleRecoveryInfo) {
    val dark = LocalGymLogDarkTheme.current
    val color = recoveryColor(info.state, dark)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = stringResource(recoveryStateLabel(info.state)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = stringResource(R.string.muscle_recovery_last) + "  " +
                java.time.LocalDate.ofEpochDay(info.lastDateEpochDay).format(dayFormat),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.muscle_recovery_expected) + "  " +
                java.time.LocalDate.ofEpochDay(info.expectedRecoveryEpochDay).format(dayFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun recoveryStateLabel(state: MuscleRecoveryState): Int = when (state) {
    MuscleRecoveryState.RECOVERING -> R.string.muscle_recovery_state_recovering
    MuscleRecoveryState.RECOVERED -> R.string.muscle_recovery_state_recovered
    MuscleRecoveryState.STALE -> R.string.muscle_recovery_state_stale
}

@Composable
private fun HeatFact(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 恢复三态配色：浅绿=恢复中，深绿=已恢复，灰=久未训练。
 * 和训练热力的红色系不同屏出现，只在恢复段使用。
 */
internal fun recoveryColor(state: MuscleRecoveryState, dark: Boolean): Color {
    val rgb = when (state) {
        MuscleRecoveryState.RECOVERING -> if (dark) 0xFFC8E6C9L else 0xFFA5D6A7L
        MuscleRecoveryState.RECOVERED -> if (dark) 0xFF66BB6AL else 0xFF2E7D32L
        MuscleRecoveryState.STALE -> if (dark) 0xFFAEB6C4L else 0xFF7C8698L
    }
    val alpha = when (state) {
        MuscleRecoveryState.RECOVERING -> 0.85f
        MuscleRecoveryState.RECOVERED -> 1f
        MuscleRecoveryState.STALE -> 0.85f
    }
    return Color(rgb).copy(alpha = alpha)
}

internal fun heatColor(level: MuscleHeatLevel, dark: Boolean): Color? {
    val rgb = when (level) {
        MuscleHeatLevel.NONE -> return null
        MuscleHeatLevel.LOW -> if (dark) 0xFFF0B4AEL else 0xFFF4B7B1L
        MuscleHeatLevel.FAIR -> if (dark) 0xFFE07A76L else 0xFFE57373L
        MuscleHeatLevel.MEDIUM -> if (dark) 0xFFD4534FL else 0xFFD64545L
        MuscleHeatLevel.HIGH -> if (dark) 0xFFC13A36L else 0xFFB42318L
        MuscleHeatLevel.PEAK -> if (dark) 0xFF9E241FL else 0xFF7F1D1DL
    }
    val alpha = when (level) {
        MuscleHeatLevel.NONE -> 0f
        MuscleHeatLevel.LOW -> 0.78f
        MuscleHeatLevel.FAIR -> 0.84f
        MuscleHeatLevel.MEDIUM -> 0.90f
        MuscleHeatLevel.HIGH -> 0.94f
        MuscleHeatLevel.PEAK -> 1f
    }
    return Color(rgb).copy(alpha = alpha)
}
