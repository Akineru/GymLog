package com.example.gymlog.ui.muscle

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.BodyGirths
import com.example.gymlog.data.GirthCopy
import com.example.gymlog.data.GirthSlot

@Composable
fun GirthCalloutOverlay(
    girths: BodyGirths,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val assets by MuscleAssetCache.assets.collectAsStateWithLifecycle()
    LaunchedEffect(context) { MuscleAssetCache.loadSide(context, front = true) }
    val anchors = assets.girthAnchors
    if (anchors.isEmpty()) return
    val missing = stringResource(R.string.mine_value_empty)
    val names = mapOf(
        GirthSlot.ARM to stringResource(R.string.body_label_arm),
        GirthSlot.FOREARM to stringResource(R.string.girth_forearm),
        GirthSlot.CHEST to stringResource(R.string.girth_chest),
        GirthSlot.WAIST to stringResource(R.string.body_label_waist),
        GirthSlot.HIP to stringResource(R.string.girth_hip),
        GirthSlot.THIGH to stringResource(R.string.girth_thigh),
        GirthSlot.CALF to stringResource(R.string.girth_calf),
    )
    val inputs = anchors.map { anchor ->
        val name = names.getValue(anchor.slot)
        GirthLabelInput(
            slot = anchor.slot,
            lines = listOf(name, GirthCopy.valueLines(anchor.slot, girths, missing)),
            imageX = anchor.x,
            imageY = anchor.y,
        )
    }
    val spoken = inputs.joinToString("\n") { it.lines.joinToString(" ") }
    val lineColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = spoken },
    ) {
        val canvasWidth = with(density) { maxWidth.toPx() }
        val canvasHeight = with(density) { maxHeight.toPx() }
        val placed = remember(inputs, canvasWidth, canvasHeight, density.density) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val aspect = canvasAspect(false)
            val figureWidth = canvasHeight * aspect
            val figureLeft = (canvasWidth - figureWidth) / 2f
            val frame = frameFor(figureWidth, canvasHeight, false)
            val frameLeft = figureLeft + frame.left
            val frameRight = frameLeft + frame.width
            val gutter = minOf(frameLeft, canvasWidth - frameRight)
            val measure = { textSize: Float, text: String ->
                paint.textSize = textSize
                paint.measureText(text)
            }
            var textPx = 8f * density.density
            for (sp in floatArrayOf(12f, 11f, 10f, 9f, 8f)) {
                val px = sp * density.density
                val widest = inputs.maxOf { input ->
                    input.lines.maxOf { line -> girthTextWidth(px, line, measure(px, line)) }
                }
                textPx = px
                if (widest <= gutter - 6f) break
            }
            val labels = placeGirthLabelsBesideFigure(
                labels = inputs,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                frameLeft = frameLeft,
                frameTop = frame.top.toFloat(),
                frameWidth = frame.width.toFloat(),
                frameHeight = frame.height.toFloat(),
                textSize = textPx,
                lineWidth = measure,
            )
            labels to textPx
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val (labels, textSize) = placed
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor.toArgb()
                this.textSize = textSize
            }
            labels.forEach { label ->
                drawLine(
                    color = lineColor,
                    start = Offset(label.lineStartX, label.lineStartY),
                    end = Offset(label.anchorX, label.anchorY),
                    strokeWidth = 1.6.dp.toPx(),
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(label.anchorX, label.anchorY),
                )
                label.lines.forEachIndexed { index, line ->
                    val baseline = label.textTop + textSize * (1f + index * 1.45f)
                    drawContext.canvas.nativeCanvas.drawText(line, label.textLeft, baseline, paint)
                }
            }
        }
    }
}
