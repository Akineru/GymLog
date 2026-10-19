package com.example.gymlog

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import com.example.gymlog.ui.muscle.MuscleMaskStore
import org.json.JSONObject
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
    fun manifestCatalogAndFilesCoverTheSameThirtyThreeMuscles() {
        val manifest = readManifest()
        val rows = manifest.getJSONArray("muscles")
        assertEquals(33, rows.length())
        assertEquals(
            MuscleCatalog.all.map { it.id }.toSet(),
            MuscleMaskCatalog.specs.map { it.muscleId }.toSet(),
        )

        val frontFiles = pngFiles("front")
        val backFiles = pngFiles("back")
        assertEquals(33, frontFiles.size)
        assertEquals(33, backFiles.size)

        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            val hint = row.getString("muscleIdHint")
            val appId = if (hint == "deltoid_middle") "deltoid_lateral" else hint
            val spec = MuscleMaskCatalog.spec(appId)
            assertNotNull("missing spec for $appId", spec)
            val checked = spec!!
            assertEquals(row.getBoolean("frontVisible"), checked.front)
            assertEquals(row.getBoolean("backVisible"), checked.back)

            val frontName = row.getString("frontMask").substringAfterLast('/')
            val backName = row.getString("backMask").substringAfterLast('/')
            assertTrue("missing front/$frontName", frontName in frontFiles)
            assertTrue("missing back/$backName", backName in backFiles)
            assertEquals("${checked.fileName}.png", frontName)
            assertEquals("${checked.fileName}.png", backName)

            assertPngSize("front/$frontName")
            assertPngSize("back/$backName")
        }
    }

    @Test
    fun everyVisibleMaskHasPixels() {
        listOf(true, false).forEach { front ->
            val masks = MuscleMaskStore(context.assets, front)
            masks.prepare()
            assertEquals(if (front) 16 else 14, masks.loadedCount())
            MuscleMaskCatalog.specs
                .filter { if (front) it.front else it.back }
                .forEach { spec ->
                    assertTrue(
                        "${if (front) "front" else "back"}/${spec.fileName} is empty",
                        masks.bitmap(spec.fileName).opaqueCount > 0,
                    )
                }
        }
    }

    @Test
    fun baseFiguresUseTheSharedCanvas() {
        listOf(
            com.example.gymlog.R.drawable.muscle_front_base,
            com.example.gymlog.R.drawable.muscle_back_base,
        ).forEach { resId ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, bounds)
            assertEquals(1024, bounds.outWidth)
            assertEquals(1536, bounds.outHeight)
        }
    }

    private fun readManifest(): JSONObject {
        val text = context.assets.open("muscle_manifest.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun pngFiles(side: String): List<String> {
        return context.assets.list("muscle_masks/$side")
            .orEmpty()
            .filter { it.endsWith(".png") }
    }

    private fun assertPngSize(relative: String) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("muscle_masks/$relative").use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        assertEquals("$relative width", 1024, bounds.outWidth)
        assertEquals("$relative height", 1536, bounds.outHeight)
    }
}
