package com.example.gymlog

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.gymlog.ui.photo.PhotoCanvasMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoCanvasMathTest {

    @Test
    fun imagePointMapsExactlyWhenTheViewMatchesTheImage() {
        val point = PhotoCanvasMath.imagePoint(50f, 40f, IntSize(100, 100), 100, 100)
        assertEquals(50f, point!!.first, 0.001f)
        assertEquals(40f, point!!.second, 0.001f)
    }

    @Test
    fun imagePointAccountsForVerticalLetterboxing() {
        val view = IntSize(200, 400)
        val topLeft = PhotoCanvasMath.imagePoint(0f, 100f, view, 100, 100)
        assertEquals(0f, topLeft!!.first, 0.001f)
        assertEquals(0f, topLeft.second, 0.001f)
        val inside = PhotoCanvasMath.imagePoint(150f, 250f, view, 100, 100)
        assertEquals(75f, inside!!.first, 0.001f)
        assertEquals(75f, inside.second, 0.001f)
    }

    @Test
    fun imagePointAccountsForHorizontalLetterboxing() {
        val view = IntSize(400, 200)
        val topLeft = PhotoCanvasMath.imagePoint(100f, 0f, view, 100, 100)
        assertEquals(0f, topLeft!!.first, 0.001f)
        assertEquals(0f, topLeft.second, 0.001f)
    }

    @Test
    fun imagePointReturnsNullOutsideTheBitmap() {
        val view = IntSize(200, 400)
        assertNull(PhotoCanvasMath.imagePoint(100f, 50f, view, 100, 100))
        assertNull(PhotoCanvasMath.imagePoint(-1f, 200f, view, 100, 100))
        assertNull(PhotoCanvasMath.imagePoint(50f, 399f, view, 100, 100))
    }

    @Test
    fun imagePointReturnsNullForAnEmptyView() {
        assertNull(PhotoCanvasMath.imagePoint(5f, 5f, IntSize.Zero, 100, 100))
    }

    @Test
    fun bitmapRectNormalisesTheDragEndpoints() {
        val view = IntSize(100, 100)
        val rect = PhotoCanvasMath.bitmapRect(Offset(40f, 30f), Offset(10f, 10f), view, 100, 100)
        assertEquals(10, rect!!.left)
        assertEquals(10, rect.top)
        assertEquals(40, rect.right)
        assertEquals(30, rect.bottom)
    }

    @Test
    fun bitmapRectScalesThroughLetterboxing() {
        val rect = PhotoCanvasMath.bitmapRect(Offset(0f, 100f), Offset(40f, 140f), IntSize(200, 400), 100, 100)
        assertEquals(0, rect!!.left)
        assertEquals(0, rect.top)
        assertEquals(20, rect.right)
        assertEquals(20, rect.bottom)
    }

    @Test
    fun bitmapRectReturnsNullForTinyDrags() {
        val view = IntSize(100, 100)
        assertNull(PhotoCanvasMath.bitmapRect(Offset(10f, 10f), Offset(11f, 11f), view, 100, 100))
    }

    @Test
    fun bitmapRectReturnsNullWhenAnEndpointIsOutside() {
        val view = IntSize(100, 100)
        assertNull(PhotoCanvasMath.bitmapRect(Offset(-5f, 10f), Offset(30f, 30f), view, 100, 100))
    }
}
