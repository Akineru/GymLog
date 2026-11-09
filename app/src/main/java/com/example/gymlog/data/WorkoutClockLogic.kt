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
        // 已经归零（跑完或者被减到 0）之后再调时长，等于重新配一次：
        // 旧的总时长必须作废，否则「2 分钟到点 + 再按 4 次 +30 秒」会显示成 4 分钟。
        if (displayMillis <= 0L) {
            val fresh = deltaMillis.coerceAtLeast(0L)
            return TimerAdjustment(durationMillis = fresh, displayMillis = fresh)
        }
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
