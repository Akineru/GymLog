package com.example.gymlog.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog.R
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.CalendarSpecialDay
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingCalendarDetailScreen(
    month: YearMonth,
    dayPaints: Map<Long, CalendarDayPaint>,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val today = LocalDate.now()
    val cells = remember(month) { calendarCells(month) }
    val weekCount = cells.size / 7

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_calendar)) },
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
                .padding(horizontal = GymLogSpacing.md, vertical = GymLogSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GymLogSpacing.sm, vertical = GymLogSpacing.xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onMonthChange(month.minusMonths(1)) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "<",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Text(
                            text = month.format(monthFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onMonthChange(month.plusMonths(1)) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = ">",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdayLabels.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        repeat(weekCount) { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                repeat(7) { weekday ->
                                    val date = cells[week * 7 + weekday]
                                    DetailDayCell(
                                        date = date,
                                        inMonth = date.month == month.month && date.year == month.year,
                                        isToday = date == today,
                                        isSelected = date == selectedDate,
                                        paint = dayPaints[date.toEpochDay()] ?: CalendarDayPaint.Empty,
                                        onClick = {
                                            val dateMonth = YearMonth.from(date)
                                            if (dateMonth != month) onMonthChange(dateMonth)
                                            onDayClick(date)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            CalendarColorLegend(
                modifier = Modifier.padding(horizontal = GymLogSpacing.xs, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun DetailDayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    paint: CalendarDayPaint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = paint.fillColor()
    val ink = when {
        fill != null && inMonth -> CalendarDayInk
        fill != null -> CalendarDayInk.copy(alpha = 0.45f)
        !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fill ?: Color.Transparent)
            .then(
                if (isSelected && inMonth) {
                    Modifier.border(
                        2.dp,
                        if (fill == null) MaterialTheme.colorScheme.primary else CalendarDayInk,
                        RoundedCornerShape(10.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val numberModifier = if (isToday && inMonth && !isSelected) {
            Modifier
                .size(28.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
        } else {
            Modifier.size(28.dp)
        }
        Box(modifier = numberModifier, contentAlignment = Alignment.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isToday && fill == null && inMonth) {
                    MaterialTheme.colorScheme.primary
                } else {
                    ink
                },
                fontWeight = if (isSelected || isToday) FontWeight.Normal else FontWeight.Normal,
            )
        }
        val special = (paint as? CalendarDayPaint.Special)?.day
        if (inMonth && special != null) {
            Text(
                text = stringResource(special.markRes()),
                color = CalendarDayInk,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
