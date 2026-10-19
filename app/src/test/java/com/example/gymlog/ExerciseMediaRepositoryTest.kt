package com.example.gymlog

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMediaEntity
import com.example.gymlog.data.ExerciseMediaRepository
import com.example.gymlog.data.ExerciseMediaStatus
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.ui.exercise.ExerciseMediaCache
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExerciseMediaRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: GymLogDatabase
    private lateinit var repository: ExerciseMediaRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExerciseMediaRepository(context, db.exerciseCatalogDao())
        runBlocking {
            db.exerciseDao().insertIgnore(
                listOf(
                    ExerciseEntity(
                        id = "ds_media",
                        name = "媒体动作",
                        bodyPart = "CHEST",
                        equipmentType = "FREE_WEIGHT",
                        weightMode = "WEIGHTED",
                        isCustom = false,
                        aliases = null,
                        createdAt = 1L,
                        source = ExerciseSource.DATASET,
                        sourceId = "media",
                    ),
                    ExerciseEntity(
                        id = "ds_no_media",
                        name = "无媒体动作",
                        bodyPart = "BACK",
                        equipmentType = "FREE_WEIGHT",
                        weightMode = "WEIGHTED",
                        isCustom = false,
                        aliases = null,
                        createdAt = 2L,
                        source = ExerciseSource.DATASET,
                        sourceId = "no-media",
                    ),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun missingFilesBecomePlaceholderStatesInsteadOfPaths() = runBlocking {
        db.exerciseCatalogDao().insertMedia(
            listOf(
                ExerciseMediaEntity(
                    exerciseId = "ds_media",
                    mediaId = "media",
                    sourceImagePath = "images/media.jpg",
                    sourceGifPath = "videos/media.gif",
                    width = 180,
                    height = 180,
                    downloadState = ExerciseMediaStatus.NOT_BUNDLED,
                ),
            ),
        )
        val media = repository.mediaForExercise("ds_media")
        assertNotNull(media)
        assertNull(media!!.thumbnailPath)
        assertNull(media.gifPath)
        assertFalse(media.hasThumbnail)
        assertFalse(media.hasGif)
        assertNull(repository.mediaForExercise("ds_no_media"))
    }

    @Test
    fun localLicensedFilesAreResolvedRelativeToTheAppMediaDirectory() = runBlocking {
        val mediaDir = File(context.filesDir, ExerciseMediaRepository.MEDIA_DIRECTORY)
        mediaDir.mkdirs()
        val thumbnail = File(mediaDir, "thumb.png")
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        thumbnail.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        db.exerciseCatalogDao().insertMedia(
            listOf(
                ExerciseMediaEntity(
                    exerciseId = "ds_media",
                    mediaId = "media",
                    sourceImagePath = null,
                    sourceGifPath = null,
                    localThumbnailPath = "thumb.png",
                    width = 180,
                    height = 180,
                    downloadState = ExerciseMediaStatus.BUNDLED,
                ),
            ),
        )
        val media = repository.mediaForExercise("ds_media")!!
        assertTrue(media.hasThumbnail)
        assertEquals(thumbnail.absolutePath, media.thumbnailPath)
        assertNotNull(ExerciseMediaCache.thumbnail(context, thumbnail.absolutePath, null))

        thumbnail.delete()
        val missing = repository.mediaForExercise("ds_media")!!
        assertNull(missing.thumbnailPath)
        assertFalse(missing.hasThumbnail)
    }

    @Test
    fun packagedAssetPathsAndThumbnailAreResolvedByStableExerciseId() = runBlocking {
        db.exerciseCatalogDao().insertMedia(
            listOf(
                ExerciseMediaEntity(
                    exerciseId = "ds_media",
                    mediaId = "2gPfomN",
                    sourceImagePath = "images/0001-2gPfomN.jpg",
                    sourceGifPath = "videos/0001-2gPfomN.gif",
                    width = 180,
                    height = 180,
                    downloadState = ExerciseMediaStatus.NOT_BUNDLED,
                ),
            ),
        )
        val media = repository.mediaForExercise("ds_media")!!
        assertTrue(media.hasThumbnail)
        assertTrue(media.hasGif)
        assertEquals("exercises/media/images/0001-2gPfomN.jpg", media.thumbnailAssetPath)
        assertEquals("exercises/media/videos/0001-2gPfomN.gif", media.gifAssetPath)
        assertNotNull(
            ExerciseMediaCache.thumbnail(context, null, media.thumbnailAssetPath),
        )
        val gifBytes = context.assets.open(media.gifAssetPath!!).use { it.readBytes() }
        assertTrue(gifBytes.size > 6)
        assertEquals("GIF89a", String(gifBytes.copyOfRange(0, 6), Charsets.US_ASCII))
        // Robolectric does not provide a real android.graphics.Movie decoder;
        // actual playback is verified only on a device/emulator.
    }

    @Test
    fun gifDecodeFailureIsReportedAsNoGifInsteadOfCrashing() = runBlocking {
        val mediaDir = File(context.filesDir, ExerciseMediaRepository.MEDIA_DIRECTORY)
        mediaDir.mkdirs()
        val brokenGif = File(mediaDir, "broken.gif")
        brokenGif.writeText("not a gif")
        db.exerciseCatalogDao().insertMedia(
            listOf(
                ExerciseMediaEntity(
                    exerciseId = "ds_media",
                    mediaId = "media",
                    sourceImagePath = null,
                    sourceGifPath = null,
                    localGifPath = "broken.gif",
                    width = 180,
                    height = 180,
                    downloadState = ExerciseMediaStatus.BUNDLED,
                ),
            ),
        )
        val media = repository.mediaForExercise("ds_media")!!
        assertTrue(media.hasGif)
        assertNull(ExerciseMediaCache.movie(context, media.gifPath!!, null))
    }

    @Test
    fun thumbnailAndGifAreIndependent() = runBlocking {
        val mediaDir = File(context.filesDir, ExerciseMediaRepository.MEDIA_DIRECTORY)
        mediaDir.mkdirs()
        val thumbnail = File(mediaDir, "only-thumb.png")
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        thumbnail.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        db.exerciseCatalogDao().insertMedia(
            listOf(
                ExerciseMediaEntity(
                    exerciseId = "ds_media",
                    mediaId = "media",
                    sourceImagePath = null,
                    sourceGifPath = null,
                    localThumbnailPath = "only-thumb.png",
                    localGifPath = "missing.gif",
                    width = 180,
                    height = 180,
                    downloadState = ExerciseMediaStatus.BUNDLED,
                ),
            ),
        )
        val media = repository.mediaForExercise("ds_media")!!
        assertTrue(media.hasThumbnail)
        assertFalse(media.hasGif)
        assertNull(media.gifPath)
    }

    private fun assertEquals(expected: String, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
