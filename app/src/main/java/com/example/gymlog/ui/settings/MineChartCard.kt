package com.example.gymlog.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
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

/**
 * 我的页按周统计柱状图卡（名片与快捷入口之间）。
 * 右上角下拉切换范围；下方 chips 与 Pager 双向联动切换指标。
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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerValue(metrics[pagerState.currentPage], bars),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = range.weeksAgoLabel(bars),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            RangeDropdown(range = range, onRangeChange = onRangeChange)
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            WeeklyBarChart(
                bars = bars,
                metric = metrics[page],
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

/** 头部大数字：当前指标的范围内总量。 */
private fun headerValue(metric: MineChartMetric, bars: List<MineWeeklyBar>): String {
    return when (metric) {
        MineChartMetric.DURATION -> formatDurationHeader(bars.sumOf { it.durationMillis })
        MineChartMetric.VOLUME -> formatVolumeCompact(bars.sumOf { it.volumeKg }) + " kg"
        MineChartMetric.DAYS -> bars.sumOf { it.trainingDays }.toString() + " 天"
    }
}

private fun MineChartRange.weeksAgoLabel(bars: List<MineWeeklyBar>): String {
    return when (this) {
        MineChartRange.M1 -> "近 4 周"
        MineChartRange.M3 -> "近 13 周"
        MineChartRange.Y1 -> "近 1 年"
        MineChartRange.ALL -> "全期 ${bars.size} 周"
    }
}

/** 时长大数：2小时05分 / 45分钟。 */
private fun formatDurationHeader(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}小时${minutes}分"
        hours > 0 -> "${hours}小时"
        else -> "${minutes}分钟"
    }
}

/** 训练量超四位数用 k：15k；以内保持原格式。 */
internal fun formatVolumeCompact(volumeKg: Double): String {
    return if (volumeKg >= 10_000) {
        "${Math.round(volumeKg / 1_000)}k"
    } else {
        WeightLogic.formatKg(volumeKg)
    }
}

/** 右上角范围选择。 */
@Composable
private fun RangeDropdown(
    range: MineChartRange,
    onRangeChange: (MineChartRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(range.label, style = MaterialTheme.typography.labelLarge)
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MineChartRange.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                onClick = {
                    expanded = false
                    onRangeChange(option)
                },
            )
        }
    }
}

/**
 * 按周柱状图：Y 轴 0/半/满三档刻度，X 轴抽稀标注周起始日期。
 * 数据全为 0 时显示占位文案。
 */
@Composable
private fun WeeklyBarChart(
    bars: List<MineWeeklyBar>,
    metric: MineChartMetric,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val labelPx = with(density) { 10.dp.toPx() }

    val values = bars.map { bar ->
        when (metric) {
            MineChartMetric.DURATION -> bar.durationMillis / 3_600_000.0 // 小时
            MineChartMetric.VOLUME -> bar.volumeKg
            MineChartMetric.DAYS -> bar.trainingDays.toDouble()
        }
    }
    val maxValue = niceCeil(values.maxOrNull() ?: 0.0)

    Canvas(modifier = modifier) {
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

        fun label(v: Double): String = when (metric) {
            MineChartMetric.DURATION -> if (maxValue >= 1) "${v.toInt()}h" else "${(v * 60).toInt()}分"
            MineChartMetric.VOLUME -> if (v >= 10_000) "${(v / 1_000).toInt()}k" else WeightLogic.formatKg(v)
            MineChartMetric.DAYS -> v.toInt().toString()
        }

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
                label(maxValue * fraction),
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
            val left = labelSpaceLeft + slot * index + (slot - barWidth) / 2
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, yFor(value)),
                size = Size(barWidth, baseline - yFor(value)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
        }

        // X 轴：抽稀标注约 5 个周起始日期
        val labelCount = minOf(5, values.size)
        if (labelCount > 0) {
            val step = (values.size - 1).coerceAtLeast(1) / (labelCount - 1).coerceAtLeast(1)
            val indices = if (values.size == 1) listOf(0) else (0 until values.size step step).toList()
            indices.forEach { index ->
                val date = bars[index].weekStart
                val text = "${date.monthValue}/${date.dayOfMonth}"
                val x = labelSpaceLeft + slot * index + slot / 2
                // 让文字以刻度为中心
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
