package com.example.gymlog.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Minimal HSV picker: hue + saturation wheel and a brightness slider.
 * The wheel is intentionally self-contained; callers only receive the color.
 */
@Composable
fun HsvColorPicker(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor.toArgb(), it) }
    }
    var hsv by remember(initialColor) { mutableStateOf(initialHsv) }
    var brightness by remember(initialColor) { mutableStateOf(initialHsv[2]) }
    val color = remember(hsv) { Color(AndroidColor.HSVToColor(hsv)) }

    LaunchedEffect(hsv[0], hsv[1], hsv[2]) {
        onColorChange(color)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = change.position.x - center.x
                        val dy = change.position.y - center.y
                        val radius = minOf(size.width, size.height) / 2f
                        val saturation = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        val hue = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
                        hsv = hsv.copyOf().also {
                            it[0] = hue
                            it[1] = saturation
                            it[2] = brightness
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red,
                        ),
                        center = center,
                    ),
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
                val markerRadius = radius * hsv[1]
                val angle = Math.toRadians(hsv[0].toDouble())
                val marker = Offset(
                    center.x + cos(angle).toFloat() * markerRadius,
                    center.y + sin(angle).toFloat() * markerRadius,
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = marker,
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = marker,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it
                hsv = hsv.copyOf().also { values -> values[2] = it }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}
