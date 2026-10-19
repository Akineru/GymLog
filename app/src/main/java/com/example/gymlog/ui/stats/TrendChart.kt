package com.example.gymlog.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.BodyMetrics
import com.example.gymlog.data.TrendPoint
import com.example.gymlog.data.WeightLogic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

enum class TrendQuantity {
    KILOGRAM,
    PERCENT,
    INDEX,
}

fun TrendQuantity.minStep(): Double {
    return if (this == TrendQuantity.KILOGRAM) 2.5 else 0.5
}

fun TrendQuantity.unit(): String {
    return when (this) {
        TrendQuantity.KILOGRAM -> "kg"
        TrendQuantity.PERCENT -> "%"
        TrendQuantity.INDEX -> ""
    }
}

fun BodyTrendMetric.quantity(): TrendQuantity {
    return when (this) {
        BodyTrendMetric.WEIGHT -> TrendQuantity.KILOGRAM
        BodyTrendMetric.FAT -> TrendQuantity.PERCENT
        BodyTrendMetric.BMI -> TrendQuantity.INDEX
    }
}

fun TrendQuantity.number(value: Double): String {
    return when (this) {
        TrendQuantity.INDEX -> BodyMetrics.displayBmi(value)
        TrendQuantity.KILOGRAM, TrendQuantity.PERCENT -> WeightLogic.formatKg(value)
    }
}

internal fun yAxisTicks(
    minValue: Double,
    maxValue: Double,
    minStep: Double = 2.5,
): List<Double> {
    var step = minStep
    var low = alignDown(minValue, step)
    var high = alignUp(maxValue, step)
    if (high - low < minStep * 2) {
        low -= minStep
        high += minStep
    }
    while (high - low > step * 4) {
        step += minStep
        low = alignDown(minValue, step)
        high = alignUp(maxValue, step)
        if (high - low < step * 2) {
            low -= step
            high += step
        }
    }
    val ticks = ArrayList<Double>()
    var value = low
    while (value <= high + step * 0.001) {
        ticks += value
        value += step
    }
    return ticks
}

private fun alignDown(value: Double, step: Double): Double {
    return floor(value / step + 1e-9) * step
}

private fun alignUp(value: Double, step: Double): Double {
    return ceil(value / step - 1e-9) * step
}

internal data class PlotFrame(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal fun plotFrame(
    width: Float,
    height: Float,
    yLabelWidth: Float,
    dateHeight: Float,
    pointRadius: Float,
    labelGap: Float,
): PlotFrame {
    val top = max(pointRadius, dateHeight / 2f) + labelGap
    val bottom = height - dateHeight - labelGap - pointRadius
    val left = yLabelWidth + labelGap
    val right = width - pointRadius - labelGap
    return PlotFrame(
        left = left,
        top = top,
        right = right.coerceAtLeast(left + 1f),
        bottom = bottom.coerceAtLeast(top + 1f),
    )
}

internal fun pointOffsets(
    frame: PlotFrame,
    points: List<TrendPoint>,
    axisMin: Double,
    axisMax: Double,
): List<Offset> {
    if (points.isEmpty()) return emptyList()
    val axisRange = (axisMax - axisMin).takeIf { it > 0.0 } ?: 1.0
    val minX = points.minOf { it.dateEpochDay }
    val maxX = points.maxOf { it.dateEpochDay }
    val plotWidth = (frame.right - frame.left).coerceAtLeast(1f)
    val plotHeight = (frame.bottom - frame.top).coerceAtLeast(1f)
    return points.map { point ->
        val x = if (minX == maxX) {
            frame.left + plotWidth / 2f
        } else {
            frame.left + ((point.dateEpochDay - minX).toFloat() / (maxX - minX)) * plotWidth
        }
        val fraction = ((point.value - axisMin) / axisRange).toFloat()
        val y = frame.bottom - fraction * plotHeight
        Offset(x, y)
    }
}

private val axisDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

private val headerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

@Composable
fun TrendChart(
    points: List<TrendPoint>,
    quantity: TrendQuantity,
    modifier: Modifier = Modifier,
) {
    val points = remember(points) { points.sortedBy { it.dateEpochDay } }
    if (points.isEmpty()) {
        Text(
            text = stringResource(R.string.stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        return
    }
    var selectedDay by remember(points) { mutableStateOf<Long?>(null) }
    val shownIndex = points.indexOfLast { it.dateEpochDay == selectedDay }
        .let { index -> if (index >= 0) index else points.lastIndex }
    val shown = points[shownIndex]
    val showingLatest = shownIndex == points.lastIndex
    val number = quantity.number(shown.value)
    val unit = quantity.unit()
    val headerDate = LocalDate.ofEpochDay(shown.dateEpochDay).format(headerDateFormatter)
    val spokenValue = if (unit.isEmpty()) number else "$number $unit"
    val description = if (showingLatest) {
        stringResource(R.string.stats_latest, spokenValue) + "，$headerDate"
    } else {
        "$spokenValue，$headerDate"
    }
    val yTicks = yAxisTicks(points.minOf { it.value }, points.maxOf { it.value }, quantity.minStep())
    val axisMin = yTicks.first()
    val axisMax = yTicks.last()
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pointFill = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color = labelColor,
        fontFeatureSettings = "tnum",
    )
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val pointRadius = with(density) { 5.dp.toPx() }
    val selectedRadius = with(density) { 7.dp.toPx() }
    val pointInset = with(density) { 13.dp.toPx() }
    val hitRadius = with(density) { 24.dp.toPx() }
    val labelGap = with(density) { 14.dp.toPx() }
    val lineWidth = with(density) { 2.5.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            if (showingLatest) {
                Text(
                    text = stringResource(R.string.stats_latest_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                    )
                }
                Text(
                    text = headerDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(start = 12.dp, bottom = 3.dp)
                        .weight(1f),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(points, yTicks, pointInset, hitRadius, labelGap) {
                    val frame = frameFor(
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        yTicks = yTicks,
                        quantity = quantity,
                        style = labelStyle,
                        textMeasurer = textMeasurer,
                        pointRadius = pointInset,
                        labelGap = labelGap,
                    )
                    val positions = pointOffsets(frame, points, yTicks.first(), yTicks.last())
                    detectTapGestures { tap ->
                        val nearest = positions.indices.minByOrNull { index ->
                            (positions[index] - tap).getDistance()
                        }
                        selectedDay = nearest
                            ?.takeIf { (positions[it] - tap).getDistance() <= hitRadius }
                            ?.let { points[it].dateEpochDay }
                    }
                },
        ) {
            val frame = frameFor(
                width = size.width,
                height = size.height,
                yTicks = yTicks,
                quantity = quantity,
                style = labelStyle,
                textMeasurer = textMeasurer,
                pointRadius = pointInset,
                labelGap = labelGap,
            )
            val positions = pointOffsets(frame, points, axisMin, axisMax)
            yTicks.forEach { tick ->
                val fraction = ((tick - axisMin) / (axisMax - axisMin).coerceAtLeast(1e-6)).toFloat()
                val y = frame.bottom - fraction * (frame.bottom - frame.top)
                drawLine(
                    color = gridColor,
                    start = Offset(frame.left, y),
                    end = Offset(frame.right, y),
                    strokeWidth = 1.dp.toPx(),
                )
                val layout = textMeasurer.measure(quantity.number(tick), labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(frame.left - labelGap - layout.size.width, y - layout.size.height / 2f),
                )
            }
            if (positions.size >= 2) {
                val fill = Path().apply {
                    moveTo(positions.first().x, frame.bottom)
                    positions.forEach { lineTo(it.x, it.y) }
                    lineTo(positions.last().x, frame.bottom)
                    close()
                }
                drawPath(path = fill, color = lineColor.copy(alpha = 0.12f))
                val path = Path().apply {
                    moveTo(positions.first().x, positions.first().y)
                    positions.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = lineWidth, cap = StrokeCap.Round),
                )
            }
            positions.forEachIndexed { index, center ->
                val chosen = index == shownIndex
                val radius = if (chosen) selectedRadius else pointRadius
                if (chosen) {
                    drawCircle(color = lineColor.copy(alpha = 0.18f), radius = radius + 8.dp.toPx(), center = center)
                }
                drawCircle(color = pointFill, radius = radius + 1.5.dp.toPx(), center = center)
                drawCircle(color = lineColor, radius = radius, center = center)
            }
            drawDateLabels(
                points = points,
                positions = positions,
                frame = frame,
                style = labelStyle,
                textMeasurer = textMeasurer,
                labelGap = labelGap,
            )
        }
    }
}

private fun frameFor(
    width: Float,
    height: Float,
    yTicks: List<Double>,
    quantity: TrendQuantity,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    pointRadius: Float,
    labelGap: Float,
): PlotFrame {
    val yLabelWidth = yTicks.maxOf { tick ->
        textMeasurer.measure(quantity.number(tick), style).size.width
    }.toFloat()
    val dateHeight = textMeasurer.measure("12月31日", style).size.height.toFloat()
    return plotFrame(
        width = width,
        height = height,
        yLabelWidth = yLabelWidth,
        dateHeight = dateHeight,
        pointRadius = pointRadius,
        labelGap = labelGap,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDateLabels(
    points: List<TrendPoint>,
    positions: List<Offset>,
    frame: PlotFrame,
    style: TextStyle,
    textMeasurer: TextMeasurer,
    labelGap: Float,
) {
    val firstIndex = points.indices.minBy { points[it].dateEpochDay }
    val lastIndex = points.indices.maxBy { points[it].dateEpochDay }
    val firstText = LocalDate.ofEpochDay(points[firstIndex].dateEpochDay).format(axisDateFormatter)
    val firstLayout = textMeasurer.measure(firstText, style)
    val y = frame.bottom + labelGap
    if (firstIndex == lastIndex) {
        val x = (positions[firstIndex].x - firstLayout.size.width / 2f)
            .coerceIn(0f, size.width - firstLayout.size.width)
        drawText(firstLayout, topLeft = Offset(x, y))
        return
    }
    val lastText = LocalDate.ofEpochDay(points[lastIndex].dateEpochDay).format(axisDateFormatter)
    val lastLayout = textMeasurer.measure(lastText, style)
    var startX = positions[firstIndex].x - firstLayout.size.width / 2f
    var endX = positions[lastIndex].x - lastLayout.size.width / 2f
    startX = startX.coerceIn(0f, size.width - firstLayout.size.width)
    endX = endX.coerceIn(0f, size.width - lastLayout.size.width)
    if (startX + firstLayout.size.width + labelGap > endX) {
        startX = 0f
        endX = size.width - lastLayout.size.width
    }
    drawText(firstLayout, topLeft = Offset(startX, y))
    drawText(lastLayout, topLeft = Offset(endX, y))
}
