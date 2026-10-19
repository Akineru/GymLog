package com.example.gymlog.ui.muscle

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.BitSet

internal class MaskBitmap(
    val bitmap: Bitmap,
    val image: ImageBitmap,
    val opaque: BitSet,
    val opaqueCount: Int,
    val centerX: Float,
    val centerY: Float,
    val leftCenterX: Float?,
    val leftCenterY: Float?,
    val rightCenterX: Float?,
    val rightCenterY: Float?,
)

/**
 * Decodes one side's visible masks once. Front and back each own a store so a
 * body-record screen never pays for the back-side masks until it needs them.
 */
internal class MuscleMaskStore(
    private val assets: AssetManager,
    private val front: Boolean,
) {
    private val cache = HashMap<String, MaskBitmap>()

    fun prepare() {
        MuscleMaskCatalog.specs.forEach { spec ->
            if (if (front) spec.front else spec.back) bitmap(spec.fileName)
        }
    }

    internal fun loadedCount(): Int = cache.size

    fun bitmap(fileName: String): MaskBitmap {
        return cache.getOrPut(fileName) { decode(fileName) }
    }

    /**
     * [preferredLayerIds] is the draw order used by [MuscleMaskCatalog.layers].
     * When a pixel belongs to several drawn layers, the top-most preferred layer
     * wins; this keeps the click target consistent with the visible highlight.
     * Without a preferred layer (no training highlight or selected overlay), the
     * smallest mask wins, with the quadriceps composite yielding to its heads.
     */
    fun hit(
        imageX: Int,
        imageY: Int,
        preferredLayerIds: List<String> = emptyList(),
    ): String? {
        if (imageX !in 0 until MuscleMaskCatalog.WIDTH || imageY !in 0 until MuscleMaskCatalog.HEIGHT) {
            return null
        }
        val index = imageY * MuscleMaskCatalog.WIDTH + imageX
        val hits = MuscleMaskCatalog.specs.mapNotNull { spec ->
            if (if (front) !spec.front else !spec.back) return@mapNotNull null
            val mask = bitmap(spec.fileName)
            if (!mask.opaque.get(index)) return@mapNotNull null
            spec.muscleId to mask.opaqueCount
        }
        if (hits.isEmpty()) return null
        val preferred = if (preferredLayerIds.isNotEmpty()) {
            val order = preferredLayerIds.withIndex().associate { it.value to it.index }
            hits.filter { it.first in order }.maxByOrNull { order[it.first] ?: -1 }
        } else {
            null
        }
        val components = hits.filter { it.first != "quadriceps" }
        val chosen = preferred
            ?: (components.ifEmpty { hits }).minBy { it.second }
        return MuscleMaskCatalog.regionId(chosen.first, imageX)
    }

    private fun decode(fileName: String): MaskBitmap {
        val side = if (front) "front" else "back"
        val decoded = assets.open("muscle_masks/$side/$fileName.png").use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Missing muscle mask muscle_masks/$side/$fileName.png")
        val measured = analyze(decoded)
        val alpha = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ALPHA_8)
        Canvas(alpha).drawBitmap(decoded, 0f, 0f, null)
        decoded.recycle()
        return MaskBitmap(
            bitmap = alpha,
            image = alpha.asImageBitmap(),
            opaque = measured.opaque,
            opaqueCount = measured.opaqueCount,
            centerX = measured.centerX,
            centerY = measured.centerY,
            leftCenterX = measured.leftCenterX,
            leftCenterY = measured.leftCenterY,
            rightCenterX = measured.rightCenterX,
            rightCenterY = measured.rightCenterY,
        )
    }

    private fun analyze(bitmap: Bitmap): MaskMeasure {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val opaque = BitSet(pixels.size)
        var count = 0
        var sumX = 0L
        var sumY = 0L
        var leftCount = 0
        var leftX = 0L
        var leftY = 0L
        var rightCount = 0
        var rightX = 0L
        var rightY = 0L
        val mid = width / 2
        pixels.forEachIndexed { index, pixel ->
            if ((pixel ushr 24) < ALPHA_HIT) return@forEachIndexed
            opaque.set(index)
            val x = index % width
            val y = index / width
            count++
            sumX += x
            sumY += y
            if (x < mid) {
                leftCount++
                leftX += x
                leftY += y
            } else {
                rightCount++
                rightX += x
                rightY += y
            }
        }
        return MaskMeasure(
            opaque = opaque,
            opaqueCount = count,
            centerX = if (count == 0) width / 2f else sumX.toFloat() / count,
            centerY = if (count == 0) height / 2f else sumY.toFloat() / count,
            leftCenterX = if (leftCount == 0) null else leftX.toFloat() / leftCount,
            leftCenterY = if (leftCount == 0) null else leftY.toFloat() / leftCount,
            rightCenterX = if (rightCount == 0) null else rightX.toFloat() / rightCount,
            rightCenterY = if (rightCount == 0) null else rightY.toFloat() / rightCount,
        )
    }

    private companion object {
        const val ALPHA_HIT = 16
    }
}

private class MaskMeasure(
    val opaque: BitSet,
    val opaqueCount: Int,
    val centerX: Float,
    val centerY: Float,
    val leftCenterX: Float?,
    val leftCenterY: Float?,
    val rightCenterX: Float?,
    val rightCenterY: Float?,
)
