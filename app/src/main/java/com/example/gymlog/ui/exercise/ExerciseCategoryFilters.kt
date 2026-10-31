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
    /** 双击分类时回调（通常是「右侧列表滚回顶部」）；null 则只响应单击。 */
    onDoubleClick: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier
            .width(82.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(ExerciseClassification.bodyUi, key = { it.id }) { part ->
            val isSelected = part.id == selected.id
            // 回调放进 rememberUpdatedState：第一次点击会触发 onSelect → 状态更新 → 重组，
            // 若直接把回调当 pointerInput 的 key，重组会取消手势、吞掉双击的第二次点击。
            val currentOnSelect by rememberUpdatedState(onSelect)
            val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
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
                        // 不用 combinedClickable：detectTapGestures 的语义和需求一致——
                        // 每次点按都触发 onSelect（单击换分类），双击在此基础上再滚回顶部。
                        detectTapGestures(
                            onTap = { currentOnSelect(part) },
                            onDoubleTap = currentOnDoubleClick?.let { cb -> { cb() } },
                        )
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
