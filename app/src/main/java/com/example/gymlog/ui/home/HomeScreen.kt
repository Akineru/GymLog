package com.example.gymlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "subtitle") {
                Text(
                    text = stringResource(
                        R.string.home_overview_subtitle,
                        selectedDate.format(yearMonthFormatter),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(
                        text = note ?: stringResource(R.string.empty_day),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                items(dayGroups, key = { it.partKey }) { group ->
                    PartCard(group = group, onOpenSession = onOpenSession)
                }
            }
            item(key = "shortcuts") {
                ShortcutCard(
                    onStartTraining = onStartTraining,
                    onOpenBody = onOpenBody,
                    onOpenTrends = onOpenTrends,
                )
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
) {
    val durationLabel = when {
        mode == HomeRangeMode.WEEK && showingCurrentPeriod -> R.string.home_duration_week
        mode == HomeRangeMode.MONTH && showingCurrentPeriod -> R.string.home_duration_month
        else -> R.string.home_duration_other
    }
    val jumpLabel = if (mode == HomeRangeMode.WEEK) R.string.home_this_week else R.string.home_this_month
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = dashboardCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(durationLabel),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ModeChip(
                        label = stringResource(R.string.home_mode_week),
                        selected = mode == HomeRangeMode.WEEK,
                        onClick = { onMode(HomeRangeMode.WEEK) },
                    )
                    ModeChip(
                        label = stringResource(R.string.home_mode_month),
                        selected = mode == HomeRangeMode.MONTH,
                        onClick = { onMode(HomeRangeMode.MONTH) },
                    )
                }
            }
            Text(
                text = TrainingTimerLogic.formatDurationChinese(summary.durationMillis),
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.home_date_range,
                        rangeStart.format(monthDayFormatter),
                        rangeEnd.format(monthDayFormatter),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { onShift(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.home_previous_period),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(jumpLabel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onJumpToToday),
                )
                IconButton(onClick = { onShift(true) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.home_next_period),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth()) {
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
                    value = WeightLogic.displayVolume(summary.volumeKg),
                    unit = "",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun OverviewStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
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
                Text(
                    text = stringResource(R.string.home_open_calendar),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenCalendar),
                )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    today -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
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
        timers.forEach { saved ->
            Text(
                text = stringResource(
                    R.string.timer_day_duration,
                    TrainingTimerLogic.formatDurationChinese(saved.record.durationMillis),
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSavedTimer(saved.record.id) },
            )
        }
    }
}

@Composable
private fun PartCard(
    group: HomePartGroup,
    onOpenSession: (String) -> Unit,
) {
    val setCount = group.exercises.sumOf { it.setCount }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun ShortcutCard(
    onStartTraining: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenTrends: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.home_shortcuts),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Shortcut(
                    icon = Icons.Filled.PlayArrow,
                    label = stringResource(R.string.home_start_training),
                    onClick = onStartTraining,
                    modifier = Modifier.weight(1f),
                )
                Shortcut(
                    icon = Icons.Filled.Accessibility,
                    label = stringResource(R.string.home_record_body),
                    onClick = onOpenBody,
                    modifier = Modifier.weight(1f),
                )
                Shortcut(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = stringResource(R.string.home_open_trends),
                    onClick = onOpenTrends,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Shortcut(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
