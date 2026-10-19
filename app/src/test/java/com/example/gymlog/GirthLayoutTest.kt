package com.example.gymlog

import android.graphics.Paint
import com.example.gymlog.data.BodyGirths
import com.example.gymlog.data.GirthCopy
import com.example.gymlog.data.GirthSlot
import com.example.gymlog.ui.muscle.GirthAnchorPoint
import com.example.gymlog.ui.muscle.GirthAnchors
import com.example.gymlog.ui.muscle.GirthLabelInput
import com.example.gymlog.ui.muscle.MuscleMaskStore
import com.example.gymlog.ui.muscle.canvasAspect
import com.example.gymlog.ui.muscle.frameFor
import com.example.gymlog.ui.muscle.pickGirthPlacement
import com.example.gymlog.ui.muscle.placeGirthLabelsBesideFigure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GirthLayoutTest {
    @Test
    fun missingGirthStaysBlankAndSidesStayIndependent() {
        val empty = GirthCopy.valueLines(GirthSlot.ARM, BodyGirths())
        assertEquals("左 -- / 右 -- cm", empty)
        val oneSide = GirthCopy.valueLines(
            GirthSlot.CALF,
            BodyGirths(calfLeftCm = 36.5, calfRightCm = null),
        )
        assertEquals("左 36.5 / 右 -- cm", oneSide)
        assertEquals("98 cm", GirthCopy.valueLines(GirthSlot.CHEST, BodyGirths(chestCm = 98.0)))
        assertEquals("-- cm", GirthCopy.valueLines(GirthSlot.WAIST, BodyGirths()))
    }

    @Test
    fun anchorsFollowTheFrontBodyAndDoNotOverlapWhenPlaced() {
        val masks = MuscleMaskStore(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().assets,
            front = true,
        )
        masks.prepare()
        val anchors = GirthAnchors.resolve(masks)
        assertEquals(GirthSlot.entries.toSet(), anchors.map { it.slot }.toSet())
        val bySlot = anchors.associateBy { it.slot }
        assertTrue(describe(anchors), y(bySlot, GirthSlot.ARM) < y(bySlot, GirthSlot.FOREARM))
        assertTrue(describe(anchors), y(bySlot, GirthSlot.CHEST) < y(bySlot, GirthSlot.WAIST))
        assertTrue(describe(anchors), y(bySlot, GirthSlot.WAIST) < y(bySlot, GirthSlot.HIP))
        assertTrue(describe(anchors), y(bySlot, GirthSlot.HIP) < y(bySlot, GirthSlot.THIGH))
        assertTrue(describe(anchors), y(bySlot, GirthSlot.THIGH) < y(bySlot, GirthSlot.CALF))

        val chest = masks.bitmap("pectoralis_major")
        assertEquals(chest.centerX, bySlot.getValue(GirthSlot.CHEST).x, 0.01f)
        assertEquals(chest.centerY, bySlot.getValue(GirthSlot.CHEST).y, 0.01f)
        assertTrue(bySlot.getValue(GirthSlot.ARM).x < 512f)
        assertTrue(bySlot.getValue(GirthSlot.FOREARM).x < 512f)
        assertTrue(bySlot.getValue(GirthSlot.THIGH).x > 512f)
        val forearm = masks.bitmap("forearm_flexors")
        assertTrue(
            "forearm should sit above the hand ${bySlot.getValue(GirthSlot.FOREARM).y} vs ${forearm.leftCenterY}",
            bySlot.getValue(GirthSlot.FOREARM).y < forearm.leftCenterY!!,
        )
        val calf = masks.bitmap("tibialis_anterior")
        assertTrue(
            "calf should sit below the knee ${bySlot.getValue(GirthSlot.CALF).y} vs ${calf.leftCenterY}",
            bySlot.getValue(GirthSlot.CALF).y > calf.leftCenterY!!,
        )

        val names = mapOf(
            GirthSlot.ARM to "臂围",
            GirthSlot.FOREARM to "小臂",
            GirthSlot.CHEST to "胸围",
            GirthSlot.WAIST to "腰围",
            GirthSlot.HIP to "臀围",
            GirthSlot.THIGH to "腿围",
            GirthSlot.CALF to "小腿",
        )
        val girths = BodyGirths(
            armLeftCm = 32.0,
            armRightCm = 33.0,
            forearmLeftCm = 26.0,
            chestCm = 98.0,
            waistCm = 80.0,
            thighRightCm = 54.0,
        )
        val labels = anchors.map { anchor ->
            GirthLabelInput(
                slot = anchor.slot,
                lines = listOf(names.getValue(anchor.slot), GirthCopy.valueLines(anchor.slot, girths)),
                imageX = anchor.x,
                imageY = anchor.y,
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val placed = pickGirthPlacement(
            labels = labels,
            canvasWidth = 926f,
            density = 2.75f,
            lineWidth = { textSize, text ->
                paint.textSize = textSize
                paint.measureText(text)
            },
        )
        assertTrue("text did not fit ${placed.textSize}", placed.fits)
        placed.labels.forEach { label ->
            assertTrue(label.textTop >= 0f)
            assertTrue(label.textBottom <= placed.canvasHeight + 0.5f)
            assertTrue(label.anchorX in placed.frameLeft..placed.frameLeft + placed.frameWidth)
            assertTrue(label.anchorY in placed.frameTop..placed.frameTop + placed.frameHeight)
            if (label.slot.onRight) {
                assertTrue(label.textLeft + 1f >= placed.frameLeft + placed.frameWidth)
            } else {
                assertTrue(label.textRight - 1f <= placed.frameLeft)
            }
        }
        val boxes = placed.labels
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val a = boxes[i]
                val b = boxes[j]
                val separated = a.textRight <= b.textLeft + 1f ||
                    b.textRight <= a.textLeft + 1f ||
                    a.textBottom + 4f <= b.textTop ||
                    b.textBottom + 4f <= a.textTop
                assertTrue("${a.slot} overlaps ${b.slot}", separated)
            }
        }
        placed.labels.forEach { label ->
            placed.labels.filter { it.slot != label.slot }.forEach { other ->
                assertTrue(
                    "${label.slot} line crosses ${other.slot}",
                    !segmentHitsRect(
                        label.lineStartX,
                        label.lineStartY,
                        label.anchorX,
                        label.anchorY,
                        other.textLeft,
                        other.textTop,
                        other.textRight,
                        other.textBottom,
                    ),
                )
            }
        }
        val arm = placed.labels.first { it.slot == GirthSlot.ARM }
        val source = bySlot.getValue(GirthSlot.ARM)
        val expectedX = placed.frameLeft + source.x / 1024f * placed.frameWidth
        val expectedY = placed.frameTop + source.y / 1536f * placed.frameHeight
        assertEquals(expectedX, arm.anchorX, 0.5f)
        assertEquals(expectedY, arm.anchorY, 0.5f)
        assertTrue(abs(arm.lineStartX - arm.anchorX) > 8f)
    }

    @Test
    fun actualGirthLayoutFitsCommonPhoneWidths() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val masks = MuscleMaskStore(context.assets, front = true)
        masks.prepare()
        val anchors = GirthAnchors.resolve(masks)
        val names = mapOf(
            GirthSlot.ARM to "臂围",
            GirthSlot.FOREARM to "小臂",
            GirthSlot.CHEST to "胸围",
            GirthSlot.WAIST to "腰围",
            GirthSlot.HIP to "臀围",
            GirthSlot.THIGH to "腿围",
            GirthSlot.CALF to "小腿",
        )
        val girths = BodyGirths(
            armLeftCm = 32.0,
            armRightCm = 33.0,
            forearmLeftCm = 26.0,
            chestCm = 98.0,
            waistCm = 80.0,
            thighRightCm = 54.0,
        )
        val inputs = anchors.map { anchor ->
            GirthLabelInput(
                slot = anchor.slot,
                lines = listOf(names.getValue(anchor.slot), GirthCopy.valueLines(anchor.slot, girths)),
                imageX = anchor.x,
                imageY = anchor.y,
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val density = 3f
        val textSize = 8f * density
        val aspect = canvasAspect(false)
        val canvasHeight = 460f * density
        listOf(320, 360, 411).forEach { widthDp ->
            val canvasWidth = widthDp * density
            val figureWidth = canvasHeight * aspect
            val figureLeft = (canvasWidth - figureWidth) / 2f
            val frame = frameFor(figureWidth, canvasHeight, false)
            val placed = placeGirthLabelsBesideFigure(
                labels = inputs,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                frameLeft = figureLeft + frame.left,
                frameTop = frame.top.toFloat(),
                frameWidth = frame.width.toFloat(),
                frameHeight = frame.height.toFloat(),
                textSize = textSize,
                lineWidth = { size, text ->
                    paint.textSize = size
                    paint.measureText(text)
                },
            )
            placed.forEach { label ->
                assertTrue("$widthDp text left", label.textLeft >= 0f)
                assertTrue("$widthDp text right", label.textRight <= canvasWidth + 0.5f)
                assertTrue("$widthDp text top", label.textTop >= 0f)
                assertTrue("$widthDp text bottom", label.textBottom <= canvasHeight + 0.5f)
                assertTrue(
                    "$widthDp anchor x",
                    label.anchorX in (figureLeft + frame.left)..(figureLeft + frame.left + frame.width),
                )
                assertTrue(
                    "$widthDp anchor y",
                    label.anchorY in frame.top.toFloat()..(frame.top + frame.height).toFloat(),
                )
            }
            for (i in placed.indices) {
                for (j in i + 1 until placed.size) {
                    val a = placed[i]
                    val b = placed[j]
                    val separated = a.textRight <= b.textLeft + 1f ||
                        b.textRight <= a.textLeft + 1f ||
                        a.textBottom + 4f <= b.textTop ||
                        b.textBottom + 4f <= a.textTop
                    assertTrue("$widthDp ${a.slot} overlaps ${b.slot}", separated)
                }
            }
        }
    }

    private fun segmentHitsRect(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): Boolean {
        val steps = 32
        for (step in 0..steps) {
            val t = step / steps.toFloat()
            val x = x1 + (x2 - x1) * t
            val y = y1 + (y2 - y1) * t
            if (x in left..right && y in top..bottom) return true
        }
        return false
    }

    private fun y(points: Map<GirthSlot, GirthAnchorPoint>, slot: GirthSlot): Float = points.getValue(slot).y

    private fun describe(anchors: List<GirthAnchorPoint>): String {
        return anchors.joinToString { "${it.slot}=${it.x.toInt()},${it.y.toInt()}" }
    }
}
