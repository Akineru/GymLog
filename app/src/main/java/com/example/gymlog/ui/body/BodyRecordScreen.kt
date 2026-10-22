package com.example.gymlog.ui.body

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyMetrics
import com.example.gymlog.data.Gender
import com.example.gymlog.data.TrendLogic
import com.example.gymlog.data.TrendPoint
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.ui.stats.BodyTrendMetric
import com.example.gymlog.ui.stats.TrendChart
import com.example.gymlog.ui.stats.quantity
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyRecordScreen(
    viewModel: BodyRecordViewModel,
    muscleHighlights: List<com.example.gymlog.data.MuscleHighlight>,
    onBack: () -> Unit,
    onOpenMuscleMap: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var editingGirths by remember { mutableStateOf(false) }
    var showingHistory by remember { mutableStateOf(false) }
    when {
        showingHistory -> BodyRecordHistory(
            viewModel = viewModel,
            onBack = { showingHistory = false },
        )
        editing -> BodyRecordEditor(
            viewModel = viewModel,
            onBack = { editing = false },
            onSaved = { editing = false },
            onOpenHistory = { showingHistory = true },
        )
        editingGirths -> GirthEditor(
            viewModel = viewModel,
            onBack = { editingGirths = false },
            onSaved = { editingGirths = false },
        )
        else -> BodyRecordOverview(
            viewModel = viewModel,
            muscleHighlights = muscleHighlights,
            onBack = onBack,
            onOpenMuscleMap = onOpenMuscleMap,
            onOpenHistory = { showingHistory = true },
            onEdit = {
                viewModel.clearSaved()
                editing = true
            },
            onEditGirths = {
                viewModel.clearSaved()
                editingGirths = true
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyRecordOverview(
    viewModel: BodyRecordViewModel,
    muscleHighlights: List<com.example.gymlog.data.MuscleHighlight>,
    onBack: () -> Unit,
    onOpenMuscleMap: () -> Unit,
    onOpenHistory: () -> Unit,
    onEdit: () -> Unit,
    onEditGirths: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var trendMetric by remember { mutableStateOf(BodyTrendMetric.WEIGHT) }
    val trendPoints = when (trendMetric) {
        BodyTrendMetric.WEIGHT -> TrendLogic.weightSeries(state.history)
        BodyTrendMetric.FAT -> TrendLogic.bodyFatSeries(state.history)
        BodyTrendMetric.BMI -> TrendLogic.bmiSeries(state.history)
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_record)) },
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
                CurrentBodyCard(state = state, onEdit = onEdit)
            }
            item {
                BodyFigureCard(
                    state = state,
                    highlights = muscleHighlights,
                    onEditGirths = onEditGirths,
                )
            }
            item {
                BodyTrendCard(
                    metric = trendMetric,
                    points = trendPoints,
                    onMetricSelected = { trendMetric = it },
                )
            }
            item {
                HistoryEntry(count = state.history.size, onClick = onOpenHistory)
            }
            item {
                GymLogCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMuscleMap),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GymLogSpacing.md, vertical = GymLogSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Accessibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = GymLogSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.muscle_map_entry),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.muscle_map_entry_hint),
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
        }
    }
}

@Composable
private fun CurrentBodyCard(
    state: BodyRecordUiState,
    onEdit: () -> Unit,
) {
    val metrics = currentMetrics(state)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 40.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "当前身体",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFBBD0EC),
            )
            if (metrics.isEmpty()) {
                Text(
                    text = stringResource(R.string.body_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBBD0EC),
                )
            } else {
                metrics.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { metric ->
                            MetricCell(metric, Modifier.weight(1f))
                        }
                        if (row.size == 1) {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp)
                .size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.body_edit),
                tint = Color(0xFFBBD0EC),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private enum class ChangeTone {
    DOWN,
    UP,
    FLAT,
}

private data class MetricChange(
    val text: String,
    val tone: ChangeTone,
)

private data class ShownMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val unit: String,
    val change: MetricChange?,
)

@Composable
private fun currentMetrics(state: BodyRecordUiState): List<ShownMetric> {
    val previous = state.history.getOrNull(1)
    val metrics = ArrayList<ShownMetric>()
    state.weightText.takeIf { it.isNotBlank() }?.let {
        metrics += ShownMetric(
            Icons.Filled.MonitorWeight,
            stringResource(R.string.stats_weight),
            it,
            "kg",
            metricChange(WeightLogic.parseKg(it), previous?.weightKg, "kg"),
        )
    }
    state.bodyFatText.takeIf { it.isNotBlank() }?.let {
        metrics += ShownMetric(
            Icons.Filled.Percent,
            stringResource(R.string.stats_fat),
            it,
            "%",
            metricChange(WeightLogic.parseKg(it), previous?.bodyFatPercent, "%"),
        )
    }
    state.heightText.takeIf { it.isNotBlank() }?.let {
        metrics += ShownMetric(
            Icons.Filled.Height,
            stringResource(R.string.body_label_height),
            it,
            "cm",
            metricChange(WeightLogic.parseKg(it), previous?.heightCm, "cm"),
        )
    }
    state.bmiText?.let {
        val previousBmi = BodyMetrics.bmi(previous?.heightCm, previous?.weightKg)
        metrics += ShownMetric(
            Icons.Filled.MonitorWeight,
            stringResource(R.string.stats_bmi),
            it,
            "",
            metricChange(it.toDoubleOrNull(), previousBmi, ""),
        )
    }
    return metrics
}

private fun metricChange(current: Double?, previous: Double?, unit: String): MetricChange? {
    if (current == null || previous == null) {
        return MetricChange("暂无对比", ChangeTone.FLAT)
    }
    val diff = current - previous
    if (kotlin.math.abs(diff) < 0.05) {
        return MetricChange("— 与上次持平", ChangeTone.FLAT)
    }
    val value = String.format(Locale.US, "%.1f", kotlin.math.abs(diff))
    val suffix = if (unit.isBlank()) "" else " $unit"
    return if (diff < 0) {
        MetricChange("↓ 较上次 -$value$suffix", ChangeTone.DOWN)
    } else {
        MetricChange("↑ 较上次 +$value$suffix", ChangeTone.UP)
    }
}

@Composable
private fun MetricCell(metric: ShownMetric, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = metric.icon,
            contentDescription = null,
            tint = Color(0xFFBBD0EC),
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metric.unit.isNotBlank()) {
                    Text(
                        text = metric.unit,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                }
            }
            metric.change?.let { change ->
                Text(
                    text = change.text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    color = when (change.tone) {
                        ChangeTone.DOWN -> Color(0xFF7BE0A5)
                        ChangeTone.UP -> Color(0xFFFFB4A2)
                        ChangeTone.FLAT -> Color(0xFF9FB4CC)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BodyTrendCard(
    metric: BodyTrendMetric,
    points: List<TrendPoint>,
    onMetricSelected: (BodyTrendMetric) -> Unit,
) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.body_trend_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyTrendMetric.entries.forEach { item ->
                    FilterChip(
                        selected = metric == item,
                        onClick = { onMetricSelected(item) },
                        label = {
                            Text(
                                text = stringResource(
                                    when (item) {
                                        BodyTrendMetric.WEIGHT -> R.string.stats_weight
                                        BodyTrendMetric.FAT -> R.string.stats_fat
                                        BodyTrendMetric.BMI -> R.string.stats_bmi
                                    },
                                ),
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                    )
                }
            }
            TrendChart(
                points = points,
                quantity = metric.quantity(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenderChips(
    selected: Gender?,
    onSelect: (Gender?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Gender.entries.forEach { gender ->
            FilterChip(
                selected = selected == gender,
                onClick = { onSelect(if (selected == gender) null else gender) },
                label = { Text(genderLabel(gender)) },
            )
        }
    }
}

@Composable
private fun genderLabel(gender: Gender): String {
    return when (gender) {
        Gender.MALE -> stringResource(R.string.gender_male)
        Gender.FEMALE -> stringResource(R.string.gender_female)
        Gender.OTHER -> stringResource(R.string.gender_other)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyRecordEditor(
    viewModel: BodyRecordViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingBirthday by remember { mutableStateOf(false) }
    var watchedSave by remember { mutableStateOf(false) }
    BackHandler(enabled = pickingBirthday) { pickingBirthday = false }
    BackHandler(enabled = !pickingBirthday, onBack = onBack)

    LaunchedEffect(state.saved) {
        if (!watchedSave) {
            watchedSave = true
            viewModel.clearSaved()
            return@LaunchedEffect
        }
        if (state.saved) onSaved()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save) {
                        Text(stringResource(R.string.save))
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
                OutlinedTextField(
                    value = state.birthdayEpochDay
                        ?.let { LocalDate.ofEpochDay(it).format(dateFormatter) }
                        .orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_birthday)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { pickingBirthday = true }) {
                    Text(stringResource(R.string.pick_birthday))
                }
                Text(
                    text = state.ageYears?.let { stringResource(R.string.body_age, it) }
                        ?: stringResource(R.string.body_age_unknown),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                Text(stringResource(R.string.field_gender), style = MaterialTheme.typography.labelLarge)
                GenderChips(
                    selected = state.gender,
                    onSelect = viewModel::onGenderChange,
                )
            }
            item {
                OutlinedTextField(
                    value = state.heightText,
                    onValueChange = viewModel::onHeightChange,
                    label = { Text(stringResource(R.string.field_height)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.weightText,
                    onValueChange = viewModel::onWeightChange,
                    label = { Text(stringResource(R.string.field_body_weight)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    text = state.bmiText?.let { stringResource(R.string.body_bmi, it) }
                        ?: stringResource(R.string.body_bmi_unknown),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                OutlinedTextField(
                    value = state.bodyFatText,
                    onValueChange = viewModel::onBodyFatChange,
                    label = { Text(stringResource(R.string.field_body_fat)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.errorMessage?.let { code ->
                item {
                    Text(
                        text = messageFor(code),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.saved) {
                item {
                    Text(
                        text = stringResource(R.string.body_saved),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                HistoryEntry(count = state.history.size, onClick = onOpenHistory)
            }
        }
    }

    if (pickingBirthday) {
        BodyDatePicker(
            epochDay = state.birthdayEpochDay ?: LocalDate.of(1998, 1, 1).toEpochDay(),
            onDismiss = { pickingBirthday = false },
            onConfirm = {
                viewModel.onBirthdayChange(it)
                pickingBirthday = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GirthEditor(
    viewModel: BodyRecordViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var watchedSave by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LaunchedEffect(state.saved) {
        if (!watchedSave) {
            watchedSave = true
            viewModel.clearSaved()
            return@LaunchedEffect
        }
        if (state.saved) onSaved()
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.girth_edit)) },
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GirthPair(
                label = stringResource(R.string.body_label_arm),
                left = state.armLeftText,
                right = state.armRightText,
                onLeft = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.ARM_LEFT, it) },
                onRight = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.ARM_RIGHT, it) },
            )
            GirthPair(
                label = stringResource(R.string.girth_forearm),
                left = state.forearmLeftText,
                right = state.forearmRightText,
                onLeft = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.FOREARM_LEFT, it) },
                onRight = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.FOREARM_RIGHT, it) },
            )
            GirthSingle(
                label = stringResource(R.string.girth_chest),
                value = state.chestText,
                onValue = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.CHEST, it) },
            )
            GirthSingle(
                label = stringResource(R.string.body_label_waist),
                value = state.waistText,
                onValue = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.WAIST, it) },
            )
            GirthSingle(
                label = stringResource(R.string.girth_hip),
                value = state.hipText,
                onValue = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.HIP, it) },
            )
            GirthPair(
                label = stringResource(R.string.girth_thigh),
                left = state.thighLeftText,
                right = state.thighRightText,
                onLeft = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.THIGH_LEFT, it) },
                onRight = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.THIGH_RIGHT, it) },
            )
            GirthPair(
                label = stringResource(R.string.girth_calf),
                left = state.calfLeftText,
                right = state.calfRightText,
                onLeft = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.CALF_LEFT, it) },
                onRight = { viewModel.onGirthChange(BodyRecordViewModel.GirthField.CALF_RIGHT, it) },
            )
            state.errorMessage?.let { code ->
                Text(
                    text = if (code == "GIRTH_INVALID") {
                        stringResource(R.string.girth_invalid)
                    } else {
                        code
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = viewModel::saveGirths,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.girth_save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GirthPair(
    label: String,
    left: String,
    right: String,
    onLeft: (String) -> Unit,
    onRight: (String) -> Unit,
) {
    Text(text = label, style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GirthField(
            label = stringResource(R.string.girth_left),
            value = left,
            onValue = onLeft,
            modifier = Modifier.weight(1f),
        )
        GirthField(
            label = stringResource(R.string.girth_right),
            value = right,
            onValue = onRight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GirthSingle(
    label: String,
    value: String,
    onValue: (String) -> Unit,
) {
    GirthField(label = label, value = value, onValue = onValue, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun GirthField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        suffix = { Text(stringResource(R.string.unit_cm)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BodyRecordHistory(
    viewModel: BodyRecordViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expandedId by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_history_title)) },
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
        if (state.history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.body_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(GymLogSpacing.md),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.history, key = { it.id }) { row ->
                    MeasurementCard(
                        row = row,
                        expanded = row.id == expandedId,
                        onToggle = {
                            expandedId = if (expandedId == row.id) null else row.id
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntry(
    count: Int,
    onClick: () -> Unit,
) {
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymLogSpacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = GymLogSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.body_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (count == 0) {
                        stringResource(R.string.body_history_empty)
                    } else {
                        stringResource(R.string.body_history_count, count)
                    },
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
private fun GirthHistoryLine(labelRes: Int, value: Double?) {
    value ?: return
    Text(stringResource(R.string.girth_single, stringResource(labelRes), WeightLogic.formatKg(value)))
}

@Composable
private fun GirthHistoryPair(labelRes: Int, left: Double?, right: Double?) {
    if (left == null && right == null) return
    val missing = stringResource(R.string.mine_value_empty)
    Text(
        stringResource(
            R.string.girth_pair,
            stringResource(labelRes),
            left?.let(WeightLogic::formatKg) ?: missing,
            right?.let(WeightLogic::formatKg) ?: missing,
        ),
    )
}

@Composable
private fun MeasurementCard(
    row: BodyMeasurementEntity,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Instant.ofEpochMilli(row.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(dateTimeFormatter),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (expanded) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                row.heightCm?.let {
                    Text(stringResource(R.string.body_history_height, WeightLogic.formatKg(it)))
                }
                row.weightKg?.let {
                    Text(stringResource(R.string.body_history_weight, WeightLogic.formatKg(it)))
                }
                row.waistCm?.let {
                    Text(stringResource(R.string.body_history_waist, WeightLogic.formatKg(it)))
                }
                GirthHistoryLine(R.string.girth_chest, row.chestCm)
                GirthHistoryLine(R.string.girth_hip, row.hipCm)
                GirthHistoryPair(R.string.body_label_arm, row.armLeftCm, row.armRightCm)
                GirthHistoryPair(R.string.girth_forearm, row.forearmLeftCm, row.forearmRightCm)
                GirthHistoryPair(R.string.girth_thigh, row.thighLeftCm, row.thighRightCm)
                GirthHistoryPair(R.string.girth_calf, row.calfLeftCm, row.calfRightCm)
                BodyMetrics.bmi(row.heightCm, row.weightKg)?.let { bmi ->
                    Text(stringResource(R.string.body_bmi, BodyMetrics.displayBmi(bmi)))
                }
                row.bodyFatPercent?.let {
                    Text(stringResource(R.string.body_history_fat, WeightLogic.formatKg(it)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyDatePicker(
    epochDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.ofEpochDay(epochDay)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val day = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .toEpochDay()
                        onConfirm(day)
                    } ?: onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun messageFor(code: String): String {
    return when (code) {
        "BIRTHDAY_INVALID" -> "生日不能晚于今天"
        "HEIGHT_INVALID" -> "请填写有效身高"
        "WEIGHT_INVALID" -> "请填写有效体重"
        "BODY_FAT_INVALID" -> "请填写有效体脂"
        "WAIST_INVALID" -> "请填写有效腰围"
        "ARM_INVALID" -> "请填写有效臂围"
        else -> "保存失败，请检查填写内容"
    }
}
