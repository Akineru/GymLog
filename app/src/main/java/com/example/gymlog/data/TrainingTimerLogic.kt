package com.example.gymlog.data

enum class TimerPhase {
    IDLE,
    RUNNING,
    PAUSED,
}

data class TimerLap(
    val index: Int,
    val intervalMillis: Long,
    val elapsedMillis: Long,
)

data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val accumulatedMillis: Long = 0,
    val segmentStartedAtElapsed: Long? = null,
    val laps: List<TimerLap> = emptyList(),
    val lapBaselineMillis: Long = 0,
    val recoveredAfterReboot: Boolean = false,
    val recordDateEpochDay: Long? = null,
)

sealed interface TimerEditResult {
    data class Ok(val millis: Long) : TimerEditResult
    data object InvalidNumber : TimerEditResult
    data object MinuteRange : TimerEditResult
    data object SecondRange : TimerEditResult
}

object TrainingTimerLogic {
    fun displayedMillis(state: TimerState, nowElapsedRealtime: Long): Long {
        if (state.phase != TimerPhase.RUNNING) return state.accumulatedMillis.coerceAtLeast(0)
        val started = state.segmentStartedAtElapsed ?: return state.accumulatedMillis.coerceAtLeast(0)
        if (nowElapsedRealtime < started) return state.accumulatedMillis.coerceAtLeast(0)
        return (state.accumulatedMillis + (nowElapsedRealtime - started)).coerceAtLeast(0)
    }

    fun restore(
        state: TimerState,
        nowElapsedRealtime: Long,
        savedBootCount: Int? = null,
        currentBootCount: Int? = null,
    ): TimerState {
        if (state.phase != TimerPhase.RUNNING) {
            return state.copy(recoveredAfterReboot = false)
        }
        val started = state.segmentStartedAtElapsed
        val bootChanged = savedBootCount != null &&
            currentBootCount != null &&
            savedBootCount >= 0 &&
            currentBootCount >= 0 &&
            savedBootCount != currentBootCount
        if (started == null || nowElapsedRealtime < started || bootChanged) {
            return state.copy(
                phase = TimerPhase.PAUSED,
                segmentStartedAtElapsed = null,
                recoveredAfterReboot = true,
            )
        }
        return state.copy(recoveredAfterReboot = false)
    }

    fun toggle(state: TimerState, nowElapsedRealtime: Long, dateEpochDay: Long? = null): TimerState {
        return when (state.phase) {
            TimerPhase.IDLE -> start(state, nowElapsedRealtime, dateEpochDay)
            TimerPhase.RUNNING -> pause(state, nowElapsedRealtime)
            TimerPhase.PAUSED -> resume(state, nowElapsedRealtime)
        }
    }

    fun start(state: TimerState, nowElapsedRealtime: Long, dateEpochDay: Long? = null): TimerState {
        if (state.phase == TimerPhase.RUNNING) return state
        val recordedDay = when (state.phase) {
            TimerPhase.IDLE -> dateEpochDay
            else -> state.recordDateEpochDay ?: dateEpochDay
        }
        return state.copy(
            phase = TimerPhase.RUNNING,
            segmentStartedAtElapsed = nowElapsedRealtime,
            recoveredAfterReboot = false,
            recordDateEpochDay = recordedDay,
        )
    }

    fun pause(state: TimerState, nowElapsedRealtime: Long): TimerState {
        if (state.phase != TimerPhase.RUNNING) return state
        return state.copy(
            phase = TimerPhase.PAUSED,
            accumulatedMillis = displayedMillis(state, nowElapsedRealtime),
            segmentStartedAtElapsed = null,
            recoveredAfterReboot = false,
        )
    }

    fun resume(state: TimerState, nowElapsedRealtime: Long): TimerState {
        if (state.phase != TimerPhase.PAUSED) return state
        return state.copy(
            phase = TimerPhase.RUNNING,
            segmentStartedAtElapsed = nowElapsedRealtime,
            recoveredAfterReboot = false,
        )
    }

    fun edit(state: TimerState, nowElapsedRealtime: Long, millis: Long): TimerState {
        val safe = millis.coerceAtLeast(0)
        val running = state.phase == TimerPhase.RUNNING
        val phase = when {
            running -> TimerPhase.RUNNING
            safe == 0L && state.laps.isEmpty() -> TimerPhase.IDLE
            else -> TimerPhase.PAUSED
        }
        return state.copy(
            phase = phase,
            accumulatedMillis = safe,
            segmentStartedAtElapsed = if (running) nowElapsedRealtime else null,
            lapBaselineMillis = safe,
            recoveredAfterReboot = false,
            recordDateEpochDay = if (phase == TimerPhase.IDLE) null else state.recordDateEpochDay,
        )
    }

    fun lap(state: TimerState, nowElapsedRealtime: Long): TimerState {
        if (state.phase == TimerPhase.IDLE) return state
        val elapsed = displayedMillis(state, nowElapsedRealtime)
        val interval = elapsed - state.lapBaselineMillis
        if (interval < 0) return state
        val next = TimerLap(
            index = state.laps.size + 1,
            intervalMillis = interval,
            elapsedMillis = elapsed,
        )
        return state.copy(
            laps = state.laps + next,
            lapBaselineMillis = elapsed,
        )
    }

    fun resetTime(state: TimerState, clearLaps: Boolean): TimerState {
        return TimerState(
            phase = TimerPhase.IDLE,
            laps = if (clearLaps) emptyList() else state.laps,
        )
    }

    fun parseEdit(hours: String, minutes: String, seconds: String): TimerEditResult {
        val hour = hours.trim().toLongOrNull()
        val minute = minutes.trim().toLongOrNull()
        val second = seconds.trim().toLongOrNull()
        if (hour == null || minute == null || second == null || hour < 0) {
            return TimerEditResult.InvalidNumber
        }
        if (minute !in 0..59) return TimerEditResult.MinuteRange
        if (second !in 0..59) return TimerEditResult.SecondRange
        if (hour > 999) return TimerEditResult.InvalidNumber
        val millis = ((hour * 60 + minute) * 60 + second) * 1000
        return TimerEditResult.Ok(millis)
    }

    fun formatDuration(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatDurationChinese(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (hours > 0) append("%02d时".format(hours))
            if (totalSeconds >= 60) append("%02d分".format(minutes))
            append("%02d秒".format(seconds))
        }
    }
}
