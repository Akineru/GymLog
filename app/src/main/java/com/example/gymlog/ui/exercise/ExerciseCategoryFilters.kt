package com.example.gymlog.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymlog.data.ExerciseClassification

@Composable
fun BodyCategoryRail(
    selected: ExerciseClassification.BodyCategory,
    onSelect: (ExerciseClassification.BodyCategory) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(82.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(ExerciseClassification.bodyUi, key = { it.id }) { part ->
            val isSelected = part.id == selected.id
            // 回调放进 rememberUpdatedState，避免重组后手指下的手势仍持有旧 lambda。
            val currentOnSelect by rememberUpdatedState(onSelect)
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                    )
                    .border(
                        1.dp,
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        RoundedCornerShape(14.dp),
                    )
                    .pointerInput(part.id) {
                        // 只处理单击：保留 pointerInput + key 的写法，避免 combinedClickable
                        // 在状态重组时取消进行中的手势。
                        detectTapGestures(onTap = { currentOnSelect(part) })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = part.labelZh,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true,
                )
            }
        }
    }
}

@Composable
fun EquipmentCategoryBar(
    selected: ExerciseClassification.EquipmentCategory,
    onSelect: (ExerciseClassification.EquipmentCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExerciseClassification.equipmentUi.forEach { type ->
            FilterChip(
                selected = selected.id == type.id,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        text = type.labelZh,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}

/**
 * 筛选摘要行：无筛选只显示「N 个动作」，避免出现双「全部」；
 * 有筛选时显示「部位 · 器械 · N 个动作」（只带已选中的维度）。
 */
@Composable
fun ExerciseSummaryText(
    count: Int,
    bodyPart: ExerciseClassification.BodyCategory,
    equipmentType: ExerciseClassification.EquipmentCategory,
    searchingAll: Boolean,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleSmall,
) {
    Text(
        text = if (searchingAll) {
            androidx.compose.ui.res.stringResource(com.example.gymlog.R.string.library_search_count, count)
        } else {
            val filters = listOf(
                bodyPart.takeIf { it.id.isNotBlank() }?.labelZh,
                equipmentType.takeIf { it.id.isNotBlank() }?.labelZh,
            ).filterNotNull()
            if (filters.isEmpty()) {
                androidx.compose.ui.res.stringResource(com.example.gymlog.R.string.library_count_plain, count)
            } else {
                androidx.compose.ui.res.stringResource(
                    com.example.gymlog.R.string.library_section_count,
                    filters.joinToString(" · "),
                    count,
                )
            }
        },
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
