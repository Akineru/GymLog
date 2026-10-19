package com.example.gymlog.ui.muscle

import com.example.gymlog.data.GirthSlot

internal data class GirthLabelInput(
    val slot: GirthSlot,
    val lines: List<String>,
    val imageX: Float,
    val imageY: Float,
)

internal data class PlacedGirthLabel(
    val slot: GirthSlot,
    val lines: List<String>,
    val anchorX: Float,
    val anchorY: Float,
    val textLeft: Float,
    val textTop: Float,
    val textWidth: Float,
    val textHeight: Float,
    val lineStartX: Float,
    val lineStartY: Float,
) {
    val textRight: Float get() = textLeft + textWidth
    val textBottom: Float get() = textTop + textHeight
}

internal data class GirthPlacement(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val frameLeft: Float,
    val frameTop: Float,
    val frameWidth: Float,
    val frameHeight: Float,
    val textSize: Float,
    val labels: List<PlacedGirthLabel>,
    val fits: Boolean,
)

internal fun pickGirthPlacement(
    labels: List<GirthLabelInput>,
    canvasWidth: Float,
    density: Float,
    lineWidth: (textSize: Float, text: String) -> Float,
): GirthPlacement {
    val sizes = floatArrayOf(13f, 12f, 11f, 10.5f, 10f, 9.5f, 9f, 8.5f)
    var last = placeGirthLabels(labels, canvasWidth, sizes.last() * density, lineWidth)
    for (sp in sizes) {
        val placed = placeGirthLabels(labels, canvasWidth, sp * density, lineWidth)
        last = placed
        if (placed.fits) return placed
    }
    return last
}

internal fun placeGirthLabels(
    labels: List<GirthLabelInput>,
    canvasWidth: Float,
    textSize: Float,
    lineWidth: (textSize: Float, text: String) -> Float,
    imageWidth: Float = MuscleMaskCatalog.WIDTH.toFloat(),
    imageHeight: Float = MuscleMaskCatalog.HEIGHT.toFloat(),
): GirthPlacement {
    val sidePad = 8f
    val labelGap = 8f
    val figureGap = 12f
    val maxText = labels.maxOfOrNull { input ->
        input.lines.maxOfOrNull { line ->
            girthTextWidth(textSize, line, lineWidth(textSize, line))
        } ?: 0f
    } ?: 0f
    val gutter = sidePad + maxText + figureGap
    val figureWidth = canvasWidth - gutter * 2f
    val minFigure = canvasWidth * 0.26f
    val fits = figureWidth >= minFigure && maxText > 0f && canvasWidth > 0f
    val drawnFigure = figureWidth.coerceAtLeast(1f)
    val figureHeight = drawnFigure * imageHeight / imageWidth
    val frameLeft = if (fits) gutter else (canvasWidth - drawnFigure) / 2f

    data class LocalLabel(val input: GirthLabelInput, val anchorLocalY: Float, val height: Float)

    val locals = labels.map { input ->
        LocalLabel(
            input = input,
            anchorLocalY = input.imageY / imageHeight * figureHeight,
            height = blockHeight(input.lines.size, textSize),
        )
    }
    val tops = HashMap<GirthSlot, Float>()
    locals.groupBy { it.input.slot.onRight }.values.forEach { side ->
        var cursor = Float.NEGATIVE_INFINITY
        side.sortedBy { it.anchorLocalY }.forEach { item ->
            var top = item.anchorLocalY - item.height / 2f
            if (top < cursor) top = cursor
            tops[item.input.slot] = top
            cursor = top + item.height + labelGap
        }
    }
    val minTop = tops.values.minOrNull()?.coerceAtMost(0f) ?: 0f
    val maxBottom = locals.maxOfOrNull { item ->
        (tops[item.input.slot] ?: 0f) + item.height
    }?.coerceAtLeast(figureHeight) ?: figureHeight
    val pad = 8f
    val frameTop = pad - minTop
    val canvasHeight = frameTop + maxBottom + pad
    val placed = locals.map { item ->
        val top = frameTop + (tops[item.input.slot] ?: 0f)
        val anchorX = frameLeft + item.input.imageX / imageWidth * drawnFigure
        val anchorY = frameTop + item.anchorLocalY
        val textLeft = if (item.input.slot.onRight) {
            canvasWidth - sidePad - maxText
        } else {
            sidePad
        }
        val lineY = top + item.height / 2f
        val lineX = if (item.input.slot.onRight) textLeft - 6f else textLeft + maxText + 6f
        PlacedGirthLabel(
            slot = item.input.slot,
            lines = item.input.lines,
            anchorX = anchorX,
            anchorY = anchorY,
            textLeft = textLeft,
            textTop = top,
            textWidth = maxText,
            textHeight = item.height,
            lineStartX = lineX,
            lineStartY = lineY,
        )
    }
    return GirthPlacement(
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        frameLeft = frameLeft,
        frameTop = frameTop,
        frameWidth = drawnFigure,
        frameHeight = figureHeight,
        textSize = textSize,
        labels = placed,
        fits = fits,
    )
}

internal fun girthTextWidth(textSize: Float, text: String, measured: Float): Float {
    var estimate = 0f
    text.forEach { ch ->
        estimate += when {
            ch.code >= 0x2E80 -> textSize
            ch == ' ' -> textSize * 0.33f
            else -> textSize * 0.62f
        }
    }
    return maxOf(measured, estimate)
}

internal fun placeGirthLabelsBesideFigure(
    labels: List<GirthLabelInput>,
    canvasWidth: Float,
    canvasHeight: Float,
    frameLeft: Float,
    frameTop: Float,
    frameWidth: Float,
    frameHeight: Float,
    textSize: Float,
    lineWidth: (Float, String) -> Float,
    imageWidth: Float = MuscleMaskCatalog.WIDTH.toFloat(),
    imageHeight: Float = MuscleMaskCatalog.HEIGHT.toFloat(),
): List<PlacedGirthLabel> {
    val edge = 2f
    val measured = labels.maxOfOrNull { input ->
        input.lines.maxOfOrNull { line ->
            girthTextWidth(textSize, line, lineWidth(textSize, line))
        } ?: 0f
    } ?: 0f
    val textWidth = measured.coerceAtLeast(textSize)
    data class Local(val input: GirthLabelInput, val anchorY: Float, val height: Float)
    val locals = labels.map { input ->
        Local(
            input = input,
            anchorY = frameTop + input.imageY / imageHeight * frameHeight,
            height = blockHeight(input.lines.size, textSize),
        )
    }
    val tops = HashMap<GirthSlot, Float>()
    locals.groupBy { it.input.slot.onRight }.values.forEach { side ->
        var cursor = 0f
        side.sortedBy { it.anchorY }.forEach { item ->
            var top = item.anchorY - item.height / 2f
            if (top < cursor) top = cursor
            tops[item.input.slot] = top
            cursor = top + item.height + 6f
        }
    }
    val maxBottom = locals.maxOfOrNull { item ->
        (tops[item.input.slot] ?: 0f) + item.height
    } ?: 0f
    val shift = (maxBottom - canvasHeight).coerceAtLeast(0f)
    return locals.map { item ->
        val top = ((tops[item.input.slot] ?: 0f) - shift).coerceAtLeast(0f)
        val anchorX = frameLeft + item.input.imageX / imageWidth * frameWidth
        val textLeft = if (item.input.slot.onRight) canvasWidth - edge - textWidth else edge
        val lineY = top + item.height / 2f
        val lineX = if (item.input.slot.onRight) textLeft - 4f else textLeft + textWidth + 4f
        PlacedGirthLabel(
            slot = item.input.slot,
            lines = item.input.lines,
            anchorX = anchorX,
            anchorY = item.anchorY,
            textLeft = textLeft,
            textTop = top,
            textWidth = textWidth,
            textHeight = item.height,
            lineStartX = lineX,
            lineStartY = lineY,
        )
    }
}

private fun blockHeight(lineCount: Int, textSize: Float): Float {
    if (lineCount <= 0) return 0f
    return textSize * (1.25f + (lineCount - 1) * 1.45f)
}
