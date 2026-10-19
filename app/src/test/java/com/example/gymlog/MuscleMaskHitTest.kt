package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import com.example.gymlog.ui.muscle.MuscleMaskStore
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
    fun drawnQuadricepsCompositeAlsoWinsTheHitTest() {
        val masks = MuscleMaskStore(context.assets, front = true)
        masks.prepare()
        val rectus = masks.bitmap("rectus_femoris")
        val x = rectus.leftCenterX!!.toInt()
        val y = rectus.leftCenterY!!.toInt()

        val layers = MuscleMaskCatalog.layers(
            listOf(
                MuscleHighlight("quadriceps", MuscleRole.PRIMARY, emptyList()),
                MuscleHighlight("rectus_femoris", MuscleRole.PRIMARY, emptyList()),
            ),
        )
        assertEquals(listOf("quadriceps"), layers.map { it.muscleId })
        assertEquals(
            "quadriceps",
            masks.hit(x, y, layers.map { it.muscleId }),
        )
        assertEquals(
            "rectus_femoris_l",
            masks.hit(x, y, listOf("rectus_femoris")),
        )
    }

    @Test
    fun plainChestHitReturnsTheSideRegion() {
        val masks = MuscleMaskStore(context.assets, front = true)
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
        val masks = MuscleMaskStore(context.assets, front = false)
        masks.prepare()
        val lat = masks.bitmap("latissimus_dorsi")
        val x = lat.leftCenterX!!.toInt()
        val y = lat.leftCenterY!!.toInt()

        assertEquals("latissimus_dorsi_l", masks.hit(x, y))
    }
}
