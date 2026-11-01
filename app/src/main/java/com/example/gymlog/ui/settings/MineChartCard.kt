package com.example.gymlog.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.gymlog.data.WeightLogic
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.launch

/** 图表指标：与 Pager 三页一一对应，左右滑动切换。 */
enum class MineChartMetric(val label: String) {
    DURATION("时长"),
    VOLUME("训练量"),
    DAYS("训练天数"),
}

/** 时长单位：小时（小数）/ 分钟。 */
enum class MineDurationUnitOption(val label: String) {
    HOURS("小时"),
    MINUTES("分钟"),
}

/** 训练量单位：KG / T（吨）。 */
enum class MineVolumeUnitOption(val label: String) {
    KG("KG"),
    T("T"),
}

/**
 * 我的页按周统计柱状图卡（名片与快捷入口之间）。
 * 头部：大数居左，范围下拉居中，单位切换居右；
 * 底部 chips 与 Pager 双向联动切换指标。
 */
@Composable
fun MineWeeklyChartCard(
    bars: List<MineWeeklyBar>,
    range: MineChartRange,
    onRangeChange: (MineChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = MineChartMetric.entries
    val pagerState = rememberPagerState { metrics.size }
    val scope = rememberCoroutineScope()
    // 单位记忆在卡片内：时长默认小时，训练量默认 KG，天数无单位切换
    var durationUnit by remember { mutableStateOf(MineDurationUnitOption.HOURS) }
    var volumeUnit by remember { mutableStateOf(MineVolumeUnitOption.KG) }
    val currentMetric = metrics[pagerState.currentPage]
    // 点选的柱子：切换指标页或范围时清空
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage, bars, range) {
        selectedBar = null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Box 叠加布局：数值是主角居中，范围切换靠左、单位切换靠右
        Box(modifier = Modifier.fillMaxWidth()) {
            MineChartDropdown(
                label = range.label,
                options = MineChartRange.entries.map { it.label },
                selectedIndex = MineChartRange.entries.indexOf(range),
                onSelect = { onRangeChange(MineChartRange.entries[it]) },
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = headerValue(currentMetric, bars, durationUnit, volumeUnit),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Center),
            )
            when (currentMetric) {
                MineChartMetric.DURATION -> MineChartDropdown(
                    label = durationUnit.label,
                    options = MineDurationUnitOption.entries.map { it.label },
                    selectedIndex = MineDurationUnitOption.entries.indexOf(durationUnit),
                    onSelect = { durationUnit = MineDurationUnitOption.entries[it] },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                MineChartMetric.VOLUME -> MineChartDropdown(
                    label = volumeUnit.label,
                    options = MineVolumeUnitOption.entries.map { it.label },
                    selectedIndex = MineVolumeUnitOption.entries.indexOf(volumeUnit),
                    onSelect = { volumeUnit = MineVolumeUnitOption.entries[it] },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
                MineChartMetric.DAYS -> Unit
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            WeeklyBarChart(
                bars = bars,
                metric = metrics[page],
                durationUnit = durationUnit,
                volumeUnit = volumeUnit,
                selectedBar = selectedBar,
                onBarClick = { selectedBar = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            metrics.forEachIndexed { index, metric ->
                val selected = pagerState.currentPage == index
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            RoundedCornerShape(50),
                        )
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** 头部大数：当前指标范围内总量，按所选单位显示。 */
private fun headerValue(
    metric: MineChartMetric,
    bars: List<MineWeeklyBar>,
    durationUnit: MineDurationUnitOption,
    volumeUnit: MineVolumeUnitOption,
): String {
    return when (metric) {
        MineChartMetric.DURATION -> {
            val millis = bars.sumOf { it.durationMillis }
            when (durationUnit) {
                // 90 分钟 = 1.5 小时
                MineDurationUnitOption.HOURS -> stripTrailingZero(millis / 3_600_000.0) + " 小时"
                MineDurationUnitOption.MINUTES -> (millis / 60_000).toString() + " 分钟"
            }
        }
        MineChartMetric.VOLUME -> {
            val kg = bars.sumOf { it.volumeKg }
            when (volumeUnit) {
                MineVolumeUnitOption.KG -> formatVolumeCompact(kg) + " kg"
                MineVolumeUnitOption.T -> formatTonnes(kg) + " T"
            }
        }
        MineChartMetric.DAYS -> bars.sumOf { it.trainingDays }.toString() + " 天"
    }
}

/** 吨：≥10T 取整，否则保留 1 位小数。 */
private fun formatTonnes(kg: Double): String {
    val t = kg / 1_000.0
    return if (t >= 10.0) Math.round(t).toString() else String.format("%.1f", t)
}

private fun stripTrailingZero(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

/** 训练量超四位数用 k：15k；以内保持原格式。 */
internal fun formatVolumeCompact(volumeKg: Double): String {
    return if (volumeKg >= 10_000) {
        "${Math.round(volumeKg / 1_000)}k"
    } else {
        WeightLogic.formatKg(volumeKg)
    }
}

/** 与按钮对齐的下拉选择：菜单锚定在按钮正下方。 */
@Composable
private fun MineChartDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    // Box 即按钮的边界，DropdownMenu 锚定其左下角，与按钮对齐
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
}

/**
 * 按周柱状图：Y 轴 0/半/满三档刻度，X 轴抽稀标注周起始日期。
 * 点击柱子在其上方气泡显示具体数值，再点同一根或空白处取消。
 * 数据全为 0 时显示占位文案。
 */
@Composable
private fun WeeklyBarChart(
    bars: List<MineWeeklyBar>,
    metric: MineChartMetric,
    durationUnit: MineDurationUnitOption,
    volumeUnit: MineVolumeUnitOption,
    selectedBar: Int? = null,
    onBarClick: (Int?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val labelPx = with(density) { 10.dp.toPx() }

    // 绘图用原始值 + 各自的格式化
    data class ChartValue(val raw: Double, val text: (Double) -> String)

    val values: List<Double> = bars.map { bar ->
        when (metric) {
            MineChartMetric.DURATION -> when (durationUnit) {
                MineDurationUnitOption.HOURS -> bar.durationMillis / 3_600_000.0
                MineDurationUnitOption.MINUTES -> bar.durationMillis / 60_000.0
            }
            MineChartMetric.VOLUME -> when (volumeUnit) {
                MineVolumeUnitOption.KG -> bar.volumeKg
                MineVolumeUnitOption.T -> bar.volumeKg / 1_000.0
            }
            MineChartMetric.DAYS -> bar.trainingDays.toDouble()
        }
    }
    val maxValue = niceCeil(values.maxOrNull() ?: 0.0)

    // 选中柱的实时值供手势闭包读取，避免 pointerInput 不重启导致读到旧值
    val currentSelected by androidx.compose.runtime.rememberUpdatedState(selectedBar)
    val currentOnBarClick by androidx.compose.runtime.rememberUpdatedState(onBarClick)
    val tapSpaceLeftPx = with(density) { 34.dp.toPx() }

    fun axisLabel(v: Double): String = when (metric) {
        MineChartMetric.DURATION -> when (durationUnit) {
            MineDurationUnitOption.HOURS -> stripTrailingZero(v) + "h"
            MineDurationUnitOption.MINUTES -> v.toInt().toString()
        }
        MineChartMetric.VOLUME -> when (volumeUnit) {
            MineVolumeUnitOption.KG -> formatVolumeCompact(v)
            MineVolumeUnitOption.T -> when {
                v == 0.0 -> "0"
                v >= 10.0 -> Math.round(v).toString()
                else -> String.format("%.1f", v)
            }
        }
        MineChartMetric.DAYS -> v.toInt().toString()
    }

    Canvas(
        modifier = modifier.pointerInput(values, metric, durationUnit, volumeUnit) {
            // 点击命中所在列：Y 轴标签区之外按列均分；再点同一根取消
            detectTapGestures { offset ->
                if (offset.x < tapSpaceLeftPx) {
                    currentOnBarClick(null)
                    return@detectTapGestures
                }
                val plotWidth = size.width - tapSpaceLeftPx
                val index = (((offset.x - tapSpaceLeftPx) / plotWidth) * values.size)
                    .toInt()
                    .coerceIn(0, values.size - 1)
                currentOnBarClick(if (currentSelected == index) null else index)
            }
        },
    ) {
        val labelSpaceLeft = 34.dp.toPx()
        val labelSpaceBottom = 18.dp.toPx()
        val plotWidth = size.width - labelSpaceLeft
        val plotHeight = size.height - labelSpaceBottom
        val baseline = plotHeight

        fun yFor(v: Double): Float = (baseline - (v / maxValue) * plotHeight).toFloat()

        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = labelPx
        }
        fun argb(c: Color): Int = android.graphics.Color.argb(
            (c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(),
        )
        paint.color = argb(labelColor)

        // 横向网格线 + Y 轴刻度（0 / 半 / 满）
        for (fraction in listOf(0.0, 0.5, 1.0)) {
            val y = yFor(maxValue * fraction)
            drawLine(
                color = gridColor,
                start = Offset(labelSpaceLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                axisLabel(maxValue * fraction),
                0f,
                y + labelPx / 3,
                paint,
            )
        }

        if (values.isEmpty() || (values.maxOrNull() ?: 0.0) <= 0.0) {
            drawContext.canvas.nativeCanvas.drawText(
                "暂无训练数据",
                labelSpaceLeft + plotWidth / 2 - labelPx * 3,
                baseline - plotHeight / 2,
                paint,
            )
            return@Canvas
        }

        val slot = plotWidth / values.size
        val barWidth = (slot * 0.55f).coerceAtMost(28.dp.toPx())
        values.forEachIndexed { index, value ->
            if (value <= 0.0) return@forEachIndexed
            // 选中时其他柱变淡，突出当前柱
            val color = when {
                selectedBar == null -> barColor
                selectedBar == index -> barColor
                else -> barColor.copy(alpha = 0.35f)
            }
            val left = labelSpaceLeft + slot * index + (slot - barWidth) / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(left, yFor(value)),
                size = Size(barWidth, baseline - yFor(value)),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }

        // 选中柱：顶部气泡显示具体数值
        val selectedIndex = selectedBar
        if (selectedIndex != null && selectedIndex in values.indices && values[selectedIndex] > 0.0) {
            val value = values[selectedIndex]
            val text = fullValueLabelOf(metric, durationUnit, volumeUnit, value)
            val bubblePadH = 8.dp.toPx()
            val textWidth = paint.measureText(text)
            val bubbleWidth = textWidth + bubblePadH * 2
            val bubbleHeight = labelPx + 10.dp.toPx()
            val barCenterX = labelSpaceLeft + slot * selectedIndex + slot / 2
            var bubbleLeft = barCenterX - bubbleWidth / 2
            bubbleLeft = bubbleLeft.coerceIn(labelSpaceLeft, size.width - bubbleWidth)
            val bubbleTop = (yFor(value) - bubbleHeight - 6.dp.toPx()).coerceAtLeast(2.dp.toPx())
            val bg = android.graphics.Paint().apply {
                isAntiAlias = true
                color = argb(labelColor)
            }
            drawContext.canvas.nativeCanvas.drawRoundRect(
                bubbleLeft,
                bubbleTop,
                bubbleLeft + bubbleWidth,
                bubbleTop + bubbleHeight,
                8.dp.toPx(),
                8.dp.toPx(),
                bg,
            )
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = labelPx
                textAlign = android.graphics.Paint.Align.CENTER
                color = android.graphics.Color.WHITE
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                bubbleLeft + bubbleWidth / 2,
                bubbleTop + bubbleHeight / 2 + labelPx / 3,
                textPaint,
            )
        }

        // X 轴：抽稀标注约 5 个周起始日期
        val labelCount = minOf(5, values.size)
        if (labelCount > 0) {
            val step = ((values.size - 1) / (labelCount - 1)).coerceAtLeast(1)
            val indices = if (values.size == 1) listOf(0) else (0 until values.size step step).toList()
            indices.forEach { index ->
                val date = bars[index].weekStart
                val text = "${date.monthValue}/${date.dayOfMonth}"
                val x = labelSpaceLeft + slot * index + slot / 2
                val textWidth = paint.measureText(text)
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    (x - textWidth / 2).coerceAtLeast(labelSpaceLeft),
                    size.height - 2.dp.toPx(),
                    paint,
                )
            }
        }
    }
}

/** 气泡里的数值：不带单位（单位已由头部大数表达），纯数字最干净。 */
private fun fullValueLabelOf(
    metric: MineChartMetric,
    durationUnit: MineDurationUnitOption,
    volumeUnit: MineVolumeUnitOption,
    v: Double,
): String = when (metric) {
    MineChartMetric.DURATION -> when (durationUnit) {
        MineDurationUnitOption.HOURS -> stripTrailingZero(v)
        MineDurationUnitOption.MINUTES -> v.toInt().toString()
    }
    MineChartMetric.VOLUME -> when (volumeUnit) {
        MineVolumeUnitOption.KG -> formatVolumeCompact(v)
        MineVolumeUnitOption.T -> when {
            v == 0.0 -> "0"
            v >= 10.0 -> Math.round(v).toString()
            else -> String.format("%.1f", v)
        }
    }
    MineChartMetric.DAYS -> v.toInt().toString()
}

/** 取比数据最大值略大的「好看」刻度：1/2/5 × 10^n。 */
private fun niceCeil(value: Double): Double {
    if (value <= 0.0) return 1.0
    val exp = floor(log10(value)).toInt()
    val base = 10.0.pow(exp)
    val f = value / base
    val nice = when {
        f <= 1.0 -> 1.0
        f <= 2.0 -> 2.0
        f <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * base
}
