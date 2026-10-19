package com.example.gymlog.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.max

/**
 * Pixel operations for the photo wall. Every function returns a new bitmap and
 * leaves [source] unchanged. Mosaic and blur write pixels into that result;
 * they are not overlay instructions.
 */
object PhotoImage {
    const val MAX_EDGE = 1600
    const val NOTE_LIMIT = 200

    fun degreesFromExif(orientation: Int): Int = when (orientation) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90,
        android.media.ExifInterface.ORIENTATION_TRANSPOSE,
        -> 90
        android.media.ExifInterface.ORIENTATION_ROTATE_180,
        android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL,
        -> 180
        android.media.ExifInterface.ORIENTATION_ROTATE_270,
        android.media.ExifInterface.ORIENTATION_TRANSVERSE,
        -> 270
        else -> 0
    }

    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val steps = ((degrees % 360) + 360) % 360
        if (steps == 0) return source.copy(Bitmap.Config.ARGB_8888, true)
        val matrix = Matrix().apply { postRotate(steps.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun crop(source: Bitmap, rect: Rect): Bitmap {
        val bounds = Rect(0, 0, source.width, source.height)
        val clipped = Rect(rect)
        if (!clipped.intersect(bounds) || clipped.width() < 1 || clipped.height() < 1) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }
        return Bitmap.createBitmap(source, clipped.left, clipped.top, clipped.width(), clipped.height())
    }

    fun stroke(source: Bitmap, points: List<Pair<Float, Float>>, color: Int, width: Float): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        if (points.size < 2) return out
        val radius = (width / 2f).toInt().coerceAtLeast(1)
        for (index in 1 until points.size) {
            stampSegment(out, points[index - 1], points[index], color, radius)
        }
        return out
    }

    private fun stampSegment(
        bitmap: Bitmap,
        start: Pair<Float, Float>,
        end: Pair<Float, Float>,
        color: Int,
        radius: Int,
    ) {
        val steps = max(
            kotlin.math.abs(end.first - start.first),
            kotlin.math.abs(end.second - start.second),
        ).toInt().coerceAtLeast(1)
        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val x = (start.first + (end.first - start.first) * t).toInt()
            val y = (start.second + (end.second - start.second) * t).toInt()
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val px = x + dx
                    val py = y + dy
                    if (px in 0 until bitmap.width && py in 0 until bitmap.height) {
                        bitmap.setPixel(px, py, color)
                    }
                }
            }
        }
    }

    fun label(source: Bitmap, text: String, x: Float, y: Float, color: Int, textSize: Float): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return out
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize.coerceAtLeast(12f)
        }
        val textX = x.coerceIn(0f, out.width.toFloat())
        val textY = y.coerceIn(paint.textSize, out.height.toFloat())
        canvas.drawText(trimmed, textX, textY, paint)
        out.setPixel(textX.toInt().coerceIn(0, out.width - 1), textY.toInt().coerceIn(0, out.height - 1), color)
        return out
    }

    /** Nearest-neighbour scale of [rect]. Pixels outside [rect] stay unchanged. */
    fun mosaic(source: Bitmap, rect: Rect, block: Int): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val area = clip(source, rect) ?: return out
        val step = block.coerceAtLeast(2)
        val sampleW = max(1, area.width() / step)
        val sampleH = max(1, area.height() / step)
        val region = Bitmap.createBitmap(source, area.left, area.top, area.width(), area.height())
        val small = Bitmap.createScaledBitmap(region, sampleW, sampleH, false)
        val pixelated = Bitmap.createScaledBitmap(small, area.width(), area.height(), false)
        Canvas(out).drawBitmap(pixelated, area.left.toFloat(), area.top.toFloat(), null)
        region.recycle()
        small.recycle()
        pixelated.recycle()
        return out
    }

    /** Box blur written into [rect]. Pixels outside [rect] stay unchanged. */
    fun blur(source: Bitmap, rect: Rect, radius: Int): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        val area = clip(source, rect) ?: return out
        val span = radius.coerceIn(1, 24)
        val width = area.width()
        val height = area.height()
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, area.left, area.top, width, height)
        val blurred = boxBlur(pixels, width, height, span)
        out.setPixels(blurred, 0, width, area.left, area.top, width, height)
        return out
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / sample > maxEdge) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private fun clip(source: Bitmap, rect: Rect): Rect? {
        val bounds = Rect(0, 0, source.width, source.height)
        val clipped = Rect(rect)
        if (!clipped.intersect(bounds) || clipped.width() < 2 || clipped.height() < 2) return null
        return clipped
    }

    private fun boxBlur(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val horizontal = IntArray(source.size)
        val vertical = IntArray(source.size)
        val window = radius * 2 + 1
        for (y in 0 until height) {
            for (x in 0 until width) {
                var alpha = 0
                var red = 0
                var green = 0
                var blue = 0
                for (offset in -radius..radius) {
                    val sample = source[y * width + (x + offset).coerceIn(0, width - 1)]
                    alpha += sample ushr 24
                    red += (sample shr 16) and 0xff
                    green += (sample shr 8) and 0xff
                    blue += sample and 0xff
                }
                horizontal[y * width + x] = pack(alpha / window, red / window, green / window, blue / window)
            }
        }
        for (x in 0 until width) {
            for (y in 0 until height) {
                var alpha = 0
                var red = 0
                var green = 0
                var blue = 0
                for (offset in -radius..radius) {
                    val sample = horizontal[(y + offset).coerceIn(0, height - 1) * width + x]
                    alpha += sample ushr 24
                    red += (sample shr 16) and 0xff
                    green += (sample shr 8) and 0xff
                    blue += sample and 0xff
                }
                vertical[y * width + x] = pack(alpha / window, red / window, green / window, blue / window)
            }
        }
        return vertical
    }

    private fun pack(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }
}

/** Relative paths may only point at the photo wall's own folders. */
object PhotoPaths {
    const val ORIGINALS = "originals"
    const val EDITED = "edited"

    fun isSafe(relative: String?): Boolean {
        if (relative.isNullOrBlank()) return false
        val normalized = relative.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains("..") || normalized.contains(':')) return false
        val folder = normalized.substringBefore('/', "")
        if (folder != ORIGINALS && folder != EDITED) return false
        val name = normalized.substringAfter('/', "")
        return name.isNotBlank() && !name.contains('/') && name.endsWith(".jpg")
    }

    fun original(id: String) = "$ORIGINALS/$id.jpg"

    fun edited(id: String) = "$EDITED/$id.jpg"
}
