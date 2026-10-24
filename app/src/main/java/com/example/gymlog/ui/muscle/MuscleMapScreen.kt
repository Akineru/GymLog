package com.example.gymlog.ui.muscle

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.Muscle
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleExerciseRef
import com.example.gymlog.data.MuscleMapLogic
import com.example.gymlog.data.MuscleRegion
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

private enum class RoleFilter {
    ALL,
    PRIMARY,
    SECONDARY,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MuscleMapScreen(
    viewModel: MuscleMapViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var roleFilter by remember { mutableStateOf(RoleFilter.ALL) }
    var selectedRegionId by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    BackHandler(enabled = !pickingDate, onBack = onBack)

    val visible = remember(state.highlights, roleFilter) {
        state.highlights.filter { highlight ->
            when (roleFilter) {
                RoleFilter.ALL -> true
                RoleFilter.PRIMARY -> highlight.role == MuscleRole.PRIMARY
                RoleFilter.SECONDARY -> highlight.role == MuscleRole.SECONDARY
            }
        }
    }
    val regionId = selectedRegionId
    val selectedMuscle = regionId?.let { MuscleCatalog.specificMuscleForRegion(it) }
    val selectedExercises = regionId?.let { id ->
        MuscleMapLogic.exercisesForRegion(id, visible)
            .groupBy { it.exerciseId }
            .map { (_, refs) ->
                val role = if (refs.any { it.role == MuscleRole.PRIMARY }) MuscleRole.PRIMARY else MuscleRole.SECONDARY
                refs.first().copy(role = role)
            }
    }.orEmpty()
    val selectedRole = selectedMuscle?.let { muscle ->
        visible.find { it.muscleId == muscle.id }?.role
            ?: visible.find { highlight ->
                regionId in MuscleCatalog.find(highlight.muscleId)?.paintRegionIds.orEmpty()
            }?.role
    }
    val unsupported = visible.mapNotNull { highlight ->
        MuscleCatalog.find(highlight.muscleId)?.takeIf { !it.visualSupported }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.muscle_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val sideBySide = maxWidth >= 1040.dp
            if (sideBySide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1.35f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item {
                            MapControls(
                                dateText = state.date.format(dateFormatter),
                                trainedCount = state.highlights.size,
                                range = state.range,
                                roleFilter = roleFilter,
                                onPrevious = {
                                    selectedRegionId = null
                                    viewModel.shiftDate(-1)
                                },
                                onNext = {
                                    selectedRegionId = null
                                    viewModel.shiftDate(1)
                                },
                                onPickDate = { pickingDate = true },
                                onRange = { selectedRegionId = null; viewModel.setRange(it) },
                                onRole = { selectedRegionId = null; roleFilter = it },
                            )
                        }
                        item {
                            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                                MuscleFigure(
                                    highlights = visible,
                                    selectedRegionId = selectedRegionId,
                                    onRegionClick = { selectedRegionId = if (selectedRegionId == it) null else it },
                                    onBackgroundClick = { selectedRegionId = null },
                                    onViewSideChange = { selectedRegionId = null },
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                        if (!state.hasSessions || unsupported.isNotEmpty()) {
                            item {
                                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        StatusCopy(
                                            hasSessions = state.hasSessions,
                                            unsupported = unsupported,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MuscleDetailCard(
                        muscle = selectedMuscle,
                        role = selectedRole,
                        exercises = selectedExercises,
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        MapControls(
                            dateText = state.date.format(dateFormatter),
                            trainedCount = state.highlights.size,
                            range = state.range,
                            roleFilter = roleFilter,
                            onPrevious = {
                                selectedRegionId = null
                                viewModel.shiftDate(-1)
                            },
                            onNext = {
                                selectedRegionId = null
                                viewModel.shiftDate(1)
                            },
                            onPickDate = { pickingDate = true },
                            onRange = { selectedRegionId = null; viewModel.setRange(it) },
                            onRole = { selectedRegionId = null; roleFilter = it },
                        )
                    }
                    item {
                        GymLogCard(modifier = Modifier.fillMaxWidth()) {
                            MuscleFigure(
                                highlights = visible,
                                selectedRegionId = selectedRegionId,
                                onRegionClick = { selectedRegionId = if (selectedRegionId == it) null else it },
                                onBackgroundClick = { selectedRegionId = null },
                                onViewSideChange = { selectedRegionId = null },
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                    if (!state.hasSessions || unsupported.isNotEmpty()) {
                        item {
                            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    StatusCopy(
                                        hasSessions = state.hasSessions,
                                        unsupported = unsupported,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        MuscleDetailCard(
                            muscle = selectedMuscle,
                            role = selectedRole,
                            exercises = selectedExercises,
                        )
                    }
                }
            }
        }
    }

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            shape = RoundedCornerShape(24.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            selectedRegionId = null
                            viewModel.setDate(picked)
                        }
                        pickingDate = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapControls(
    dateText: String,
    trainedCount: Int,
    range: MuscleMapRange,
    roleFilter: RoleFilter,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onRange: (MuscleMapRange) -> Unit,
    onRole: (RoleFilter) -> Unit,
) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.muscle_prev_day))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = onPickDate) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.muscle_trained_count, trainedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.muscle_next_day))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "范围",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = range == MuscleMapRange.DAY,
                    onClick = { onRange(MuscleMapRange.DAY) },
                    label = { Text(stringResource(R.string.muscle_scope_day)) },
                )
                FilterChip(
                    selected = range == MuscleMapRange.WEEK,
                    onClick = { onRange(MuscleMapRange.WEEK) },
                    label = { Text(stringResource(R.string.muscle_scope_week)) },
                )
                FilterChip(
                    selected = range == MuscleMapRange.MONTH,
                    onClick = { onRange(MuscleMapRange.MONTH) },
                    label = { Text(stringResource(R.string.muscle_scope_month)) },
                )
            }
            Text(
                text = "角色",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = roleFilter == RoleFilter.ALL,
                    onClick = { onRole(RoleFilter.ALL) },
                    label = { Text(stringResource(R.string.muscle_filter_all)) },
                )
                FilterChip(
                    selected = roleFilter == RoleFilter.PRIMARY,
                    onClick = { onRole(RoleFilter.PRIMARY) },
                    label = { Text(stringResource(R.string.muscle_primary)) },
                )
                FilterChip(
                    selected = roleFilter == RoleFilter.SECONDARY,
                    onClick = { onRole(RoleFilter.SECONDARY) },
                    label = { Text(stringResource(R.string.muscle_secondary)) },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LegendRow()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendRow() {
    val palette = rememberMusclePalette()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LegendItem(palette.primary, stringResource(R.string.muscle_legend_primary))
            LegendItem(palette.secondary, stringResource(R.string.muscle_legend_secondary))
            LegendItem(palette.selected, stringResource(R.string.muscle_legend_selected))
            LegendItem(palette.idle, stringResource(R.string.muscle_legend_idle))
        }
        Text(
            text = stringResource(R.string.muscle_legend),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatusCopy(hasSessions: Boolean, unsupported: List<Muscle>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!hasSessions) {
            Text(
                text = stringResource(R.string.muscle_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (unsupported.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.muscle_unsupported_list,
                    unsupported.joinToString("、") { it.nameZh },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MuscleDetailCard(
    muscle: Muscle?,
    role: MuscleRole?,
    exercises: List<MuscleExerciseRef>,
    modifier: Modifier = Modifier,
) {
    val palette = rememberMusclePalette()
    GymLogCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (muscle == null) {
                Text(
                    text = stringResource(R.string.muscle_pick_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(muscle.nameZh, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Normal)
                Text(
                    muscle.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.muscle_region_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(regionName(muscle.region), style = MaterialTheme.typography.bodyLarge)
                RoleBadge(role, palette)
                Text(
                    stringResource(R.string.muscle_related),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (exercises.isEmpty()) {
                    Text(
                        stringResource(R.string.muscle_region_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    exercises.forEach { exercise ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            RoleBadge(exercise.role, palette)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(role: MuscleRole?, palette: MusclePalette) {
    val (label, color) = when (role) {
        MuscleRole.PRIMARY -> stringResource(R.string.muscle_primary) to palette.primary
        MuscleRole.SECONDARY -> stringResource(R.string.muscle_secondary) to palette.secondary
        null -> stringResource(R.string.muscle_idle) to palette.idle
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.22f)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun regionName(region: MuscleRegion): String {
    return when (region) {
        MuscleRegion.CHEST -> stringResource(R.string.muscle_part_chest)
        MuscleRegion.SHOULDERS -> stringResource(R.string.muscle_part_shoulders)
        MuscleRegion.BACK -> stringResource(R.string.muscle_part_back)
        MuscleRegion.ARMS -> stringResource(R.string.muscle_part_arms)
        MuscleRegion.CORE -> stringResource(R.string.muscle_part_core)
        MuscleRegion.GLUTES -> stringResource(R.string.muscle_part_glutes)
        MuscleRegion.LEGS -> stringResource(R.string.muscle_part_legs)
        MuscleRegion.CALVES -> stringResource(R.string.muscle_part_calves)
    }
}
