package com.example.gymlog

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.media.ExifInterface
import com.example.gymlog.data.PhotoImage
import com.example.gymlog.data.PhotoPaths
import com.example.gymlog.data.PhotoWallEntity
import com.example.gymlog.data.PhotoWallRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoImageTest {
    @Test
    fun mosaicWritesUniformBlocksAndLeavesTheOutsideAlone() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        for (y in 0 until 8) {
            for (x in 0 until 8) source.setPixel(x, y, Color.rgb(x * 30, y * 20, 40))
        }
        val outside = source.getPixel(7, 7)
        val result = PhotoImage.mosaic(source, Rect(0, 0, 4, 4), 4)
        assertEquals(outside, result.getPixel(7, 7))
        assertEquals(result.getPixel(0, 0), result.getPixel(3, 3))
        assertEquals(source.getPixel(7, 7), outside)
    }

    @Test
    fun blurChangesTheRegionAndKeepsTheOutside() {
        val source = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        source.setPixel(2, 2, Color.BLACK)
        val outside = source.getPixel(11, 11)
        val before = source.getPixel(3, 2)
        val result = PhotoImage.blur(source, Rect(0, 0, 6, 6), 2)
        assertEquals(outside, result.getPixel(11, 11))
        assertNotEquals(before, result.getPixel(3, 2))
        assertEquals(Color.WHITE, source.getPixel(11, 11))
    }

    @Test
    fun cropAndRotateChangeTheBitmapSize() {
        val source = Bitmap.createBitmap(10, 6, Bitmap.Config.ARGB_8888)
        val cropped = PhotoImage.crop(source, Rect(1, 1, 5, 4))
        assertEquals(4, cropped.width)
        assertEquals(3, cropped.height)
        val turned = PhotoImage.rotate(source, 90)
        assertEquals(6, turned.width)
        assertEquals(10, turned.height)
    }

    @Test
    fun strokeAndLabelChangePixels() {
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        val drawn = PhotoImage.stroke(source, listOf(2f to 2f, 10f to 2f), Color.RED, 3f)
        assertEquals(Color.RED, drawn.getPixel(6, 2))
        assertEquals(Color.WHITE, source.getPixel(6, 2))
        val labeled = PhotoImage.label(source, "记", 1f, 16f, Color.BLUE, 18f)
        assertEquals(source.width, labeled.width)
        var changed = false
        for (y in 0 until labeled.height) {
            for (x in 0 until labeled.width) {
                if (labeled.getPixel(x, y) != Color.WHITE) changed = true
            }
        }
        assertTrue(changed)
    }

    @Test
    fun exifDegreesAndUnsafePaths() {
        assertEquals(90, PhotoImage.degreesFromExif(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, PhotoImage.degreesFromExif(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, PhotoImage.degreesFromExif(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(0, PhotoImage.degreesFromExif(ExifInterface.ORIENTATION_NORMAL))
        assertTrue(PhotoPaths.isSafe("originals/abc.jpg"))
        assertTrue(PhotoPaths.isSafe("edited/abc.jpg"))
        assertFalse(PhotoPaths.isSafe("../originals/abc.jpg"))
        assertFalse(PhotoPaths.isSafe("/originals/abc.jpg"))
        assertFalse(PhotoPaths.isSafe("originals/a/b.jpg"))
        assertFalse(PhotoPaths.isSafe("cache/abc.jpg"))
    }

    @Test
    fun sameDayPhotosStayInCreatedOrderAndMonthFilterDoesNotDropThem() {
        val rows = listOf(
            photo("b", 20_000, 2),
            photo("a", 20_000, 3),
            photo("c", 19_990, 9),
        )
        val sorted = PhotoWallRepository.sort(rows)
        assertEquals(listOf("a", "b", "c"), sorted.map { it.id })
        val month = java.time.YearMonth.from(java.time.LocalDate.ofEpochDay(20_000))
        assertEquals(listOf("a", "b"), PhotoWallRepository.inMonth(rows, month).map { it.id })
    }

    private fun photo(id: String, day: Long, created: Long) = PhotoWallEntity(
        id = id,
        originalRelativePath = "originals/$id.jpg",
        editedRelativePath = "edited/$id.jpg",
        dateEpochDay = day,
        createdAt = created,
        updatedAt = created,
        note = "",
        width = 1,
        height = 1,
    )
}
