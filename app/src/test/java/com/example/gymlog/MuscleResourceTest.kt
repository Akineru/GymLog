package com.example.gymlog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import com.example.gymlog.ui.muscle.MuscleMaskStore
import com.example.gymlog.ui.muscle.MuscleSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MuscleResourceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun catalogSpecsAndFilesCoverTheSameNineteenMuscles() {
        assertEquals(
            MuscleCatalog.all.map { it.id }.toSet(),
            MuscleMaskCatalog.specs.map { it.muscleId }.toSet(),
        )

        listOf(MuscleSex.MALE, MuscleSex.FEMALE).forEach { sex ->
            val frontFiles = pngFiles(sex, "front")
            val backFiles = pngFiles(sex, "back")
            assertEquals(13, frontFiles.size)
            assertEquals(14, backFiles.size)
            MuscleMaskCatalog.specs
                .filter { it.front }
                .forEach { assertTrue("${sex}/front/${it.muscleId}.png", "${it.muscleId}.png" in frontFiles) }
            MuscleMaskCatalog.specs
                .filter { it.back }
                .forEach { assertTrue("${sex}/back/${it.muscleId}.png", "${it.muscleId}.png" in backFiles) }
        }
    }

    @Test
    fun everyVisibleMaskHasPixels() {
        listOf(MuscleSex.MALE, MuscleSex.FEMALE).forEach { sex ->
            listOf(true, false).forEach { front ->
                val masks = MuscleMaskStore(context.assets, sex, front)
                masks.prepare()
                assertEquals(if (front) 13 else 14, masks.loadedCount())
                MuscleMaskCatalog.specs
                    .filter { if (front) it.front else it.back }
                    .forEach { spec ->
                        assertTrue(
                            "${sex} ${if (front) "front" else "back"}/${spec.muscleId} is empty",
                            masks.bitmap(spec.muscleId).opaqueCount > 0,
                        )
                    }
            }
        }
    }

    @Test
    fun baseFiguresUseTheSharedCanvas() {
        listOf(
            com.example.gymlog.R.drawable.muscle_front_base_male,
            com.example.gymlog.R.drawable.muscle_back_base_male,
            com.example.gymlog.R.drawable.muscle_front_base_female,
            com.example.gymlog.R.drawable.muscle_back_base_female,
        ).forEach { resId ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, bounds)
            assertEquals(1024, bounds.outWidth)
            assertEquals(1536, bounds.outHeight)
        }
    }

    /** 遮罩不透明像素必须落在底图不透明像素内——对齐回归测试。 */
    @Test
    fun masksStayInsideBase() {
        // 素材实测基线：male back/gastrocnemius 有 4px 边缘溢出（约 0.0003%），
        // 容差 8px 只拦截「明显错位/换错图」级别的回归，不卡素材边缘噪声。
        listOf(MuscleSex.MALE, MuscleSex.FEMALE).forEach { sex ->
            listOf(true, false).forEach { front ->
                val base = decodeBase(sex, front)
                val basePixels = IntArray(base.width * base.height)
                base.getPixels(basePixels, 0, base.width, 0, 0, base.width, base.height)

                val masks = MuscleMaskStore(context.assets, sex, front)
                masks.prepare()
                MuscleMaskCatalog.specs
                    .filter { if (front) it.front else it.back }
                    .forEach { spec ->
                        val mask = masks.bitmap(spec.muscleId)
                        assertEquals(
                            "${sex} ${side(front)}/${spec.muscleId} size",
                            1024 to 1536,
                            mask.bitmap.width to mask.bitmap.height,
                        )
                        val pixels = IntArray(mask.bitmap.width * mask.bitmap.height)
                        mask.bitmap.getPixels(pixels, 0, mask.bitmap.width, 0, 0, mask.bitmap.width, mask.bitmap.height)
                        var violations = 0
                        pixels.forEachIndexed { index, pixel ->
                            if ((pixel ushr 24) >= 16 && (basePixels[index] ushr 24) < 16) violations++
                        }
                        assertTrue(
                            "${sex} ${side(front)}/${spec.muscleId} overflows the base by $violations px",
                            violations <= MAX_OVERFLOW_PIXELS,
                        )
                    }
                if (!base.isRecycled) base.recycle()
            }
        }
    }

    private fun decodeBase(sex: MuscleSex, front: Boolean): Bitmap {
        val resId = when {
            front && sex == MuscleSex.MALE -> com.example.gymlog.R.drawable.muscle_front_base_male
            front -> com.example.gymlog.R.drawable.muscle_front_base_female
            sex == MuscleSex.MALE -> com.example.gymlog.R.drawable.muscle_back_base_male
            else -> com.example.gymlog.R.drawable.muscle_back_base_female
        }
        return context.resources.openRawResource(resId).use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            })
        } ?: error("missing base $resId")
    }

    private fun side(front: Boolean) = if (front) "front" else "back"

    private companion object {
        const val MAX_OVERFLOW_PIXELS = 8
    }

    private fun pngFiles(sex: MuscleSex, side: String): List<String> {
        return context.assets.list("muscle_masks/${sex.name.lowercase()}/$side")
            .orEmpty()
            .filter { it.endsWith(".png") }
    }
}
