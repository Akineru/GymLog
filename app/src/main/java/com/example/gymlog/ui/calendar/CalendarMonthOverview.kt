package com.example.gymlog.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.partVolumes
import com.example.gymlog.ui.theme.BrandBlue
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.Ink
import com.example.gymlog.ui.theme.LiquidGlassCard
import com.example.gymlog.ui.theme.LiquidGlassColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

/** Pager 页与月份的换算：第 0 页 = 1970年1月，共 1600 页（约到 2103 年）。 */
private const val PAGE_COUNT = 1600
private fun pageOfMonth(month: YearMonth): Int =
    ((month.year - 1970) * 12 + month.monthValue - 1).coerceIn(0, PAGE_COUNT - 1)
private fun monthOfPage(page: Int): YearMonth =
    YearMonth.of(1970, 1).plusMonths(page.toLong())

@Composable
fun CalendarMonthOverview(
    month: YearMonth,
    sessions: List<SessionDetail>,
    measurements: List<BodyMeasurementEntity>,
    dayPaints: Map<Long, CalendarDayPaint>,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpenFullMonth: () -> Unit,
) {
    val monthSessions = remember(month, sessions) {
        sessions.filter { detail ->
            val date = LocalDate.ofEpochDay(detail.session.dateEpochDay)
            YearMonth.from(date) == month && detail.exercises.any { it.sets.isNotEmpty() }
        }
    }
    val trainingCount = remember(monthSessions) {
        monthSessions.map { LocalDate.ofEpochDay(it.session.dateEpochDay) }.distinct().size
    }
    val setCount = remember(monthSessions) {
        monthSessions.sumOf { detail -> detail.exercises.sumOf { it.sets.size } }
    }
    val volumeKg = remember(monthSessions, measurements) {
        monthSessions.sumOf { detail ->
            listOf(detail).partVolumes(measurements).sumOf { (_, volume) -> volume }
        }
    }
    val today = LocalDate.now()
    // 左右滑动翻月：pager 页 ↔ 月份状态双向联动
    val pagerState = rememberPagerState(initialPage = pageOfMonth(month)) { PAGE_COUNT }
    // collect 闭包长驻，必须用 rememberUpdatedState 读最新月份，否则比较的是组合时的旧值
    val currentMonth by rememberUpdatedState(month)
    LaunchedEffect(pagerState) {
        // 手势结束落定的页 → 更新月份（settled 不会打断拖动）
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val m = monthOfPage(page)
            if (m != currentMonth) onMonthChange(m)
        }
    }
    LaunchedEffect(month) {
        // 外部（按钮/点击其他月日期）改月份 → 同步翻到对应页
        val target = pageOfMonth(month)
        if (pagerState.settledPage != target) pagerState.scrollToPage(target)
    }
    val pagerScope = rememberCoroutineScope()
    val turnPage: (Int) -> Unit = { delta ->
        // ‹ › 按钮也走 pager 翻页动画
        val target = (pagerState.settledPage + delta).coerceIn(0, PAGE_COUNT - 1)
        pagerScope.launch { pagerState.animateScrollToPage(target) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "本月训练",
                        style = MaterialTheme.typography.titleSmall,
                        color = LiquidGlassColors.inkMid,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${month.atDay(1).format(dayFormatter)} — ${month.atEndOfMonth().format(dayFormatter)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = LiquidGlassColors.inkSoft,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    MonthStat("训练次数", trainingCount.toString(), Modifier.weight(1f))
                    MonthStat("总组数", setCount.toString(), Modifier.weight(1f))
                    MonthStat(
                        "总容量",
                        String.format(Locale.US, "%.1f t", volumeKg / 1_000.0),
                        Modifier.weight(1f),
                    )
                }
            }
        }
        GymLogCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                ) {
                    Text(
                        text = month.format(monthFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenFullMonth)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { turnPage(-1) }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { turnPage(1) }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                // 月份网格左右滑动切换，自带滑动动画
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val pageMonth = monthOfPage(page)
                    MonthGrid(
                        cells = remember(pageMonth) { calendarCells(pageMonth) },
                        month = pageMonth,
                        today = today,
                        selectedDate = selectedDate,
                        dayPaints = dayPaints,
                        onMonthChange = onMonthChange,
                        onDayClick = onDayClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthStat(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = LiquidGlassColors.ink,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LiquidGlassColors.inkSoft,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthGrid(
    cells: List<LocalDate>,
    month: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    dayPaints: Map<Long, CalendarDayPaint>,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    // 白卡上的星期标签：原来的 #9EB5D0 只有 2.1:1，压到次级色
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    MonthDayCircle(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        paint = dayPaints[date.toEpochDay()] ?: CalendarDayPaint.Empty,
                        onClick = {
                            if (YearMonth.from(date) != month) onMonthChange(YearMonth.from(date))
                            onDayClick(date)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCircle(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    paint: CalendarDayPaint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = when (paint) {
        is CalendarDayPaint.Training -> Color(LocalCalendarColorPalette.current.color(paint.category))
        is CalendarDayPaint.Special -> Color(paint.day.argb)
        CalendarDayPaint.Empty -> Color.Transparent
    }
    val ink = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
        fill != Color.Transparent -> fill.onColorInk()
        paint is CalendarDayPaint.Training -> Ink
        paint is CalendarDayPaint.Special -> Ink
        else -> MaterialTheme.colorScheme.onSurface
    }
    val background = if (isSelected) MaterialTheme.colorScheme.primary else fill
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(background)
                .then(
                    if (isToday && !isSelected) {
                        Modifier.border(1.dp, BrandBlue, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = ink,
                fontWeight = if (isSelected || isToday) FontWeight.Normal else FontWeight.Normal,
            )
        }
    }
}
