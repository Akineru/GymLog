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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

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
    val cells = remember(month) { calendarCells(month) }
    val today = LocalDate.now()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "本月训练",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFBBD0EC),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${month.atDay(1).format(dayFormatter)} — ${month.atEndOfMonth().format(dayFormatter)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFBBD0EC),
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
                            .clickable { onMonthChange(month.minusMonths(1)) }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onMonthChange(month.plusMonths(1)) }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                MonthGrid(
                    cells = cells,
                    month = month,
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

@Composable
private fun MonthStat(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = Color.White,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFBBD0EC),
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
                    color = Color(0xFF9EB5D0),
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
