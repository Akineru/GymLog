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

    // 到点归零后不按重置、直接按 +30 秒：旧的总时长要作废，从 0 重新累加。
    @Test
    fun adjustingTimerAfterItRanOutStartsFreshInsteadOfAccumulating() {
        var adjustment = WorkoutClockLogic.adjustTimer(
            durationMillis = 120_000L,
            displayMillis = 0L,
            deltaMillis = 30_000L,
        )
        assertEquals(30_000L, adjustment.durationMillis)
        assertEquals(30_000L, adjustment.displayMillis)

        repeat(3) {
            adjustment = WorkoutClockLogic.adjustTimer(
                durationMillis = adjustment.durationMillis,
                displayMillis = adjustment.displayMillis,
                deltaMillis = 30_000L,
            )
        }
        // 按满 4 次 = 02:00，而不是 02:00 + 原来的 02:00 = 04:00
        assertEquals(120_000L, adjustment.durationMillis)
        assertEquals(120_000L, adjustment.displayMillis)

        val minus = WorkoutClockLogic.adjustTimer(120_000L, 0L, -30_000L)
        assertEquals(0L, minus.durationMillis)
        assertEquals(0L, minus.displayMillis)
    }

    // 跑到一半（还剩 1 分、总时长 2 分）按 +30 秒：剩余和总时长一起加。
    @Test
    fun adjustingRunningTimerAddsToBothRemainingAndDuration() {
        val adjustment = WorkoutClockLogic.adjustTimer(120_000L, 60_000L, 30_000L)
        assertEquals(150_000L, adjustment.durationMillis)
        assertEquals(90_000L, adjustment.displayMillis)
    }

    @Test
    fun clockFormatHandlesHours() {
        assertEquals("00:01", WorkoutClockLogic.formatClock(1_000L))
        assertEquals("01:01:01", WorkoutClockLogic.formatClock(3_661_000L))
    }
}
