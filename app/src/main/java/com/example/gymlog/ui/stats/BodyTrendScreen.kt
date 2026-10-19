package com.example.gymlog.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BodyTrendScreen(
    viewModel: BodyTrendViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_body_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BodyTrendMetric.entries.forEach { metric ->
                        FilterChip(
                            selected = state.metric == metric,
                            onClick = { viewModel.onMetricSelected(metric) },
                            label = { Text(bodyMetricLabel(metric)) },
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = dashboardCardShape(),
                    colors = dashboardCardColors(),
                    elevation = dashboardCardElevation(),
                    border = dashboardCardBorder(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TrendChart(
                            points = state.points,
                            quantity = state.metric.quantity(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun bodyMetricLabel(metric: BodyTrendMetric): String {
    return stringResource(
        when (metric) {
            BodyTrendMetric.WEIGHT -> R.string.stats_weight
            BodyTrendMetric.FAT -> R.string.stats_fat
            BodyTrendMetric.BMI -> R.string.stats_bmi
        },
    )
}

