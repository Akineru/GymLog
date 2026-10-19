package com.example.gymlog.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.CalendarSpecialDay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayKindSelector(
    selected: DayKind?,
    onSelect: (DayKind) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(DayKind.REST, DayKind.INDULGE, DayKind.LAZY).forEach { kind ->
            FilterChip(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                label = { Text(stringResource(kind.labelRes())) },
            )
        }
    }
}

internal fun DayKind.labelRes(): Int = when (this) {
    DayKind.TRAINING -> R.string.day_kind_training
    DayKind.REST -> R.string.day_kind_rest
    DayKind.INDULGE -> R.string.day_kind_indulge
    DayKind.LAZY -> R.string.day_kind_lazy
}

internal fun DayKind.markRes(): Int = when (this) {
    DayKind.TRAINING -> R.string.day_kind_mark_training
    DayKind.REST -> R.string.day_kind_mark_rest
    DayKind.INDULGE -> R.string.day_kind_mark_indulge
    DayKind.LAZY -> R.string.day_kind_mark_lazy
}

@Composable
internal fun DayKind.markColor(): Color = when (this) {
    DayKind.TRAINING -> MaterialTheme.colorScheme.primary
    DayKind.REST -> Color(CalendarSpecialDay.REST.argb)
    DayKind.INDULGE -> Color(CalendarSpecialDay.INDULGE.argb)
    DayKind.LAZY -> Color(CalendarSpecialDay.LAZY.argb)
}
