package com.example.gymlog.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog.R
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.TimerRecordWithLaps
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.ui.settings.SettingsTopBarAction
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogNumberText
import com.example.gymlog.ui.theme.GymLogSecondaryButton
import com.example.gymlog.ui.theme.GymLogSpacing

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val yearMonthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

private val monthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

private val fullDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessions: List<SessionDetail>,
    measurements: List<BodyMeasurementEntity>,
    savedTimers: List<TimerRecordWithLaps>,
    dayNotes: Map<Long, String>,
    onStartTraining: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenSavedTimer: (String) -> Unit,
    onOpenBody: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenCalendar: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedDate = state.selectedDate
    val today = remember { LocalDate.now() }
    val range = remember(state.mode, selectedDate) {
        HomePeriod.statsRange(state.mode, selectedDate)
    }
    val summary = remember(sessions, measurements, savedTimers, range) {
        HomePeriod.summarize(
            sessions = sessions,
            timers = savedTimers.map { it.record },
            measurements = measurements,
            start = range.first,
            endInclusive = range.second,
        )
    }
    val weekStart = remember(selectedDate) { HomePeriod.weekStart(selectedDate) }
    val trainedDays = remember(sessions, weekStart) {
        HomePeriod.trainedEpochDays(sessions, weekStart, weekStart.plusDays(6))
    }
    val dayGroups = remember(sessions, measurements, selectedDate) {
        HomePeriod.dayGroups(sessions, measurements, selectedDate)
    }
    val dayTimers = remember(savedTimers, selectedDate) {
        savedTimers.filter { it.record.dateEpochDay == selectedDate.toEpochDay() }
    }
    val currentPeriod = remember(state.mode, today) {
        HomePeriod.statsRange(state.mode, today)
    }
    val showingCurrentPeriod = range == currentPeriod

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = { SettingsTopBarAction(onOpenSettings) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 4.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "subtitle") {
                Text(
                    text = stringResource(
                        R.string.home_overview_subtitle,
                        selectedDate.format(yearMonthFormatter),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            item(key = "overview") {
                OverviewCard(
                    mode = state.mode,
                    showingCurrentPeriod = showingCurrentPeriod,
                    rangeStart = range.first,
                    rangeEnd = range.second,
                    summary = summary,
                    onMode = viewModel::setMode,
                    onShift = viewModel::shift,
                    onJumpToToday = viewModel::jumpToToday,
                    onStartTraining = onStartTraining,
                )
            }
            item(key = "week") {
                WeekCard(
                    weekStart = weekStart,
                    selectedDate = selectedDate,
                    today = today,
                    trainedDays = trainedDays,
                    onSelect = viewModel::selectDate,
                    onOpenCalendar = { onOpenCalendar(selectedDate) },
                )
            }
            item(key = "day-header") {
                DayHeader(
                    date = selectedDate,
                    timers = dayTimers,
                    onOpenSavedTimer = onOpenSavedTimer,
                )
            }
            if (dayGroups.isEmpty()) {
                item(key = "day-empty") {
                    val note = dayNotes[selectedDate.toEpochDay()]?.takeIf { it.isNotBlank() }
                    EmptyDayCard(
                        isToday = selectedDate == today,
                        note = note,
                        onOpenBody = onOpenBody,
                        onOpenTrends = onOpenTrends,
                    )
                }
            } else {
                items(dayGroups, key = { it.partKey }) { group ->
                    PartCard(group = group, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    mode: HomeRangeMode,
    showingCurrentPeriod: Boolean,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    summary: HomeSummary,
    onMode: (HomeRangeMode) -> Unit,
    onShift: (Boolean) -> Unit,
    onJumpToToday: () -> Unit,
    onStartTraining: () -> Unit,
) {
    val durationLabel = when {
        mode == HomeRangeMode.WEEK && showingCurrentPeriod -> R.string.home_duration_week
        mode == HomeRangeMode.MONTH && showingCurrentPeriod -> R.string.home_duration_month
        else -> R.string.home_duration_other
    }
    var volumeInTons by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                ),
            )
            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(durationLabel),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFBBD0EC),
                    modifier = Modifier.weight(1f),
                )
                RangeModeSwitch(mode = mode, onMode = onMode)
            }
            GymLogNumberText(
                text = TrainingTimerLogic.formatDurationChinese(summary.durationMillis),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onShift(false) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.home_previous_period),
                        tint = Color(0xFF8AB4FF),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.home_date_range,
                        rangeStart.format(monthDayFormatter),
                        rangeEnd.format(monthDayFormatter),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFBBD0EC),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onJumpToToday)
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                IconButton(
                    onClick = { onShift(true) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.home_next_period),
                        tint = Color(0xFF8AB4FF),
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverviewStat(
                    label = stringResource(R.string.home_stat_days),
                    value = summary.trainingDays.toString(),
                    unit = stringResource(R.string.home_unit_day),
                    modifier = Modifier.weight(1f),
                )
                OverviewStat(
                    label = stringResource(R.string.home_stat_sets),
                    value = summary.setCount.toString(),
                    unit = stringResource(R.string.home_unit_set),
                    modifier = Modifier.weight(1f),
                )
                OverviewStat(
                    label = stringResource(R.string.home_stat_volume),
                    value = if (volumeInTons) {
                        String.format(Locale.US, "%.1f", summary.volumeKg / 1_000.0)
                    } else {
                        String.format(Locale.US, "%,.0f", summary.volumeKg)
                    },
                    unit = stringResource(
                        if (volumeInTons) R.string.home_unit_t else R.string.home_unit_kg,
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = { volumeInTons = !volumeInTons },
                )
            }
            GymLogActionButton(
                text = stringResource(R.string.home_start_training),
                onClick = onStartTraining,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Filled.PlayArrow,
            )
    }
}

@Composable
private fun RangeModeSwitch(
    mode: HomeRangeMode,
    onMode: (HomeRangeMode) -> Unit,
) {
    val weekSelected = mode == HomeRangeMode.WEEK
    val indicatorOffset by animateDpAsState(
        targetValue = if (weekSelected) 0.dp else 36.dp,
        animationSpec = tween(durationMillis = 220),
        label = "range-mode-indicator",
    )
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = indicatorOffset)
                .width(36.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            listOf(
                stringResource(R.string.home_mode_week) to HomeRangeMode.WEEK,
                stringResource(R.string.home_mode_month) to HomeRangeMode.MONTH,
            ).forEach { (label, value) ->
                val selected = mode == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onMode(value) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else Color(0xFFBBD0EC),
                        fontWeight = if (selected) FontWeight.Normal else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBBD0EC),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFBBD0EC),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekCard(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    trainedDays: Set<Long>,
    onSelect: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val weekdays = stringArrayResource(R.array.home_weekdays)
    val currentWeek = weekStart == HomePeriod.weekStart(today)
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (currentWeek) {
                        stringResource(R.string.home_week_footprint)
                    } else {
                        stringResource(
                            R.string.home_date_range,
                            weekStart.format(monthDayFormatter),
                            weekStart.plusDays(6).format(monthDayFormatter),
                        )
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenCalendar)
                        .padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_open_calendar),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { index ->
                    val date = weekStart.plusDays(index.toLong())
                    WeekDay(
                        label = weekdays.getOrElse(index) { "" },
                        date = date,
                        selected = date == selectedDate,
                        today = date == today,
                        trained = trainedDays.contains(date.toEpochDay()),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDay(
    label: String,
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    trained: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .then(
                    if (today && !selected) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    if (trained) MaterialTheme.colorScheme.primary else Color.Transparent,
                ),
        )
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    timers: List<TimerRecordWithLaps>,
    onOpenSavedTimer: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = date.format(fullDateFormatter),
            style = MaterialTheme.typography.titleMedium,
        )
        val totalDayDuration = timers.sumOf { it.record.durationMillis }
        if (totalDayDuration > 0L) {
            val dayDurationLabel = stringResource(R.string.timer_day_duration_label)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(dayDurationLabel)
                    }
                    append(TrainingTimerLogic.formatDurationChinese(totalDayDuration))
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (timers.size == 1) {
                            Modifier.clickable { onOpenSavedTimer(timers.first().record.id) }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun EmptyDayCard(
    isToday: Boolean,
    note: String?,
    onOpenBody: () -> Unit,
    onOpenTrends: () -> Unit,
) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isToday) R.string.home_empty_title_today else R.string.home_empty_title_other,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = note ?: stringResource(R.string.home_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
            ) {
                GymLogSecondaryButton(
                    text = stringResource(R.string.home_record_body),
                    onClick = onOpenBody,
                    modifier = Modifier.weight(1f),
                )
                GymLogSecondaryButton(
                    text = stringResource(R.string.home_open_trends),
                    onClick = onOpenTrends,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PartCard(
    group: HomePartGroup,
    onOpenSession: (String) -> Unit,
) {
    val setCount = group.exercises.sumOf { it.setCount }
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.home_part_training, group.label),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.home_part_summary,
                    group.exercises.size,
                    setCount,
                    WeightLogic.displayVolume(group.volumeKg),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            group.exercises.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { exercise ->
                        ExerciseTile(
                            exercise = exercise,
                            onOpenSession = onOpenSession,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseTile(
    exercise: HomeExerciseRow,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onOpenSession(exercise.sessionId) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = exercise.name.ifBlank { stringResource(R.string.empty_session_exercises) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.home_exercise_sets, exercise.setCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = WeightLogic.displayVolume(exercise.volumeKg),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
