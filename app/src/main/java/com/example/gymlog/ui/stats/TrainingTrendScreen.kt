package com.example.gymlog.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.TrendPoint
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.ui.exercise.BodyCategoryRail
import com.example.gymlog.ui.exercise.EquipmentCategoryBar
import com.example.gymlog.ui.theme.GymLogCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTrendScreen(
    viewModel: TrainingTrendViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExercisePickerBar(
                exercise = state.selectedExercise,
                onClick = { pickerOpen = true },
            )
            TrendCard(
                state = state,
                onMetricSelected = viewModel::onMetricSelected,
            )
            StatsCard(stats = state.stats)
            if (state.weeks.any { it.days > 0 }) {
                FrequencyCard(weeks = state.weeks, averageGapDays = state.averageGapDays)
            }
            // 训练记录吃掉剩下的高度，自己在卡内滚动：上面的图和统计始终留在屏幕上。
            HistoryCard(rows = state.history, modifier = Modifier.weight(1f))
        }
    }
    if (pickerOpen) {
        ExercisePickerSheet(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onBodyPartSelected = viewModel::onBodyPartSelected,
            onEquipmentSelected = viewModel::onEquipmentSelected,
            onSelect = { id ->
                viewModel.onExerciseSelected(id)
                viewModel.onQueryChange("")
                pickerOpen = false
            },
            onDismiss = {
                viewModel.onQueryChange("")
                pickerOpen = false
            },
        )
    }
}

/**
 * 一行动作选择条：左边一个小圆点 + 完整动作名（左对齐），右边是部位胶囊和箭头，点开下面的选择面板。
 * 顶部只留部位：完整名里已经带器械，标「部位 · 器械」会把右边的标签撑长。
 */
@Composable
private fun ExercisePickerBar(
    exercise: ExerciseEntity?,
    onClick: () -> Unit,
) {
    val name = exercise?.name ?: stringResource(R.string.stats_pick_exercise)
    val tag = exercise?.bodyLabel().orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 9.dp),
        )
        if (tag.isNotBlank()) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 3.dp)
                .size(16.dp),
        )
    }
}

/**
 * 趋势卡：左上大字是当前指标的数值（点图上的点会跟着变），右上是指标开关。
 * 在卡片上左右滑也能切换指标，滑到头停住不循环。
 */
@Composable
private fun TrendCard(
    state: TrainingTrendUiState,
    onMetricSelected: (TrainingTrendMetric) -> Unit,
) {
    val points = state.points
    val metric = state.metric
    var shown by remember(points) { mutableStateOf<TrendPoint?>(points.lastOrNull()) }
    var shownIsLatest by remember(points) { mutableStateOf(true) }
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val metrics = TrainingTrendMetric.entries
    val index = metrics.indexOf(metric)

    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(metric) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                    onDragEnd = {
                        if (total <= -swipeThreshold) {
                            metrics.getOrNull(index + 1)?.let(onMetricSelected)
                        } else if (total >= swipeThreshold) {
                            metrics.getOrNull(index - 1)?.let(onMetricSelected)
                        }
                    },
                )
            },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metricLabel(metric),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = trendValueText(metric, shown?.value),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
                        )
                    }
                    Text(
                        text = trendCaption(metric, points, shown, shownIsLatest),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                MetricSwitch(selected = metric, onSelect = onMetricSelected)
            }
            TrendChart(
                points = points,
                quantity = TrendQuantity.KILOGRAM,
                modifier = Modifier.padding(top = 4.dp),
                chartHeight = 180.dp,
                showHeader = false,
                onShownChange = { point, latest ->
                    shown = point
                    shownIsLatest = latest
                },
            )
        }
    }
}

@Composable
private fun MetricSwitch(
    selected: TrainingTrendMetric,
    onSelect: (TrainingTrendMetric) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TrainingTrendMetric.entries.forEach { metric ->
            val on = metric == selected
            Text(
                text = metricLabel(metric),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (on) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                        } else {
                            Color.Transparent
                        },
                    )
                    .clickable(role = Role.RadioButton) { onSelect(metric) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun StatsCard(stats: TrendStats) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(
                label = stringResource(R.string.stats_sessions),
                value = if (stats.sessionCount == 0) "—" else stats.sessionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatCell(
                label = stringResource(R.string.stats_total_volume),
                value = if (stats.totalVolumeKg <= 0.0) {
                    "—"
                } else {
                    totalVolumeText(stats.totalVolumeKg)
                },
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatCell(
                label = stringResource(R.string.stats_best_set),
                value = if (stats.bestReps == 0) {
                    "—"
                } else {
                    "${WeightLogic.formatKg(stats.bestWeightKg)}×${stats.bestReps}"
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** 卡片里的小标题：左边名字，右边一行弱化说明。 */
@Composable
private fun SectionHeader(title: String, meta: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** 练习频率：近 12 周每周一格，练得越勤颜色越深。 */
@Composable
private fun FrequencyCard(weeks: List<TrainingWeek>, averageGapDays: Int?) {
    val caption = if (averageGapDays != null) {
        stringResource(R.string.stats_frequency_average, weeks.size, averageGapDays)
    } else {
        stringResource(R.string.stats_frequency_count, weeks.size, weeks.sumOf { it.days })
    }
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            SectionHeader(
                title = stringResource(R.string.stats_frequency),
                meta = caption,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                weeks.forEach { week ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(weekColor(week.days)),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = monthLabel(weeks.firstOrNull()?.startEpochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.stats_this_week),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 0 天留灰底，1 / 2 / 3 天以上依次加深。 */
@Composable
private fun weekColor(days: Int): Color {
    val primary = MaterialTheme.colorScheme.primary
    return when {
        days <= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        days == 1 -> primary.copy(alpha = 0.26f)
        days == 2 -> primary.copy(alpha = 0.5f)
        else -> primary
    }
}

/** 训练记录：一场一行，日期 / 最好一组 / 组数次数 / 当次容量。 */
@Composable
private fun HistoryCard(rows: List<TrendSessionRow>, modifier: Modifier = Modifier) {
    // 最好一组只标一行：并列时标最近那次。
    val best = rows.maxWithOrNull(compareBy({ it.bestWeightKg }, { it.bestReps }))
    val bestSessionId = rows.firstOrNull {
        it.bestWeightKg == best?.bestWeightKg && it.bestReps == best?.bestReps
    }?.sessionId
    GymLogCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            SectionHeader(
                title = stringResource(R.string.stats_history),
                meta = if (rows.isEmpty()) {
                    ""
                } else {
                    stringResource(R.string.stats_history_count, rows.size)
                },
            )
            if (rows.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                // 列表能往下 / 往上滚时，对应一侧盖一层卡片色渐隐，提示「还有内容」，滚到头自动消失。
                val listState = rememberLazyListState()
                val canScrollDown by remember {
                    derivedStateOf { listState.canScrollForward }
                }
                val canScrollUp by remember {
                    derivedStateOf { listState.canScrollBackward }
                }
                val cardColor = MaterialTheme.colorScheme.surfaceContainerLowest
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(rows, key = { _, row -> row.sessionId }) { index, row ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                            HistoryRow(row = row, isBest = row.sessionId == bestSessionId)
                        }
                    }
                    if (canScrollUp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .height(16.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            cardColor,
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }
                    if (canScrollDown) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(34.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.55f to cardColor.copy(alpha = 0.55f),
                                        1f to cardColor,
                                    ),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(row: TrendSessionRow, isBest: Boolean) {
    val date = LocalDate.ofEpochDay(row.dateEpochDay)
    // 日期列跟着系统字号一起放大，否则 130% 以上的字号会和右边的最好一组挤在一起。
    val dateWidth = 68.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(dateWidth)) {
            Text(
                text = date.format(historyDateFormatter),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = weekdayLabels[date.dayOfWeek.value - 1],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = WeightLogic.formatKg(row.bestWeightKg),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = " kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = " × ${row.bestReps}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                if (isBest) {
                    Text(
                        text = stringResource(R.string.stats_history_best),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.stats_history_sets,
                    row.setCount,
                    row.totalReps,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%,d", row.volumeKg.roundToLong()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = " kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.stats_volume),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private val historyDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月", Locale.CHINA)

private val weekdayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

private fun monthLabel(epochDay: Long?): String {
    return epochDay?.let { LocalDate.ofEpochDay(it).format(monthFormatter) }.orEmpty()
}

/**
 * 动作选择面板：布局照动作库——上方搜索框，下面左侧部位竖排、右侧器械横排 + 结果列表。
 * 不输入时只列最近练过的；输入后连没练过的也会出现（列表里标「未练过」）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    state: TrainingTrendUiState,
    onQueryChange: (String) -> Unit,
    onBodyPartSelected: (ExerciseClassification.BodyCategory) -> Unit,
    onEquipmentSelected: (ExerciseClassification.EquipmentCategory) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.86f)
                .padding(bottom = 8.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.stats_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp),
            ) {
                BodyCategoryRail(
                    selected = state.bodyPart,
                    onSelect = onBodyPartSelected,
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EquipmentCategoryBar(
                        selected = state.equipmentType,
                        onSelect = onEquipmentSelected,
                    )
                    Text(
                        text = if (state.query.isBlank()) {
                            stringResource(R.string.stats_recent)
                        } else {
                            stringResource(R.string.stats_search_result)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                    )
                    if (state.exercises.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_no_match),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                        ) {
                            items(state.exercises, key = { it.id }) { exercise ->
                                ExercisePickerRow(
                                    exercise = exercise,
                                    selected = exercise.id == state.selectedExerciseId,
                                    trained = exercise.id in state.trainedExerciseIds,
                                    onClick = { onSelect(exercise.id) },
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
private fun ExercisePickerRow(
    exercise: ExerciseEntity,
    selected: Boolean,
    trained: Boolean,
    onClick: () -> Unit,
) {
    val base = "${exercise.bodyLabel()} · ${exercise.equipmentLabel()}"
    val tag = if (trained) base else "$base · ${stringResource(R.string.stats_untrained)}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun metricLabel(metric: TrainingTrendMetric): String {
    return stringResource(
        when (metric) {
            TrainingTrendMetric.VOLUME -> R.string.stats_volume
            TrainingTrendMetric.WORKING_PR -> R.string.stats_working_pr
            TrainingTrendMetric.MAX_PR -> R.string.stats_max_pr
        },
    )
}

private fun ExerciseEntity.bodyLabel(): String {
    return ExerciseClassification.body(bodyCategoryId)?.labelZh
        ?: bodyPartDetail?.takeIf { it.isNotBlank() }
        ?: "未分类"
}

private fun ExerciseEntity.equipmentLabel(): String {
    return ExerciseClassification.equipment(equipmentCategoryId)?.labelZh
        ?: equipmentDetail?.takeIf { it.isNotBlank() }
        ?: "未分类"
}

/** 容量带千分位且单位另画，所以去掉 [WeightLogic.displayVolume] 自带的「 kg」。 */
private fun trendValueText(metric: TrainingTrendMetric, value: Double?): String {
    if (value == null) return "—"
    return if (metric == TrainingTrendMetric.VOLUME) {
        WeightLogic.displayVolume(value).removeSuffix(" kg")
    } else {
        WeightLogic.formatKg(value)
    }
}

private val captionDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

/** 大字下面那行：最新一次显示相对上次的变化，点了历史点就显示那天的日期。 */
private fun trendCaption(
    metric: TrainingTrendMetric,
    points: List<TrendPoint>,
    shown: TrendPoint?,
    shownIsLatest: Boolean,
): String {
    val date = shown?.let { LocalDate.ofEpochDay(it.dateEpochDay).format(captionDateFormatter) }
        ?: return ""
    if (!shownIsLatest) return date
    val previous = points.getOrNull(points.lastIndex - 1) ?: return date
    val current = points.lastOrNull() ?: return date
    return when {
        metric == TrainingTrendMetric.VOLUME && previous.value > 0.0 -> {
            val percent = (current.value - previous.value) / previous.value * 100.0
            val sign = if (percent >= 0) "+" else ""
            "较上次 $sign${String.format(Locale.US, "%.1f", percent)}%"
        }
        metric != TrainingTrendMetric.VOLUME -> {
            val delta = current.value - previous.value
            if (delta == 0.0) "与上次持平" else "较上次 ${if (delta > 0) "+" else ""}${WeightLogic.formatKg(delta)} kg"
        }
        else -> date
    }
}

/** 总容量：过万用 t，否则原样加千分位，保证三格统计里不会被截断。 */
private fun totalVolumeText(volumeKg: Double): String {
    return if (volumeKg >= 10_000) {
        val tons = Math.round(volumeKg / 100.0) / 10.0
        "$tons t"
    } else {
        "${String.format(Locale.US, "%,d", volumeKg.roundToLong())} kg"
    }
}
