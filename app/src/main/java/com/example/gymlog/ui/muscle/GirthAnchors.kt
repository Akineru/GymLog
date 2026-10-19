package com.example.gymlog.ui.muscle

import com.example.gymlog.data.GirthSlot

internal data class GirthAnchorPoint(
    val slot: GirthSlot,
    val x: Float,
    val y: Float,
)

internal object GirthAnchors {
    fun resolve(masks: MuscleMaskStore): List<GirthAnchorPoint> {
        val chest = masks.bitmap("pectoralis_major")
        val arm = masks.bitmap("biceps_brachii")
        val forearm = masks.bitmap("forearm_flexors")
        val rectus = masks.bitmap("rectus_abdominis")
        val thigh = masks.bitmap("quadriceps")
        val calf = masks.bitmap("tibialis_anterior")
        val armPoint = band(GirthSlot.ARM, arm, 0.30f, 0.70f)
        val forearmMass = band(GirthSlot.FOREARM, forearm, 0.18f, 0.48f)
        val thighPoint = band(GirthSlot.THIGH, thigh, 0.22f, 0.52f)
        val calfMass = band(GirthSlot.CALF, calf, 0.42f, 0.72f)
        val waist = band(GirthSlot.WAIST, rectus, 0.62f, 0.92f)
        // Flexor pixels bunch at the hand, so the dot moves up the forearm toward the elbow.
        val forearmPoint = forearmMass.copy(
            y = armPoint.y + (forearmMass.y - armPoint.y) * 0.62f,
        )
        // Tibialis pixels start at the knee, so the dot moves down the shin.
        val calfPoint = calfMass.copy(
            y = calfMass.y + (calfMass.y - thighPoint.y) * 0.35f,
        )
        // The pelvis is the gap under the abs and above the thigh. The back glute sits lower.
        val hipY = (waist.y + (thighPoint.y - waist.y) * 0.28f).coerceAtMost(thighPoint.y - 1f)
        return listOf(
            GirthAnchorPoint(GirthSlot.CHEST, chest.centerX, chest.centerY),
            armPoint,
            forearmPoint,
            waist,
            GirthAnchorPoint(GirthSlot.HIP, waist.x, hipY),
            thighPoint,
            calfPoint,
        )
    }

    private fun band(slot: GirthSlot, mask: MaskBitmap, from: Float, to: Float): GirthAnchorPoint {
        val leftSide = if (slot == GirthSlot.WAIST) null else !slot.onRight
        val point = centroidInBand(mask, leftSide, from, to)
        if (point != null) return GirthAnchorPoint(slot, point.first, point.second)
        val x = if (slot.onRight) mask.rightCenterX else mask.leftCenterX
        val y = if (slot.onRight) mask.rightCenterY else mask.leftCenterY
        if (x == null || y == null || slot == GirthSlot.WAIST) {
            return GirthAnchorPoint(slot, mask.centerX, mask.centerY)
        }
        return GirthAnchorPoint(slot, x, y)
    }

    private fun sideRange(mask: MaskBitmap, leftSide: Boolean?): ClosedFloatingPointRange<Float>? {
        val width = mask.bitmap.width
        val height = mask.bitmap.height
        val mid = width / 2
        var minY = height
        var maxY = -1
        for (index in 0 until mask.opaque.size()) {
            if (!mask.opaque.get(index)) continue
            val x = index % width
            val y = index / width
            val inSide = when (leftSide) {
                null -> true
                true -> x < mid
                false -> x >= mid
            }
            if (!inSide) continue
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (maxY < minY) return null
        return minY.toFloat()..maxY.toFloat()
    }

    private fun centroidInBand(
        mask: MaskBitmap,
        leftSide: Boolean?,
        from: Float,
        to: Float,
    ): Pair<Float, Float>? {
        val range = sideRange(mask, leftSide) ?: return null
        val width = mask.bitmap.width
        val mid = width / 2
        val y0 = range.start + (range.endInclusive - range.start) * from
        val y1 = range.start + (range.endInclusive - range.start) * to
        var count = 0
        var sumX = 0.0
        var sumY = 0.0
        for (index in 0 until mask.opaque.size()) {
            if (!mask.opaque.get(index)) continue
            val x = index % width
            val y = index / width
            val inSide = when (leftSide) {
                null -> true
                true -> x < mid
                false -> x >= mid
            }
            if (!inSide || y < y0 || y > y1) continue
            count++
            sumX += x
            sumY += y
        }
        if (count == 0) return null
        return (sumX / count).toFloat() to (sumY / count).toFloat()
    }
}
