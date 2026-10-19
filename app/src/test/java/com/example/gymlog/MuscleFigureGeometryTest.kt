package com.example.gymlog

import com.example.gymlog.ui.muscle.canvasAspect
import com.example.gymlog.ui.muscle.frameFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleFigureGeometryTest {
    @Test
    fun frameKeepsTheSourceAspectAndStaysInsideTheCanvas() {
        val frame = frameFor(width = 360f, height = 460f, callouts = false)

        assertTrue(frame.left >= 0)
        assertTrue(frame.top >= 0)
        assertTrue(frame.left + frame.width <= 360)
        assertTrue(frame.top + frame.height <= 460)
        assertEquals(
            1024f / 1536f,
            frame.width.toFloat() / frame.height,
            0.01f,
        )
        assertEquals(1024f / 1536f / 0.96f, canvasAspect(false), 0.0001f)
    }

    @Test
    fun sourceAndCanvasCoordinatesRoundTrip() {
        val frame = frameFor(width = 360f, height = 460f, callouts = false)
        val canvas = frame.imageToCanvas(512f, 768f)
        val source = frame.canvasToImage(canvas.x, canvas.y)

        assertEquals(512f, source.x, 0.01f)
        assertEquals(768f, source.y, 0.01f)
    }

    @Test
    fun calloutFrameLeavesHorizontalRoomForLabels() {
        val plain = frameFor(width = 360f, height = 460f, callouts = false)
        val callouts = frameFor(width = 360f, height = 460f, callouts = true)

        assertTrue(callouts.width < plain.width)
        assertTrue(callouts.left > plain.left)
    }
}
