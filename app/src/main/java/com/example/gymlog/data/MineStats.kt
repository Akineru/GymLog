package com.example.gymlog.data

import kotlin.math.round

enum class MineDurationUnit {
    SECOND,
    MINUTE,
    HOUR,
}

data class MineDurationText(
    val value: String,
    val unit: MineDurationUnit,
)

object MineStats {
    fun trainingDayCount(sessions: List<SessionDetail>, todayEpochDay: Long): Int {
        val first = sessions
            .asSequence()
            .filter { session -> session.exercises.any { it.sets.isNotEmpty() } }
            .minOfOrNull { it.session.dateEpochDay }
            ?: return 0
        val span = todayEpochDay - first + 1L
        if (span <= 0L) return 0
        if (span > Int.MAX_VALUE) return Int.MAX_VALUE
        return span.toInt()
    }

    /** Inclusive training span divided by 365, ceiling to one decimal place. */
    fun formatTrainingYears(spanDays: Int): String {
        if (spanDays <= 0) return "0.0"
        val tenths = (spanDays.toLong() * 10L + 364L) / 365L
        return "${tenths / 10}.${tenths % 10}"
    }

    fun totalTimerMillis(records: List<TimerRecordEntity>): Long {
        var total = 0L
        for (record in records) {
            val duration = record.durationMillis.coerceAtLeast(0L)
            if (duration > 0L && total > Long.MAX_VALUE - duration) return Long.MAX_VALUE
            total += duration
        }
        return total
    }

    fun formatDuration(millis: Long): MineDurationText {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        if (totalSeconds <= 0L) {
            return MineDurationText("0", MineDurationUnit.MINUTE)
        }
        if (totalSeconds < 60L) {
            return MineDurationText(totalSeconds.toString(), MineDurationUnit.SECOND)
        }
        if (totalSeconds < 3600L) {
            return MineDurationText((totalSeconds / 60L).toString(), MineDurationUnit.MINUTE)
        }
        val hours = totalSeconds / 3600.0
        val rounded = round(hours * 10.0) / 10.0
        val text = if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
        return MineDurationText(text, MineDurationUnit.HOUR)
    }

    fun formatAsMinutes(millis: Long): MineDurationText {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        return MineDurationText((totalSeconds / 60L).toString(), MineDurationUnit.MINUTE)
    }

    fun latestBodyFatPercent(measurements: List<BodyMeasurementEntity>): Double? {
        return TrendLogic.bodyFatSeries(measurements).lastOrNull()?.value
    }

    fun latestBodyWeightKg(measurements: List<BodyMeasurementEntity>): Double? {
        return TrendLogic.weightSeries(measurements).lastOrNull()?.value
    }
}
