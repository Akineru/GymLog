package com.example.gymlog

import com.example.gymlog.data.TimerAlertGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 到点提醒的「是否真的到点」判定。
 *
 * 这些断言锁住一件事：**收到闹钟 ≠ 到点**。
 * 备用闹钟（setExactAndAllowWhileIdle）在国产 ROM 上会被按批处理窗口提前投递，
 * 实测 03:00 的组歇在 02:31 就弹了「计时结束」。判定必须按目标时刻复核。
 */
class TimerAlertGateTest {

    private val tolerance = TimerAlertGate.EARLY_TOLERANCE_MILLIS

    @Test
    fun earlyArrivalOnlyRearmsAndNeverAlerts() {
        // 目标 180_000，现在 151_000 => 还差 29 秒，属于提前到达
        val verdict = TimerAlertGate.evaluate(atElapsed = 180_000L, nowElapsed = 151_000L, retried = false)
        assertFalse("提前到达不能提醒", verdict.alert)
        assertEquals("要按真实剩余时间补排", 29_000L, verdict.rearmMillis)
    }

    @Test
    fun exactlyOnTimeAlerts() {
        val verdict = TimerAlertGate.evaluate(atElapsed = 180_000L, nowElapsed = 180_000L, retried = false)
        assertTrue(verdict.alert)
        assertEquals(0L, verdict.rearmMillis)
    }

    @Test
    fun smallJitterWithinToleranceStillAlerts() {
        // 早 800ms 是闹钟抖动，不该被当成提前
        val verdict = TimerAlertGate.evaluate(atElapsed = 180_000L, nowElapsed = 180_000L - 800L, retried = false)
        assertTrue(verdict.alert)
        // 边界：正好等于容差也提醒
        assertTrue(TimerAlertGate.evaluate(180_000L, 180_000L - tolerance, retried = false).alert)
        // 超过容差一点就要补排
        assertFalse(TimerAlertGate.evaluate(180_000L, 180_000L - tolerance - 1L, retried = false).alert)
    }

    @Test
    fun lateArrivalAlerts() {
        // 被系统推迟后晚到，必须照常提醒
        val verdict = TimerAlertGate.evaluate(atElapsed = 180_000L, nowElapsed = 240_000L, retried = false)
        assertTrue(verdict.alert)
    }

    @Test
    fun secondEarlyArrivalAlertsAnyway() {
        // 已经补排过一次还是提前：宁可早一点响，也不能永远不响（防止补排死循环）
        val verdict = TimerAlertGate.evaluate(atElapsed = 180_000L, nowElapsed = 151_000L, retried = true)
        assertTrue(verdict.alert)
    }

    @Test
    fun missingTargetTimeFallsBackToAlerting() {
        // 老版本排下的闹钟没有目标时刻：信息不足时不能吞掉提醒
        assertTrue(TimerAlertGate.evaluate(atElapsed = 0L, nowElapsed = 1_000L, retried = false).alert)
    }
}
