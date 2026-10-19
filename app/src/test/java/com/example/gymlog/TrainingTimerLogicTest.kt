package com.example.gymlog

import com.example.gymlog.data.TimerEditResult
import com.example.gymlog.data.TimerPhase
import com.example.gymlog.data.TimerState
import com.example.gymlog.data.TrainingTimerLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTimerLogicTest {
    @Test
    fun pauseDoesNotCountTheGapBeforeResume() {
        val started = TrainingTimerLogic.start(TimerState(), nowElapsedRealtime = 1_000)
        val paused = TrainingTimerLogic.pause(started, nowElapsedRealtime = 4_000)
        assertEquals(3_000, TrainingTimerLogic.displayedMillis(paused, nowElapsedRealtime = 9_000))
        val resumed = TrainingTimerLogic.resume(paused, nowElapsedRealtime = 9_000)
        assertEquals(4_000, TrainingTimerLogic.displayedMillis(resumed, nowElapsedRealtime = 10_000))
    }

    @Test
    fun lapsKeepIntervalAndElapsedSeparately() {
        val running = TrainingTimerLogic.start(TimerState(), nowElapsedRealtime = 0)
        val first = TrainingTimerLogic.lap(running, nowElapsedRealtime = 2_000)
        val second = TrainingTimerLogic.lap(first, nowElapsedRealtime = 5_000)
        assertEquals(1, second.laps[0].index)
        assertEquals(2_000, second.laps[0].intervalMillis)
        assertEquals(2_000, second.laps[0].elapsedMillis)
        assertEquals(2, second.laps[1].index)
        assertEquals(3_000, second.laps[1].intervalMillis)
        assertEquals(5_000, second.laps[1].elapsedMillis)
    }

    @Test
    fun editResetsTheNextIntervalWithoutChangingEarlierLaps() {
        val running = TrainingTimerLogic.start(TimerState(), nowElapsedRealtime = 0)
        val lapped = TrainingTimerLogic.lap(running, nowElapsedRealtime = 5_000)
        val paused = TrainingTimerLogic.pause(lapped, nowElapsedRealtime = 5_000)
        val edited = TrainingTimerLogic.edit(paused, nowElapsedRealtime = 8_000, millis = 1_000)
        assertEquals(5_000, edited.laps.single().elapsedMillis)
        assertEquals(1_000, edited.lapBaselineMillis)
        val resumed = TrainingTimerLogic.resume(edited, nowElapsedRealtime = 8_000)
        val next = TrainingTimerLogic.lap(resumed, nowElapsedRealtime = 10_000)
        assertEquals(2_000, next.laps.last().intervalMillis)
        assertEquals(3_000, next.laps.last().elapsedMillis)
        assertEquals(5_000, next.laps.first().elapsedMillis)
    }

    @Test
    fun rebootDropsAnInvalidRunningTimestampAndDoesNotGoNegative() {
        val running = TimerState(
            phase = TimerPhase.RUNNING,
            accumulatedMillis = 1_000,
            segmentStartedAtElapsed = 50_000,
        )
        val restored = TrainingTimerLogic.restore(running, nowElapsedRealtime = 100)
        assertEquals(TimerPhase.PAUSED, restored.phase)
        assertTrue(restored.recoveredAfterReboot)
        assertEquals(1_000, TrainingTimerLogic.displayedMillis(restored, nowElapsedRealtime = 100))
    }

    @Test
    fun rebootPausesWhenBootCountChangesEvenIfElapsedLooksValid() {
        val running = TimerState(
            phase = TimerPhase.RUNNING,
            accumulatedMillis = 4_000,
            segmentStartedAtElapsed = 1_000,
        )
        val restored = TrainingTimerLogic.restore(
            state = running,
            nowElapsedRealtime = 90_000,
            savedBootCount = 3,
            currentBootCount = 4,
        )
        assertEquals(TimerPhase.PAUSED, restored.phase)
        assertTrue(restored.recoveredAfterReboot)
        assertEquals(4_000, TrainingTimerLogic.displayedMillis(restored, nowElapsedRealtime = 90_000))
    }

    @Test
    fun resetStopsAtZeroAndCanKeepLaps() {
        val running = TrainingTimerLogic.start(TimerState(), nowElapsedRealtime = 0)
        val lapped = TrainingTimerLogic.lap(running, nowElapsedRealtime = 2_000)
        val reset = TrainingTimerLogic.resetTime(lapped, clearLaps = false)
        assertEquals(TimerPhase.IDLE, reset.phase)
        assertEquals(0, TrainingTimerLogic.displayedMillis(reset, nowElapsedRealtime = 9_000))
        assertEquals(1, reset.laps.size)
        val cleared = TrainingTimerLogic.resetTime(lapped, clearLaps = true)
        assertTrue(cleared.laps.isEmpty())
    }

    @Test
    fun editRejectsOutOfRangeMinutesAndSeconds() {
        assertTrue(TrainingTimerLogic.parseEdit("1", "60", "0") is TimerEditResult.MinuteRange)
        assertTrue(TrainingTimerLogic.parseEdit("1", "0", "60") is TimerEditResult.SecondRange)
        assertTrue(TrainingTimerLogic.parseEdit("", "1", "1") is TimerEditResult.InvalidNumber)
        val ok = TrainingTimerLogic.parseEdit("01", "05", "09") as TimerEditResult.Ok
        assertEquals((1 * 3600 + 5 * 60 + 9) * 1000L, ok.millis)
        assertEquals("01:05:09", TrainingTimerLogic.formatDuration(ok.millis))
        assertEquals("01时05分09秒", TrainingTimerLogic.formatDurationChinese(ok.millis))
        assertEquals("01时00分09秒", TrainingTimerLogic.formatDurationChinese((3600 + 9) * 1000L))
        assertEquals("09分31秒", TrainingTimerLogic.formatDurationChinese(9 * 60_000L + 31_000L))
        assertEquals("01分00秒", TrainingTimerLogic.formatDurationChinese(60_000L))
        assertEquals("40秒", TrainingTimerLogic.formatDurationChinese(40_000L))
        assertEquals("00秒", TrainingTimerLogic.formatDurationChinese(0))
    }

    @Test
    fun startRemembersTheOpenedDayThroughPauseAndResume() {
        val day = 20_000L
        val started = TrainingTimerLogic.toggle(TimerState(), nowElapsedRealtime = 0, dateEpochDay = day)
        val paused = TrainingTimerLogic.toggle(started, nowElapsedRealtime = 1_000, dateEpochDay = 20_001L)
        val resumed = TrainingTimerLogic.toggle(paused, nowElapsedRealtime = 5_000, dateEpochDay = 20_002L)
        assertEquals(day, resumed.recordDateEpochDay)
        val reset = TrainingTimerLogic.resetTime(resumed, clearLaps = true)
        assertEquals(null, reset.recordDateEpochDay)
    }
}
