package com.example.gymlog.ui.photo

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * The editor's image stage. Owns gesture routing for the active [tool]:
 * tap-to-place text, freehand draw, and drag-rectangle crop / mosaic / blur.
 * All view coordinates are converted to bitmap pixels through
 * [PhotoCanvasMath]; callbacks receive bitmap-space values.
 */
@Composable
internal fun PhotoEditorCanvas(
    bitmap: Bitmap,
    tool: PhotoTool,
    pendingText: String,
    onPlaceText: (Float, Float) -> Unit,
    onDraw: (List<Pair<Float, Float>>) -> Unit,
    onCrop: (Rect) -> Unit,
    onMosaic: (Rect) -> Unit,
    onBlur: (Rect) -> Unit,
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var stroke by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 420.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1726))
            .onSizeChanged { viewSize = it }
            .pointerInput(bitmap, tool, viewSize, pendingText) {
                if (tool == PhotoTool.TEXT && pendingText.isNotBlank()) {
                    detectTapGestures { offset ->
                        val point = PhotoCanvasMath.imagePoint(offset.x, offset.y, viewSize, bitmap.width, bitmap.height)
                        if (point != null) {
                            onPlaceText(point.first, point.second)
                        }
                    }
                } else if (tool == PhotoTool.DRAW) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val point = PhotoCanvasMath.imagePoint(offset.x, offset.y, viewSize, bitmap.width, bitmap.height)
                            stroke = listOfNotNull(point)
                        },
                        onDrag = { change, _ ->
                            val point = PhotoCanvasMath.imagePoint(change.position.x, change.position.y, viewSize, bitmap.width, bitmap.height)
                            if (point != null) stroke = stroke + point
                        },
                        onDragEnd = {
                            onDraw(stroke)
                            stroke = emptyList()
                        },
                    )
                } else {
                    detectDragGestures(
                        onDragStart = {
                            dragStart = it
                            dragEnd = it
                        },
                        onDrag = { change, _ -> dragEnd = change.position },
                        onDragEnd = {
                            val start = dragStart
                            val end = dragEnd
                            dragStart = null
                            dragEnd = null
                            if (start == null || end == null) return@detectDragGestures
                            val rect = PhotoCanvasMath.bitmapRect(start, end, viewSize, bitmap.width, bitmap.height)
                                ?: return@detectDragGestures
                            when (tool) {
                                PhotoTool.CROP -> onCrop(rect)
                                PhotoTool.MOSAIC -> onMosaic(rect)
                                PhotoTool.BLUR -> onBlur(rect)
                                else -> Unit
                            }
                        },
                    )
                }
            },
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        val start = dragStart
        val end = dragEnd
        if (start != null && end != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    color = Color(0x88FFFFFF),
                    topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                    size = Size(
                        kotlin.math.abs(end.x - start.x),
                        kotlin.math.abs(end.y - start.y),
                    ),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}
