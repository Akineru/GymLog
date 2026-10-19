package com.example.gymlog.ui.home

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.MineStats
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.TimerRecordEntity
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSetEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class HomeRangeMode {
    WEEK,
    MONTH,
}

data class HomeSummary(
    val durationMillis: Long,
    val trainingDays: Int,
    val setCount: Int,
    val volumeKg: Double,
)

data class HomeExerciseRow(
    val sessionId: String,
    val exerciseId: String,
    val name: String,
    val setCount: Int,
    val volumeKg: Double,
)

data class HomePartGroup(
    val partKey: String,
    val label: String,
    val volumeKg: Double,
    val exercises: List<HomeExerciseRow>,
)

object HomePeriod {
    fun weekStart(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun weekEnd(date: LocalDate): LocalDate = weekStart(date).plusDays(6)

    fun statsRange(mode: HomeRangeMode, selected: LocalDate): Pair<LocalDate, LocalDate> {
        return when (mode) {
            HomeRangeMode.WEEK -> weekStart(selected) to weekEnd(selected)
            HomeRangeMode.MONTH -> {
                val month = YearMonth.from(selected)
                month.atDay(1) to month.atEndOfMonth()
            }
        }
    }

    fun shift(mode: HomeRangeMode, selected: LocalDate, forward: Boolean): LocalDate {
        val delta = if (forward) 1L else -1L
        return when (mode) {
            HomeRangeMode.WEEK -> selected.plusWeeks(delta)
            HomeRangeMode.MONTH -> selected.plusMonths(delta)
        }
    }

    fun summarize(
        sessions: List<SessionDetail>,
        timers: List<TimerRecordEntity>,
        measurements: List<BodyMeasurementEntity>,
        start: LocalDate,
        endInclusive: LocalDate,
    ): HomeSummary {
        val startEpoch = start.toEpochDay()
        val endEpoch = endInclusive.toEpochDay()
        val days = HashSet<Long>()
        var setCount = 0
        var volume = 0.0
        for (detail in sessions) {
            val day = detail.session.dateEpochDay
            if (day < startEpoch || day > endEpoch) continue
            val counted = countSession(detail, measurements)
            if (counted.setCount == 0) continue
            days += day
            setCount += counted.setCount
            volume += counted.volumeKg
        }
        val rangedTimers = timers.filter { it.dateEpochDay in startEpoch..endEpoch }
        return HomeSummary(
            durationMillis = MineStats.totalTimerMillis(rangedTimers),
            trainingDays = days.size,
            setCount = setCount,
            volumeKg = volume,
        )
    }

    fun trainedEpochDays(
        sessions: List<SessionDetail>,
        start: LocalDate,
        endInclusive: LocalDate,
    ): Set<Long> {
        val startEpoch = start.toEpochDay()
        val endEpoch = endInclusive.toEpochDay()
        val days = HashSet<Long>()
        for (detail in sessions) {
            val day = detail.session.dateEpochDay
            if (day < startEpoch || day > endEpoch) continue
            if (detail.exercises.any { it.sets.isNotEmpty() }) days += day
        }
        return days
    }

    fun dayGroups(
        sessions: List<SessionDetail>,
        measurements: List<BodyMeasurementEntity>,
        date: LocalDate,
    ): List<HomePartGroup> {
        val epoch = date.toEpochDay()
        val rows = ArrayList<MutableRow>()
        sessions.forEachIndexed { sessionIndex, detail ->
            if (detail.session.dateEpochDay != epoch) return@forEachIndexed
            val bodyWeight = WeightLogic.bodyWeightKg(measurements, epoch)
            detail.exercises.forEachIndexed { exerciseIndex, item ->
                if (item.sets.isEmpty()) return@forEachIndexed
                rows += MutableRow(
                    sessionId = detail.session.id,
                    exerciseId = item.exercise.id,
                    name = item.exercise.name,
                    part = ExerciseClassification.exerciseGroupingKey(item.exercise),
                    setCount = item.sets.size,
                    volumeKg = exerciseVolume(item.sets, bodyWeight),
                    sessionIndex = sessionIndex,
                    exerciseIndex = exerciseIndex,
                )
            }
        }
        return rows.groupBy { it.part }
            .map { (part, items) ->
                val ordered = items.sortedWith(
                    compareByDescending<MutableRow> { it.volumeKg }
                        .thenBy { it.sessionIndex }
                        .thenBy { it.exerciseIndex },
                )
                HomePartGroup(
                    partKey = part,
                    label = ExerciseClassification.partLabel(part),
                    volumeKg = items.sumOf { it.volumeKg },
                    exercises = ordered.map { row ->
                        HomeExerciseRow(
                            sessionId = row.sessionId,
                            exerciseId = row.exerciseId,
                            name = row.name,
                            setCount = row.setCount,
                            volumeKg = row.volumeKg,
                        )
                    },
                )
            }
            .sortedWith(
                compareByDescending<HomePartGroup> { it.volumeKg }
                    .thenBy { ExerciseClassification.partSortOrder(it.partKey) },
            )
    }

    private fun countSession(
        detail: SessionDetail,
        measurements: List<BodyMeasurementEntity>,
    ): CountedSession {
        var setCount = 0
        var volume = 0.0
        val bodyWeight = WeightLogic.bodyWeightKg(measurements, detail.session.dateEpochDay)
        for (item in detail.exercises) {
            if (item.sets.isEmpty()) continue
            setCount += item.sets.size
            volume += exerciseVolume(item.sets, bodyWeight)
        }
        return CountedSession(setCount, volume)
    }

    private fun exerciseVolume(
        sets: List<WorkoutSetEntity>,
        bodyWeightKg: Double,
    ): Double {
        return WeightLogic.totalVolume(
            sets.map { set ->
                WeightLogic.effectiveKg(
                    WeightMode.fromStorage(set.weightMode),
                    set.weightKg,
                    bodyWeightKg,
                ) to set.reps
            },
        )
    }

    private data class MutableRow(
        val sessionId: String,
        val exerciseId: String,
        val name: String,
        val part: String,
        val setCount: Int,
        val volumeKg: Double,
        val sessionIndex: Int,
        val exerciseIndex: Int,
    )

    private data class CountedSession(
        val setCount: Int,
        val volumeKg: Double,
    )
}
