package com.example.gymlog.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.ui.exercise.BodyCategoryRail
import com.example.gymlog.ui.exercise.EquipmentCategoryBar
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrainingTrendScreen(
    viewModel: TrainingTrendViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_training_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrainingTrendMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = state.metric == metric,
                        onClick = { viewModel.onMetricSelected(metric) },
                        label = {
                            Text(
                                text = trainingMetricLabel(metric),
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                    )
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BodyCategoryRail(
                    selected = state.bodyPart,
                    onSelect = viewModel::onBodyPartSelected,
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EquipmentCategoryBar(
                        selected = state.equipmentType,
                        onSelect = viewModel::onEquipmentSelected,
                    )
                    Text(
                        text = stringResource(
                            R.string.library_section_count,
                            state.bodyPart.labelZh,
                            state.equipmentType.labelZh,
                            state.exercises.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (state.exercises.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.stats_category_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        } else {
                            items(state.exercises, key = { it.id }) { exercise ->
                                ExerciseChoice(
                                    exercise = exercise,
                                    selected = exercise.id == state.selectedExerciseId,
                                    onClick = { viewModel.onExerciseSelected(exercise.id) },
                                )
                            }
                        }
                    }
                    if (state.selectedExerciseId != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 12.dp, bottom = 12.dp),
                            shape = dashboardCardShape(),
                            colors = dashboardCardColors(),
                            elevation = dashboardCardElevation(),
                            border = dashboardCardBorder(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                TrendChart(
                                    points = state.points,
                                    quantity = TrendQuantity.KILOGRAM,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseChoice(
    exercise: ExerciseEntity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = exercise.trendLabel(),
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun trainingMetricLabel(metric: TrainingTrendMetric): String {
    return stringResource(
        when (metric) {
            TrainingTrendMetric.VOLUME -> R.string.stats_volume
            TrainingTrendMetric.WORKING_PR -> R.string.stats_working_pr
            TrainingTrendMetric.MAX_PR -> R.string.stats_max_pr
        },
    )
}

