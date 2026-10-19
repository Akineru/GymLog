package com.example.gymlog

import com.example.gymlog.data.TrendPoint
import com.example.gymlog.ui.stats.plotFrame
import com.example.gymlog.ui.stats.pointOffsets
import com.example.gymlog.ui.stats.yAxisTicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartFrameTest {
    @Test
    fun singlePointSitsInsideTheCanvas() {
        val frame = plotFrame(
            width = 400f,
            height = 300f,
            yLabelWidth = 40f,
            dateHeight = 20f,
            pointRadius = 8f,
            labelGap = 8f,
        )
        val ticks = yAxisTicks(80.0, 80.0)
        val point = pointOffsets(
            frame,
            listOf(TrendPoint(100, 80.0)),
            ticks.first(),
            ticks.last(),
        ).single()
        assertEquals((frame.left + frame.right) / 2f, point.x, 0.01f)
        assertTrue(point.y - 8f >= 0f)
        assertTrue(point.y + 8f <= 300f - 20f)
    }

    @Test
    fun extremeValuesStayInsideThePlot() {
        val frame = plotFrame(400f, 300f, 48f, 24f, 10f, 8f)
        val ticks = yAxisTicks(60.0, 100.0)
        val points = listOf(
            TrendPoint(1, 60.0),
            TrendPoint(10, 100.0),
        )
        val offsets = pointOffsets(frame, points, ticks.first(), ticks.last())
        assertTrue(offsets[1].x > offsets[0].x)
        offsets.forEach { offset ->
            assertTrue(offset.x - 10f >= 0f)
            assertTrue(offset.x + 10f <= 400f)
            assertTrue(offset.y - 10f >= 0f)
            assertTrue(offset.y + 10f <= 300f - 24f)
        }
    }

    @Test
    fun equalValuesDoNotInventASlope() {
        val frame = plotFrame(400f, 300f, 40f, 20f, 8f, 8f)
        val ticks = yAxisTicks(50.0, 50.0, minStep = 0.5)
        val offsets = pointOffsets(
            frame,
            listOf(TrendPoint(1, 50.0), TrendPoint(4, 50.0)),
            ticks.first(),
            ticks.last(),
        )
        assertEquals(offsets[0].y, offsets[1].y, 0.01f)
        assertTrue(offsets[1].x > offsets[0].x)
    }
}
