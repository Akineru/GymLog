package com.example.gymlog.ui.photo

import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Conversions between the editor canvas' view coordinates and bitmap pixel
 * coordinates. The preview draws the bitmap with
 * [androidx.compose.ui.layout.ContentScale.Fit], so the bitmap is letterboxed
 * inside the view; these helpers map through that same transform.
 */
internal object PhotoCanvasMath {

    /**
     * Maps a view point to a bitmap pixel, or returns null when the point lands
     * in the letterbox area outside the bitmap.
     */
    fun imagePoint(x: Float, y: Float, view: IntSize, imageW: Int, imageH: Int): Pair<Float, Float>? {
        if (view.width == 0 || view.height == 0) return null
        val scale = min(view.width / imageW.toFloat(), view.height / imageH.toFloat())
        val left = (view.width - imageW * scale) / 2f
        val top = (view.height - imageH * scale) / 2f
        val px = (x - left) / scale
        val py = (y - top) / scale
        if (px < 0f || py < 0f || px >= imageW || py >= imageH) return null
        return px to py
    }

    /**
     * Maps the two endpoints of a view drag to a bitmap pixel rect, or returns
     * null when either endpoint is outside the bitmap or the rect is too small
     * to matter.
     */
    fun bitmapRect(start: Offset, end: Offset, view: IntSize, imageW: Int, imageH: Int): Rect? {
        val a = imagePoint(start.x, start.y, view, imageW, imageH) ?: return null
        val b = imagePoint(end.x, end.y, view, imageW, imageH) ?: return null
        val left = minOf(a.first, b.first).roundToInt()
        val top = minOf(a.second, b.second).roundToInt()
        val right = maxOf(a.first, b.first).roundToInt()
        val bottom = maxOf(a.second, b.second).roundToInt()
        if (right - left < 2 || bottom - top < 2) return null
        return Rect(left, top, right, bottom)
    }
}
