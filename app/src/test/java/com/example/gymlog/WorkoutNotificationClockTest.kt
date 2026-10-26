package com.example.gymlog

import com.example.gymlog.data.WorkoutClockDisplayState
import com.example.gymlog.data.WorkoutClockKind
import com.example.gymlog.data.WorkoutClockLogic
import com.example.gymlog.data.WorkoutNotificationClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 独立计时器在通知里的显示映射。
 *
 * 这些断言锁住三件事：
 * 1. 通知不自己计时 —— 时间一律由「快照值 + 单调时钟差」算出，与 App 内同一公式。
 * 2. 归零不出现负数，也不假装还在跑。
 * 3. 「未启动 / 已重置」由 [WorkoutNotificationClock.started] 明确给出，
 *    不再靠「显示值是否等于初始值」反推 —— 否则暂停在初始值会被误判成未启动。
 */
class WorkoutNotificationClockTest {

    private fun countdown(
        displayMillis: Long,
        durationMillis: Long = 120_000L,
        running: Boolean = false,
        started: Boolean = true,
        anchor: Long = 1_000L,
    ) = WorkoutNotificationClock(
        kind = WorkoutClockKind.COUNTDOWN,
        running = running,
        started = started,
        displayMillis = displayMillis,
        durationMillis = durationMillis,
        anchoredAtElapsedRealtime = anchor,
    )

    private fun stopwatch(
        displayMillis: Long,
        running: Boolean = false,
        started: Boolean = true,
        anchor: Long = 1_000L,
    ) = WorkoutNotificationClock(
        kind = WorkoutClockKind.STOPWATCH,
        running = running,
        started = started,
        displayMillis = displayMillis,
        durationMillis = 120_000L,
        anchoredAtElapsedRealtime = anchor,
    )

    @Test
    fun countdownRunningCountsDownFromTheAnchor() {
        val clock = countdown(displayMillis = 120_000L, running = true, anchor = 1_000L)
        val at7s = clock.resolve(8_000L)
        assertEquals(WorkoutClockDisplayState.RUNNING, at7s?.state)
        assertEquals(113_000L, at7s?.millis)
    }

    @Test
    fun countdownNeverGoesNegativeAndTurnsIntoFinished() {
        val clock = countdown(displayMillis = 5_000L, running = true, anchor = 1_000L)
        // 跑过头 4 秒
        val late = clock.resolve(10_000L)
        assertEquals(WorkoutClockDisplayState.FINISHED, late?.state)
        assertEquals(0L, late?.millis)
    }

    @Test
    fun countdownPausedStaysFrozen() {
        val clock = countdown(displayMillis = 73_000L)
        val a = clock.resolve(999_999L)
        val b = clock.resolve(1_000_000L)
        assertEquals(WorkoutClockDisplayState.PAUSED, a?.state)
        assertEquals(73_000L, a?.millis)
        assertEquals(a, b)
    }

    /** 开始后立刻暂停：数值还停在初始时长，但必须显示「已暂停」，不能当成未启动。 */
    @Test
    fun pausedRightAfterStartStillShowsPaused() {
        val clock = countdown(displayMillis = 120_000L, durationMillis = 120_000L, started = true)
        val display = clock.resolve(5_000L)
        assertEquals(WorkoutClockDisplayState.PAUSED, display?.state)
        assertEquals(120_000L, display?.millis)
    }

    /** 秒表的初始值是 0；暂停在 0 也要显示暂停，而不是消失。 */
    @Test
    fun stopwatchPausedAtZeroStillShowsPaused() {
        val clock = stopwatch(displayMillis = 0L, started = true)
        val display = clock.resolve(5_000L)
        assertEquals(WorkoutClockDisplayState.PAUSED, display?.state)
        assertEquals(0L, display?.millis)
    }

    /** 未启动 / 已重置：由 started=false 决定，与显示值无关。 */
    @Test
    fun countdownNotStartedOrResetIsHidden() {
        assertNull(countdown(displayMillis = 120_000L, started = false).resolve(5_000L))
        assertNull(countdown(displayMillis = 30_000L, durationMillis = 30_000L, started = false).resolve(5_000L))
    }

    /** 重置后重新加时（duration 与 display 同步变大）仍然算未启动。 */
    @Test
    fun resetThenAdjustStaysNotStarted() {
        val clock = countdown(displayMillis = 150_000L, durationMillis = 150_000L, started = false)
        assertNull(clock.resolve(5_000L))
    }

    @Test
    fun countdownFinishedStaysAtZero() {
        val clock = countdown(displayMillis = 0L, durationMillis = 120_000L, started = true)
        val display = clock.resolve(999_999L)
        assertEquals(WorkoutClockDisplayState.FINISHED, display?.state)
        assertEquals(0L, display?.millis)
    }

    @Test
    fun countdownAdjustedBaseIsRespected() {
        // 运行中 +30s：快照值变大，锚点重置，之后从新值继续倒数
        val clock = countdown(
            displayMillis = 90_000L,
            durationMillis = 150_000L,
            running = true,
            anchor = 5_000L,
        )
        assertEquals(85_000L, clock.resolve(10_000L)?.millis)
    }

    @Test
    fun stopwatchRunningAccumulates() {
        val clock = stopwatch(displayMillis = 1_000L, running = true, anchor = 1_000L)
        assertEquals(WorkoutClockDisplayState.RUNNING, clock.resolve(11_000L)?.state)
        assertEquals(11_000L, clock.resolve(11_000L)?.millis)
    }

    @Test
    fun stopwatchPausedStaysFrozen() {
        val clock = stopwatch(displayMillis = 42_000L)
        assertEquals(WorkoutClockDisplayState.PAUSED, clock.resolve(999_999L)?.state)
        assertEquals(42_000L, clock.resolve(999_999L)?.millis)
    }

    @Test
    fun stopwatchResetIsHidden() {
        assertNull(stopwatch(displayMillis = 0L, started = false).resolve(5_000L))
    }

    @Test
    fun switchingModeFallsBackToHidden() {
        // 切到倒计时：回到默认时长且 started=false => 不显示
        assertNull(countdown(displayMillis = 120_000L, started = false).resolve(1L))
        // 切到秒表：0 且 started=false => 不显示
        assertNull(stopwatch(displayMillis = 0L, started = false).resolve(1L))
    }

    @Test
    fun clockTextUsesTheSameFormatterAsTheApp() {
        assertEquals("02:00", WorkoutClockLogic.formatClock(120_000L))
        assertEquals("01:53", WorkoutClockLogic.formatClock(113_000L))
        assertEquals("00:00", WorkoutClockLogic.formatClock(0L))
        assertEquals("01:00:05", WorkoutClockLogic.formatClock(3_605_000L))
    }
}
