package com.example.gymlog

import com.example.gymlog.data.WorkoutNotificationPhase
import com.example.gymlog.data.WorkoutNotificationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 通知状态的纯逻辑约束。这些断言保证通知不会变成「第二套计时」，
 * 也不会因为运行中每秒走动而反复重投。
 */
class WorkoutNotificationStateTest {

    private fun state(
        phase: WorkoutNotificationPhase,
        elapsed: Long,
        name: String? = "卧推",
        draftKey: String = "new-1",
        anchor: Long = 1_000L,
    ) = WorkoutNotificationState(
        draftKey = draftKey,
        phase = phase,
        elapsedMillis = elapsed,
        exerciseName = name,
        anchoredAtElapsedRealtime = anchor,
    )

    @Test
    fun runningElapsedKeepsCountingFromTheAnchor() {
        val s = state(WorkoutNotificationPhase.RUNNING, elapsed = 30_000L, anchor = 1_000L)
        // 30 秒之后
        assertEquals(60_000L, s.elapsedAt(31_000L))
        // 时间倒退也不算负数
        assertEquals(30_000L, s.elapsedAt(500L))
    }

    @Test
    fun pausedAndFinishingStayFrozen() {
        val paused = state(WorkoutNotificationPhase.PAUSED, elapsed = 754_000L)
        assertEquals(754_000L, paused.elapsedAt(999_999L))
        val finishing = state(WorkoutNotificationPhase.FINISHING, elapsed = 1_800_000L)
        assertEquals(1_800_000L, finishing.elapsedAt(999_999L))
    }

    @Test
    fun signatureIgnoresElapsedWhileRunning() {
        // 运行中每 100ms 都会推一次状态，但签名不变 => 不重投通知
        val a = state(WorkoutNotificationPhase.RUNNING, elapsed = 1_000L, anchor = 1_000L)
        val b = state(WorkoutNotificationPhase.RUNNING, elapsed = 1_100L, anchor = 1_100L)
        assertEquals(a.signature, b.signature)
    }

    @Test
    fun signatureChangesWhenFrozenElapsedChanges() {
        val a = state(WorkoutNotificationPhase.PAUSED, elapsed = 60_000L)
        val b = state(WorkoutNotificationPhase.PAUSED, elapsed = 61_000L)
        assertNotEquals(a.signature, b.signature)
    }

    @Test
    fun signatureChangesOnPhaseExerciseAndDraft() {
        val base = state(WorkoutNotificationPhase.RUNNING, elapsed = 0L)
        assertNotEquals(base.signature, state(WorkoutNotificationPhase.FINISHING, elapsed = 0L).signature)
        assertNotEquals(base.signature, state(WorkoutNotificationPhase.RUNNING, elapsed = 0L, name = "深蹲").signature)
        assertNotEquals(base.signature, state(WorkoutNotificationPhase.RUNNING, elapsed = 0L, draftKey = "new-2").signature)
    }

    @Test
    fun nullAndEmptyExerciseNameShareTheSameSignature() {
        val nullName = state(WorkoutNotificationPhase.RUNNING, elapsed = 0L, name = null)
        val emptyName = state(WorkoutNotificationPhase.RUNNING, elapsed = 0L, name = "")
        // orEmpty() 让 null 与空串等价，避免文案在两者之间来回抖动
        assertEquals(nullName.signature, emptyName.signature)
    }
}
