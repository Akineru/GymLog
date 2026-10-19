package com.example.gymlog

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.ui.muscle.MuscleAssetCache
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
        MuscleAssetCache.loadSide(context, front = true)
        val firstFront = MuscleAssetCache.assets.value.front
        assertNotNull(firstFront)
        assertNotNull(firstFront!!.masks)
        assertEquals(7, MuscleAssetCache.assets.value.girthAnchors.size)
        assertNull("back must stay lazy until requested", MuscleAssetCache.assets.value.back)

        MuscleAssetCache.loadSide(context, front = true)
        assertSame(firstFront, MuscleAssetCache.assets.value.front)

        MuscleAssetCache.loadSide(context, front = false)
        assertNotNull(MuscleAssetCache.assets.value.back)
        assertSame(firstFront, MuscleAssetCache.assets.value.front)
    }

    @Test
    fun frontSideLoadTimeIsRecorded() = runTest {
        val started = System.nanoTime()
        MuscleAssetCache.loadSide(context, front = true)
        val firstMs = (System.nanoTime() - started) / 1_000_000
        val reusedStarted = System.nanoTime()
        MuscleAssetCache.loadSide(context, front = true)
        val reusedMs = (System.nanoTime() - reusedStarted) / 1_000_000
        println("MUSCLE_FRONT_LOAD_MS=$firstMs MUSCLE_FRONT_REUSE_MS=$reusedMs")
        assertNotNull(MuscleAssetCache.assets.value.front)
    }

    @Test
    fun maskFailureKeepsTheBaseAndRetryRecoversMasks() = runTest {
        MuscleAssetCache.loadSide(FailingAssetsContext(context), front = true)

        val degraded = MuscleAssetCache.assets.value.front
        assertNotNull(degraded)
        assertNull(degraded!!.masks)
        assertFalse(MuscleAssetCache.loading.value)

        MuscleAssetCache.retry(context, front = true)

        val recovered = MuscleAssetCache.assets.value.front
        assertNotNull(recovered)
        assertNotNull(recovered!!.masks)
        assertEquals(7, MuscleAssetCache.assets.value.girthAnchors.size)
    }

    @Test
    fun baseFailureIsReportedWithoutThrowing() = runTest {
        MuscleAssetCache.loadSide(FailingResourcesContext(context), front = true)

        assertNull(MuscleAssetCache.assets.value.front)
        assertNotNull(MuscleAssetCache.errors.value[true])
        assertFalse(MuscleAssetCache.loading.value)
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
