package com.example.gymlog.data

object WorkoutClockLogic {
    const val DEFAULT_TIMER_MILLIS = 120_000L
    const val TIMER_STEP_MILLIS = 30_000L

    data class TimerAdjustment(
        val durationMillis: Long,
        val displayMillis: Long,
    )

    fun adjustTimer(
        durationMillis: Long,
        displayMillis: Long,
        deltaMillis: Long,
    ): TimerAdjustment {
        val duration = (durationMillis + deltaMillis).coerceAtLeast(0L)
        val display = (displayMillis + deltaMillis).coerceIn(0L, duration)
        return TimerAdjustment(durationMillis = duration, displayMillis = display)
    }

    fun formatClock(millis: Long): String {
        val seconds = millis.coerceAtLeast(0L) / 1000L
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        val rest = seconds % 60L
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, rest)
        } else {
            "%02d:%02d".format(minutes, rest)
        }
    }
}
