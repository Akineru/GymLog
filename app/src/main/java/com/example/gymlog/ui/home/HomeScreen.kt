package com.example.gymlog.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog.R
import com.example.gymlog.ui.theme.appMeshBackground
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.TimerRecordWithLaps
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.data.WeightLogic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

private val dayTitleFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日 E", Locale.CHINA)

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
    val singleSessionId = remember(dayGroups) {
        dayGroups.asSequence()
            .flatMap { group -> group.exercises.asSequence().map { it.sessionId } }
            .distinct()
            .toList()
            .singleOrNull()
    }
    val colors = homeColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .appMeshBackground(),
    ) {
        HomeHeader(today = today, onOpenSettings = onOpenSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = HomeDims.screenPadding,
                top = 2.dp,
                end = HomeDims.screenPadding,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(HomeDims.cardGap),
        ) {
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
                    today = today,
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
                        timers = dayTimers,
                        onOpenBody = onOpenBody,
                        onOpenTrends = onOpenTrends,
                    )
                }
            } else {
                item(key = "day-summary") {
                    DaySummaryCard(
                        timers = dayTimers,
                        groups = dayGroups,
                        singleSessionId = singleSessionId,
                        onOpenSession = onOpenSession,
                    )
                }
                items(dayGroups, key = { it.partKey }) { group ->
                    PartCard(group = group, onOpenSession = onOpenSession)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 顶部

@Composable
private fun HomeHeader(
    today: LocalDate,
    onOpenSettings: () -> Unit,
) {
    val colors = homeColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 25.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp,
                ),
                color = colors.ink,
                maxLines = 1,
            )
            Text(
                text = today.format(dayTitleFormatter),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                // 这行直接压在页面背景上，用次级色而不是辅助色，保证对比度
                color = colors.inkSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .frost(colors, RoundedCornerShape(11.dp), strong = true),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 概览卡

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
    val colors = homeColors()
    val durationLabel = when {
        mode == HomeRangeMode.WEEK && showingCurrentPeriod -> R.string.home_duration_week
        mode == HomeRangeMode.MONTH && showingCurrentPeriod -> R.string.home_duration_month
        else -> R.string.home_duration_other
    }
    var volumeInTons by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ceramic(colors, CeramicStyle.Overview)
            .padding(HomeDims.cardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(durationLabel),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                color = colors.inkSecondary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            RangeModeSwitch(mode = mode, onMode = onMode)
        }

        DurationHero(durationMillis = summary.durationMillis)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodNavButton(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.home_previous_period),
                onClick = { onShift(false) },
            )
            Text(
                text = stringResource(
                    R.string.home_date_range,
                    rangeStart.format(monthDayFormatter),
                    rangeEnd.format(monthDayFormatter),
                ),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onJumpToToday)
                    .padding(vertical = 6.dp),
            )
            PeriodNavButton(
                icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.home_next_period),
                onClick = { onShift(true) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(1.dp)
                .background(colors.hair),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                label = stringResource(R.string.home_stat_days),
                value = summary.trainingDays.toString(),
                unit = stringResource(R.string.home_unit_day),
                modifier = Modifier.weight(STAT_WEIGHT_NARROW),
            )
            ColumnDivider()
            StatColumn(
                label = stringResource(R.string.home_stat_sets),
                value = summary.setCount.toString(),
                unit = stringResource(R.string.home_unit_set),
                modifier = Modifier.weight(STAT_WEIGHT_NARROW),
            )
            ColumnDivider()
            StatColumn(
                label = stringResource(R.string.home_stat_volume),
                value = if (volumeInTons) {
                    String.format(Locale.US, "%.1f", summary.volumeKg / 1_000.0)
                } else {
                    String.format(Locale.US, "%,.0f", summary.volumeKg)
                },
                unit = stringResource(
                    if (volumeInTons) R.string.home_unit_t else R.string.home_unit_kg,
                ),
                modifier = Modifier.weight(STAT_WEIGHT_WIDE),
                onClick = { volumeInTons = !volumeInTons },
            )
        }

        StartTrainingButton(onClick = onStartTraining, brand = colors.brand, onBrand = colors.onBrand)
    }
}

/** 训练时长拆成"数字 + 单位"两段，数字重、单位轻。 */
private data class DurationParts(
    val primary: String,
    val primaryUnit: String,
    val secondary: String?,
    val secondaryUnit: String?,
)

private fun durationParts(millis: Long): DurationParts {
    val safeMillis = millis.coerceAtLeast(0L)
    val totalMinutes = safeMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = safeMillis / 1_000L
    return when {
        hours > 0 -> DurationParts(
            primary = hours.toString(),
            primaryUnit = "小时",
            secondary = minutes.toString().padStart(2, '0'),
            secondaryUnit = "分",
        )
        minutes > 0 -> DurationParts(minutes.toString(), "分", null, null)
        // 不足 1 分钟按秒显示，和日期标题行的总时长口径一致，避免出现"0 分"却写着 12 秒
        seconds > 0 -> DurationParts(seconds.toString(), "秒", null, null)
        else -> DurationParts("0", "分", null, null)
    }
}

@Composable
private fun DurationHero(durationMillis: Long) {
    val colors = homeColors()
    val parts = remember(durationMillis) { durationParts(durationMillis) }
    Row(
        modifier = Modifier.padding(top = 9.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 两个数字共用同一套字号 / 字重 / 字距，不再一大一小、也不挤压字形。
        val numberStyle = MaterialTheme.typography.titleLarge.copy(
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        )
        Text(
            text = parts.primary,
            style = numberStyle,
            color = colors.ink,
            maxLines = 1,
        )
        Text(
            text = parts.primaryUnit,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
            color = colors.inkSecondary,
            modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
        )
        if (parts.secondary != null && parts.secondaryUnit != null) {
            Text(
                text = parts.secondary,
                style = numberStyle,
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
            Text(
                text = parts.secondaryUnit,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                color = colors.inkSecondary,
                modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
            )
        }
    }
}

@Composable
private fun PeriodNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = homeColors()
    Box(
        modifier = Modifier
            .size(31.dp)
            .frost(colors, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.brand,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun RangeModeSwitch(
    mode: HomeRangeMode,
    onMode: (HomeRangeMode) -> Unit,
) {
    val colors = homeColors()
    val weekSelected = mode == HomeRangeMode.WEEK
    val shape = RoundedCornerShape(HomeDims.chipRadius)
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(31.dp)
            .frost(colors, shape)
            .padding(3.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            SegmentCell(
                label = stringResource(R.string.home_mode_week),
                selected = weekSelected,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = { onMode(HomeRangeMode.WEEK) },
            )
            SegmentCell(
                label = stringResource(R.string.home_mode_month),
                selected = !weekSelected,
                colors = colors,
                modifier = Modifier.weight(1f),
                onClick = { onMode(HomeRangeMode.MONTH) },
            )
        }
    }
}

@Composable
private fun SegmentCell(
    label: String,
    selected: Boolean,
    colors: HomeColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "segment-bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) colors.brand else colors.inkSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "segment-text",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = if (selected) 2.dp else 0.dp,
                shape = RoundedCornerShape(HomeDims.chipRadius),
                clip = false,
                ambientColor = Color(0x14202B40),
                spotColor = Color(0x1F202B40),
            )
            .clip(RoundedCornerShape(HomeDims.chipRadius))
            .background(background)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = textColor,
        )
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = homeColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
            color = colors.inkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = statValueFontSize(value, 21.sp, 18.sp, 16.sp),
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // 数字先收缩，单位永远留出位置（否则 KG 会被列宽裁掉）
                modifier = Modifier.weight(1f, fill = false),
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = colors.inkTertiary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                )
            }
        }
    }
}

/**
 * 三列统计的宽度分配：天数和组数最多两三位，让出宽度给「训练容量」（六七位数）。
 * 等比分配时容量列会被裁掉单位，见 airead「首页统计列宽」。
 */
private const val STAT_WEIGHT_NARROW = 0.8f
private const val STAT_WEIGHT_WIDE = 1.4f

/**
 * 数字太长时降一档字号：`11,287` 用最大号，七位以上（含千分位）才缩，
 * 这样常规数据视觉不变，极端值也不会把单位挤出去。
 */
private fun statValueFontSize(value: String, large: TextUnit, medium: TextUnit, small: TextUnit): TextUnit =
    when {
        value.length >= 11 -> small
        value.length >= 9 -> medium
        else -> large
    }

@Composable
private fun ColumnDivider() {
    val colors = homeColors()
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .width(1.dp)
            .height(30.dp)
            .background(colors.hair),
    )
}

@Composable
private fun StartTrainingButton(
    onClick: () -> Unit,
    brand: Color,
    onBrand: Color,
) {
    val shape = RoundedCornerShape(HomeDims.controlRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                clip = false,
                ambientColor = brand.copy(alpha = 0.18f),
                spotColor = brand.copy(alpha = 0.42f),
            ),
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = brand,
                contentColor = onBrand,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.home_start_training),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------- 训练足迹

@Composable
private fun WeekCard(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    trainedDays: Set<Long>,
    onSelect: (LocalDate) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val colors = homeColors()
    val weekdays = stringArrayResource(R.array.home_weekdays)
    val currentWeek = weekStart == HomePeriod.weekStart(today)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ceramic(colors, CeramicStyle.Flat)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp,
                ),
                color = colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenCalendar)
                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_open_calendar),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.brand,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
        ) {
            repeat(7) { index ->
                val date = weekStart.plusDays(index.toLong())
                WeekDay(
                    label = weekdays.getOrElse(index) { "" },
                    date = date,
                    selected = date == selectedDate,
                    today = date == today,
                    trained = trainedDays.contains(date.toEpochDay()),
                    colors = colors,
                    onClick = { onSelect(date) },
                    modifier = Modifier.weight(1f),
                )
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
    colors: HomeColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circleColor by animateColorAsState(
        targetValue = if (selected) colors.brand else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "weekday-circle",
    )
    val dayColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            today -> colors.brand
            else -> colors.ink
        },
        animationSpec = tween(durationMillis = 180),
        label = "weekday-text",
    )
    val ringColor by animateColorAsState(
        targetValue = if (today && !selected) colors.brand.copy(alpha = 0.34f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "weekday-ring",
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            ),
            color = colors.inkSecondary,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(circleColor)
                .border(1.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (selected || today) FontWeight.Medium else FontWeight.Normal,
                    fontFeatureSettings = "tnum",
                ),
                color = dayColor,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        selected && trained -> colors.brand.copy(alpha = 0.55f)
                        trained -> colors.brand.copy(alpha = 0.85f)
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

// ---------------------------------------------------------------- 当天

@Composable
private fun DayHeader(
    date: LocalDate,
    today: LocalDate,
    timers: List<TimerRecordWithLaps>,
    onOpenSavedTimer: (String) -> Unit,
) {
    val colors = homeColors()
    val isToday = date == today
    val totalDayDuration = timers.sumOf { it.record.durationMillis }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(dayTitleFormatter),
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.1).sp,
            ),
            color = colors.ink,
            maxLines = 1,
        )
        if (isToday) {
            Text(
                text = stringResource(R.string.home_day_today),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.brand,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .brandChip(colors, RoundedCornerShape(HomeDims.chipRadius))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (totalDayDuration > 0L) {
            val dayDurationLabel = stringResource(R.string.timer_day_duration_label)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.inkSecondary)) {
                        append(dayDurationLabel)
                    }
                    append(TrainingTimerLogic.formatDurationChinese(totalDayDuration))
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.ink,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (timers.size == 1) {
                            Modifier.clickable { onOpenSavedTimer(timers.first().record.id) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 2.dp, horizontal = 2.dp),
            )
        }
    }
}

/** 当天汇总：时长 / 组数 / 容量三列，数字重、标签轻。 */
@Composable
private fun DaySummaryCard(
    timers: List<TimerRecordWithLaps>,
    groups: List<HomePartGroup>,
    singleSessionId: String?,
    onOpenSession: (String) -> Unit,
) {
    val colors = homeColors()
    val durationMillis = timers.sumOf { it.record.durationMillis }
    val setCount = groups.sumOf { group -> group.exercises.sumOf { it.setCount } }
    val volumeKg = groups.sumOf { it.volumeKg }
    val durationLabel = remember(durationMillis) { durationParts(durationMillis) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ceramic(colors, CeramicStyle.Warm)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_today_summary),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (singleSessionId != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenSession(singleSessionId) }
                        .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_view_detail),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = colors.brand,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryStat(
                label = stringResource(R.string.home_summary_duration),
                value = durationLabel.primary,
                unit = durationLabel.primaryUnit,
                modifier = Modifier.weight(STAT_WEIGHT_NARROW),
            )
            ColumnDivider()
            SummaryStat(
                label = stringResource(R.string.home_stat_sets),
                value = setCount.toString(),
                unit = stringResource(R.string.home_unit_set),
                modifier = Modifier.weight(STAT_WEIGHT_NARROW),
            )
            ColumnDivider()
            SummaryStat(
                label = stringResource(R.string.home_stat_volume),
                value = WeightLogic.displayVolume(volumeKg),
                unit = "",
                modifier = Modifier.weight(STAT_WEIGHT_WIDE),
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = homeColors()
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
            color = colors.inkTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = statValueFontSize(value, 22.sp, 19.sp, 16.sp),
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.6).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = colors.inkTertiary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyDayCard(
    isToday: Boolean,
    note: String?,
    timers: List<TimerRecordWithLaps>,
    onOpenBody: () -> Unit,
    onOpenTrends: () -> Unit,
) {
    val colors = homeColors()
    val durationMillis = timers.sumOf { it.record.durationMillis }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ceramic(colors, CeramicStyle.Warm)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .brandChip(colors, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 11.dp)) {
                Text(
                    text = stringResource(
                        if (isToday) R.string.home_empty_title_today else R.string.home_empty_title_other,
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.1).sp,
                    ),
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = note ?: stringResource(R.string.home_empty_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (durationMillis > 0L) {
                    Text(
                        text = stringResource(
                            R.string.home_empty_timer_hint,
                            TrainingTimerLogic.formatDurationChinese(durationMillis),
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                        color = colors.inkTertiary,
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GhostButton(
                text = stringResource(R.string.home_record_body),
                onClick = onOpenBody,
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = stringResource(R.string.home_open_trends),
                onClick = onOpenTrends,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = homeColors()
    Box(
        modifier = modifier
            .height(43.dp)
            .frost(colors, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.ink,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- 部位卡

@Composable
private fun PartCard(
    group: HomePartGroup,
    onOpenSession: (String) -> Unit,
) {
    val colors = homeColors()
    val setCount = group.exercises.sumOf { it.setCount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ceramic(colors, CeramicStyle.Flat)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_part_training, group.label),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.1).sp,
                ),
                color = colors.ink,
                modifier = Modifier.weight(1f),
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
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                color = colors.inkTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        group.exercises.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { exercise ->
                    ExerciseTile(
                        exercise = exercise,
                        colors = colors,
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

@Composable
private fun ExerciseTile(
    exercise: HomeExerciseRow,
    colors: HomeColors,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(HomeDims.tileRadius))
            .background(colors.tileBackground)
            .clickable { onOpenSession(exercise.sessionId) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(
            text = exercise.name.ifBlank { stringResource(R.string.empty_session_exercises) },
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            ),
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.home_exercise_sets, exercise.setCount),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = colors.inkTertiary,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = WeightLogic.displayVolume(exercise.volumeKg),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
