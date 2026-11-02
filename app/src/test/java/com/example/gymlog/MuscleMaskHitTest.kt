package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import com.example.gymlog.ui.muscle.MuscleMaskStore
import com.example.gymlog.ui.muscle.MuscleSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MuscleMaskHitTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun quadricepsHighlightWinsTheHitTest() {
        val masks = MuscleMaskStore(context.assets, MuscleSex.MALE, front = true)
        masks.prepare()
        val quad = masks.bitmap("quadriceps")
        val x = quad.leftCenterX!!.toInt()
        val y = quad.leftCenterY!!.toInt()

        val layers = MuscleMaskCatalog.layers(
            listOf(MuscleHighlight("quadriceps", MuscleRole.PRIMARY, emptyList())),
        )
        assertEquals(listOf("quadriceps"), layers.map { it.muscleId })
        assertEquals("quadriceps_l", masks.hit(x, y, layers.map { it.muscleId }))
    }

    @Test
    fun plainChestHitReturnsTheSideRegion() {
        val masks = MuscleMaskStore(context.assets, MuscleSex.MALE, front = true)
        masks.prepare()
        val chest = masks.bitmap("pectoralis_major")
        val x = chest.leftCenterX!!.toInt()
        val y = chest.leftCenterY!!.toInt()

        val hit = masks.hit(x, y)
        assertNotNull(hit)
        assertEquals("pectoralis_major_l", hit)
    }

    @Test
    fun backMasksUseTheBackStore() {
        val masks = MuscleMaskStore(context.assets, MuscleSex.MALE, front = false)
        masks.prepare()
        val lat = masks.bitmap("latissimus_dorsi")
        val x = lat.leftCenterX!!.toInt()
        val y = lat.leftCenterY!!.toInt()

        assertEquals("latissimus_dorsi_l", masks.hit(x, y))
    }

    @Test
    fun femaleStoreServesTheFemaleDirectory() {
        val male = MuscleMaskStore(context.assets, MuscleSex.MALE, front = true)
        val female = MuscleMaskStore(context.assets, MuscleSex.FEMALE, front = true)
        male.prepare()
        female.prepare()
        val maleChest = male.bitmap("pectoralis_major")
        val femaleChest = female.bitmap("pectoralis_major")
        // 两套素材不同源，质心不可能逐像素一致；一致才说明加载错了目录
        org.junit.Assert.assertNotEquals(
            maleChest.centerX to maleChest.centerY,
            femaleChest.centerX to femaleChest.centerY,
        )
    }
}
