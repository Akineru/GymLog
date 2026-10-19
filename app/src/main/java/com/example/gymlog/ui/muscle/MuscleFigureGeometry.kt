package com.example.gymlog.ui.muscle

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

/**
 * Shared source-to-canvas geometry for the base figure, every mask, hit testing
 * and the girth callouts. All image coordinates use the 1024x1536 asset canvas.
 */
internal data class FigureFrame(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    fun imageToCanvas(imageX: Float, imageY: Float): Offset {
        return Offset(
            x = left + imageX / MuscleMaskCatalog.WIDTH * width,
            y = top + imageY / MuscleMaskCatalog.HEIGHT * height,
        )
    }

    fun canvasToImage(canvasX: Float, canvasY: Float): Offset {
        return Offset(
            x = (canvasX - left) / width * MuscleMaskCatalog.WIDTH,
            y = (canvasY - top) / height * MuscleMaskCatalog.HEIGHT,
        )
    }
}

private const val LABEL_INSET = 0.16f

internal fun canvasAspect(callouts: Boolean): Float {
    val inset = if (callouts) LABEL_INSET else 0.02f
    val imageAspect = MuscleMaskCatalog.WIDTH.toFloat() / MuscleMaskCatalog.HEIGHT
    return imageAspect / (1f - inset * 2f)
}

internal fun frameFor(width: Float, height: Float, callouts: Boolean): FigureFrame {
    val inset = if (callouts) LABEL_INSET else 0.02f
    val availW = width * (1f - inset * 2f)
    val scale = min(
        availW / MuscleMaskCatalog.WIDTH,
        height / MuscleMaskCatalog.HEIGHT,
    )
    val dstW = (MuscleMaskCatalog.WIDTH * scale).toInt().coerceAtLeast(1)
    val dstH = (MuscleMaskCatalog.HEIGHT * scale).toInt().coerceAtLeast(1)
    return FigureFrame(
        left = ((width - dstW) / 2f).toInt(),
        top = ((height - dstH) / 2f).toInt(),
        width = dstW,
        height = dstH,
    )
}
