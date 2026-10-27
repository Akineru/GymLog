package com.example.gymlog.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymlog.R
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.CalendarSpecialDay
import com.example.gymlog.ui.theme.GymLogTheme
import com.example.gymlog.ui.theme.colorForPart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)

internal val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun TrainingCalendarCard(
    month: YearMonth,
    partsByDate: Map<LocalDate, List<String>>,
    dayPaints: Map<Long, CalendarDayPaint> = emptyMap(),
    selectedDate: LocalDate? = null,
    collapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val monthCells = remember(month) { calendarCells(month) }
    val cells = remember(monthCells, selectedDate, collapsed) {
        if (!collapsed) {
            monthCells
        } else {
            val selectedIndex = selectedDate?.let(monthCells::indexOf) ?: -1
            if (selectedIndex >= 0) {
                val weekStart = (selectedIndex / 7) * 7
                monthCells.subList(weekStart, weekStart + 7)
            } else {
                monthCells.take(7)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
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
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpen)
                        .padding(vertical = 4.dp),
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

            Spacer(Modifier.height(4.dp))

            val weekCount = cells.size / 7
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(weekCount) { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        repeat(7) { weekday ->
                            val date = cells[week * 7 + weekday]
                            DayCell(
                                date = date,
                                inMonth = date.month == month.month && date.year == month.year,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                parts = partsByDate[date].orEmpty(),
                                paint = dayPaints[date.toEpochDay()] ?: CalendarDayPaint.Empty,
                                onClick = {
                                    val dateMonth = YearMonth.from(date)
                                    if (dateMonth != month) {
                                        onMonthChange(dateMonth)
                                    }
                                    onDayClick(date)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            onToggleCollapse?.let { toggle ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = toggle,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (collapsed) {
                                Icons.Filled.KeyboardArrowDown
                            } else {
                                Icons.Filled.KeyboardArrowUp
                            },
                            contentDescription = stringResource(
                                if (collapsed) {
                                    R.string.calendar_expand_month
                                } else {
                                    R.string.calendar_collapse_week
                                },
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    parts: List<String>,
    paint: CalendarDayPaint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = paint.fillColor()
    val ink = if (fill == null) {
        when {
            isSelected && inMonth -> MaterialTheme.colorScheme.onPrimary
            !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.onSurface
        }
    } else if (inMonth) {
        fill.onColorInk()
    } else {
        fill.onColorInk().copy(alpha = 0.45f)
    }
    val numberModifier = when {
        fill == null && isSelected && inMonth -> Modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
        fill == null && isToday && inMonth -> Modifier
            .size(28.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        fill != null && isToday && inMonth && !isSelected -> Modifier
            .size(28.dp)
            .border(1.5.dp, CalendarDayInk, CircleShape)
        else -> Modifier.size(28.dp)
    }
    val selection = if (isSelected) {
        Modifier.border(1.5.dp, if (fill == null) MaterialTheme.colorScheme.primary else CalendarDayInk, RoundedCornerShape(10.dp))
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(fill ?: Color.Transparent)
            .then(selection)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = numberModifier, contentAlignment = Alignment.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = ink,
                fontWeight = if (isSelected && inMonth) FontWeight.Normal else FontWeight.Normal,
            )
        }
        Spacer(Modifier.height(1.dp))
        SpecialDayMark(paint = paint, inMonth = inMonth)
        PartDots(parts = parts, onFill = fill != null)
    }
}

@Composable
private fun SpecialDayMark(paint: CalendarDayPaint, inMonth: Boolean) {
    val special = (paint as? CalendarDayPaint.Special)?.day
    val label = if (inMonth && special != null) stringResource(special.markRes()) else ""
    Text(
        text = label,
        color = if (label.isEmpty()) Color.Transparent else CalendarDayInk,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
    )
}

@Composable
private fun PartDots(parts: List<String>, onFill: Boolean) {
    Row(
        modifier = Modifier.height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (parts.isEmpty()) {
            return@Row
        }
        val visible = parts.take(3)
        visible.forEach { part ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(colorForPart(part)),
            )
        }
        val extra = parts.size - visible.size
        if (extra > 0) {
            Text(
                text = "+$extra",
                fontSize = 7.sp,
                color = if (onFill) CalendarDayInk else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 8.sp,
            )
        }
    }
}

internal fun calendarCells(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val weekCount = (lead + month.lengthOfMonth() + 6) / 7
    val start = first.minusDays(lead.toLong())
    return List(weekCount * 7) { index -> start.plusDays(index.toLong()) }
}

@Preview(showBackground = true)
@Composable
private fun TrainingCalendarCardPreview() {
    val previewMonth = YearMonth.of(2026, 10)
    val fakeParts = mapOf(
        LocalDate.of(2026, 10, 1) to listOf("胸"),
        LocalDate.of(2026, 10, 3) to listOf("背", "肩"),
        LocalDate.of(2026, 10, 6) to listOf("胸", "背", "腿", "腹"),
        LocalDate.of(2026, 10, 11) to listOf("手臂"),
        LocalDate.of(2026, 10, 15) to listOf("腿", "腹"),
        LocalDate.of(2026, 10, 18) to listOf("胸", "肩", "手臂"),
        LocalDate.of(2026, 10, 22) to listOf("其他"),
        LocalDate.of(2026, 10, 25) to listOf("背"),
        LocalDate.of(2026, 10, 28) to listOf("胸", "背", "肩", "腿", "手臂"),
    )
    var month by remember { mutableStateOf(previewMonth) }
    GymLogTheme(dynamicColor = false) {
        TrainingCalendarCard(
            month = month,
            partsByDate = fakeParts,
            selectedDate = LocalDate.of(2026, 10, 11),
            onMonthChange = { month = it },
            onDayClick = {},
            onOpen = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
