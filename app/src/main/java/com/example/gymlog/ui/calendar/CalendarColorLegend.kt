package com.example.gymlog.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.CalendarSpecialDay
import com.example.gymlog.ui.theme.Ink

internal val CalendarDayInk = Ink

internal fun CalendarDayPaint.fillColor(): Color? {
    val argb = argb() ?: return null
    return Color(argb)
}

@Composable
fun CalendarColorLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarColorCategory.entries.forEach { category ->
                LegendItem(color = Color(category.argb), label = stringResource(category.labelRes()))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarSpecialDay.entries.forEach { day ->
                LegendItem(color = Color(day.argb), label = stringResource(day.labelRes()))
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

private fun CalendarColorCategory.labelRes(): Int = when (this) {
    CalendarColorCategory.CHEST -> R.string.calendar_color_chest
    CalendarColorCategory.BACK -> R.string.calendar_color_back
    CalendarColorCategory.SHOULDERS -> R.string.calendar_color_shoulders
    CalendarColorCategory.LEGS -> R.string.calendar_color_legs
    CalendarColorCategory.ARMS -> R.string.calendar_color_arms
    CalendarColorCategory.OTHER -> R.string.calendar_color_other
}

internal fun CalendarSpecialDay.markRes(): Int = when (this) {
    CalendarSpecialDay.LAZY -> R.string.day_kind_mark_lazy
    CalendarSpecialDay.INDULGE -> R.string.day_kind_mark_indulge
    CalendarSpecialDay.REST -> R.string.day_kind_mark_rest
}

private fun CalendarSpecialDay.labelRes(): Int = when (this) {
    CalendarSpecialDay.LAZY -> R.string.day_kind_lazy
    CalendarSpecialDay.INDULGE -> R.string.day_kind_indulge
    CalendarSpecialDay.REST -> R.string.day_kind_rest
}
