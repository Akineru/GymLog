package com.example.gymlog

import com.example.gymlog.data.WorkoutClockLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutClockLogicTest {
    @Test
    fun timerDefaultsToTwoMinutes() {
        assertEquals(120_000L, WorkoutClockLogic.DEFAULT_TIMER_MILLIS)
        assertEquals("02:00", WorkoutClockLogic.formatClock(120_000L))
    }

    @Test
    fun timerAdjustsInThirtySecondStepsAndNeverGoesNegative() {
        val plus = WorkoutClockLogic.adjustTimer(60_000L, 60_000L, 30_000L)
        assertEquals(90_000L, plus.durationMillis)
        assertEquals(90_000L, plus.displayMillis)

        val minus = WorkoutClockLogic.adjustTimer(60_000L, 60_000L, -30_000L)
        assertEquals(30_000L, minus.durationMillis)
        assertEquals(30_000L, minus.displayMillis)

        val clamped = WorkoutClockLogic.adjustTimer(30_000L, 30_000L, -30_000L)
        assertEquals(0L, clamped.durationMillis)
        assertEquals(0L, clamped.displayMillis)
    }

    @Test
    fun clockFormatHandlesHours() {
        assertEquals("00:01", WorkoutClockLogic.formatClock(1_000L))
        assertEquals("01:01:01", WorkoutClockLogic.formatClock(3_661_000L))
    }
}
