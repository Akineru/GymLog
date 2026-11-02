package com.example.gymlog

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.ui.muscle.MuscleAssetCache
import com.example.gymlog.ui.muscle.MuscleSex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MuscleAssetCacheTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MuscleAssetCache.resetForTesting()
    }

    @After
    fun tearDown() {
        MuscleAssetCache.resetForTesting()
    }

    @Test
    fun sameSideIsReusedAndBackLoadsIndependently() = runTest {
        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(context, front = true)
        val firstFront = MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front
        assertNotNull(firstFront)
        assertNotNull(firstFront!!.masks)
        assertEquals(7, MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.girthAnchors.size)
        assertNull("back must stay lazy until requested", MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.back)

        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(context, front = true)
        assertSame(firstFront, MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front)

        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(context, front = false)
        assertNotNull(MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.back)
        assertSame(firstFront, MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front)
    }

    @Test
    fun frontSideLoadTimeIsRecorded() = runTest {
        val started = System.nanoTime()
        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(context, front = true)
        val firstMs = (System.nanoTime() - started) / 1_000_000
        val reusedStarted = System.nanoTime()
        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(context, front = true)
        val reusedMs = (System.nanoTime() - reusedStarted) / 1_000_000
        println("MUSCLE_FRONT_LOAD_MS=$firstMs MUSCLE_FRONT_REUSE_MS=$reusedMs")
        assertNotNull(MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front)
    }

    @Test
    fun maskFailureKeepsTheBaseAndRetryRecoversMasks() = runTest {
        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(FailingAssetsContext(context), front = true)

        val degraded = MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front
        assertNotNull(degraded)
        assertNull(degraded!!.masks)
        assertFalse(MuscleAssetCache.forSex(MuscleSex.MALE).loading.value)

        MuscleAssetCache.forSex(MuscleSex.MALE).retry(context, front = true)

        val recovered = MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front
        assertNotNull(recovered)
        assertNotNull(recovered!!.masks)
        assertEquals(7, MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.girthAnchors.size)
    }

    @Test
    fun baseFailureIsReportedWithoutThrowing() = runTest {
        MuscleAssetCache.forSex(MuscleSex.MALE).loadSide(FailingResourcesContext(context), front = true)

        assertNull(MuscleAssetCache.forSex(MuscleSex.MALE).assets.value.front)
        assertNotNull(MuscleAssetCache.forSex(MuscleSex.MALE).errors.value[true])
        assertFalse(MuscleAssetCache.forSex(MuscleSex.MALE).loading.value)
    }

    private class FailingAssetsContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun getAssets(): AssetManager = error("assets unavailable")
    }

    private class FailingResourcesContext(base: Context) : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun getResources(): Resources = error("resources unavailable")
    }
}
