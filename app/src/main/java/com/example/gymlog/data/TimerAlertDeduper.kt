package com.example.gymlog.data

/**
 * Suppresses the duplicate delivery of the primary and OEM fallback alarms,
 * while allowing a new countdown (new token) to alert immediately.
 */
internal class TimerAlertDeduper(private val windowMillis: Long = 3_000L) {
    private var lastToken = 0L
    private var lastAlertAt = 0L

    fun shouldAlert(token: Long, now: Long): Boolean {
        if (token != 0L && token == lastToken && now - lastAlertAt < windowMillis) {
            return false
        }
        lastToken = token
        lastAlertAt = now
        return true
    }
}
