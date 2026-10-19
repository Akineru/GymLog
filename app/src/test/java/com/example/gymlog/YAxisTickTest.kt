package com.example.gymlog

import com.example.gymlog.ui.stats.yAxisTicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YAxisTickTest {
    @Test
    fun narrowRangeUsesTwoPointFiveSteps() {
        val ticks = yAxisTicks(80.0, 80.0)
        assertEquals(listOf(77.5, 80.0, 82.5), ticks)
        assertTicksAtLeast(ticks, 2.5)
    }

    @Test
    fun wideRangeKeepsAtLeastTwoPointFiveAndStaysOnGrid() {
        val ticks = yAxisTicks(60.0, 100.0)
        assertTrue(ticks.size <= 5)
        assertTrue(ticks.first() <= 60.0)
        assertTrue(ticks.last() >= 100.0)
        assertTicksAtLeast(ticks, 2.5)
        ticks.forEach { tick ->
            val steps = tick / 2.5
            assertEquals(steps, kotlin.math.round(steps), 0.001)
        }
    }

    @Test
    fun halfStepCoversFatAndBmiWithoutLeavingTheGrid() {
        val ticks = yAxisTicks(17.2, 18.4, minStep = 0.5)
        assertTrue(ticks.first() <= 17.2)
        assertTrue(ticks.last() >= 18.4)
        assertTrue(ticks.size <= 5)
        assertTicksAtLeast(ticks, 0.5)
        ticks.forEach { tick ->
            val steps = tick / 0.5
            assertEquals(steps, kotlin.math.round(steps), 0.001)
        }
    }

    @Test
    fun identicalValueGetsPaddingOnAHalfStepGrid() {
        val ticks = yAxisTicks(23.6, 23.6, minStep = 0.5)
        assertEquals(listOf(23.0, 23.5, 24.0, 24.5), ticks)
        assertTicksAtLeast(ticks, 0.5)
    }

    private fun assertTicksAtLeast(ticks: List<Double>, minGap: Double) {
        ticks.zipWithNext().forEach { (left, right) ->
            assertTrue(right - left >= minGap - 0.001)
        }
    }
}
