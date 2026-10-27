package com.example.gymlog

import com.example.gymlog.data.TimerAlertDeduper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAlertDeduperTest {
    @Test
    fun primaryAndFallbackWithSameTokenAlertOnlyOnce() {
        val deduper = TimerAlertDeduper()

        assertTrue(deduper.shouldAlert(token = 42L, now = 1_000L))
        assertFalse(deduper.shouldAlert(token = 42L, now = 1_500L))
    }

    @Test
    fun newCountdownTokenAlertsImmediately() {
        val deduper = TimerAlertDeduper()

        assertTrue(deduper.shouldAlert(token = 42L, now = 1_000L))
        assertTrue(deduper.shouldAlert(token = 43L, now = 1_100L))
    }

    @Test
    fun sameTokenAfterWindowIsAllowed() {
        val deduper = TimerAlertDeduper()

        assertTrue(deduper.shouldAlert(token = 42L, now = 1_000L))
        assertTrue(deduper.shouldAlert(token = 42L, now = 4_000L))
    }
}
