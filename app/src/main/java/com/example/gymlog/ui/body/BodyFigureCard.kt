package com.example.gymlog.ui.body

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.BodyGirths
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.girths
import com.example.gymlog.ui.muscle.GirthCalloutOverlay
import com.example.gymlog.ui.muscle.MuscleFigure
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape

private enum class FigureMode {
    BODY,
    MUSCLE,
}

private val FigureHeight = 460.dp

@Composable
fun BodyFigureCard(
    state: BodyRecordUiState,
    highlights: List<MuscleHighlight>,
    onEditGirths: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(FigureMode.BODY) }
    var selectedRegionId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(highlights) { selectedRegionId = null }
    val girths = state.history.firstOrNull()?.girths() ?: BodyGirths()
    val selectedName = selectedRegionId?.let { MuscleCatalog.specificMuscleForRegion(it)?.nameZh }
    val showMuscle = mode == FigureMode.MUSCLE
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !showMuscle,
                    onClick = {
                        mode = FigureMode.BODY
                        selectedRegionId = null
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.girth_mode_body),
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                FilterChip(
                    selected = showMuscle,
                    onClick = {
                        mode = FigureMode.MUSCLE
                        selectedRegionId = null
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.girth_mode_muscle),
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onEditGirths,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.girth_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                MuscleFigure(
                    highlights = if (showMuscle) highlights else emptyList(),
                    selectedRegionId = if (showMuscle) selectedRegionId else null,
                    onRegionClick = {
                        if (showMuscle) {
                            selectedRegionId = if (selectedRegionId == it) null else it
                        }
                    },
                    boundedHeight = FigureHeight,
                    showCallouts = false,
                    lockFront = !showMuscle,
                    showSwitch = showMuscle,
                    switchBelow = true,
                    onViewSideChange = { selectedRegionId = null },
                    onBackgroundClick = { selectedRegionId = null },
                )
                if (!showMuscle) {
                    GirthCalloutOverlay(
                        girths = girths,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            if (showMuscle && selectedName != null) {
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
