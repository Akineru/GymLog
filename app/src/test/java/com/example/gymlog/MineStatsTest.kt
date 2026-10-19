package com.example.gymlog

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.MineDurationUnit
import com.example.gymlog.data.MineStats
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.TimerRecordEntity
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MineStatsTest {
    @Test
    fun trainingDaysSpanFromFirstRecordThroughToday() {
        val sessions = listOf(
            session(day = 10, sets = 2),
            session(day = 10, sets = 1),
            session(day = 11, sets = 1),
            session(day = 1, sets = 0),
            session(day = 12, sets = 0),
        )
        assertEquals(7, MineStats.trainingDayCount(sessions, todayEpochDay = 16))
        assertEquals(16, MineStats.trainingDayCount(listOf(session(day = 1, sets = 1)), todayEpochDay = 16))
        assertEquals(1, MineStats.trainingDayCount(listOf(session(day = 16, sets = 1)), todayEpochDay = 16))
        assertEquals(0, MineStats.trainingDayCount(listOf(session(day = 20, sets = 1)), todayEpochDay = 16))
    }

    @Test
    fun emptyHistoryIsZeroDays() {
        assertEquals(0, MineStats.trainingDayCount(emptyList(), todayEpochDay = 16))
    }

    @Test
    fun trainingYearsCeilToOneDecimal() {
        assertEquals("0.0", MineStats.formatTrainingYears(0))
        assertEquals("0.1", MineStats.formatTrainingYears(1))
        assertEquals("0.1", MineStats.formatTrainingYears(16))
        assertEquals("0.1", MineStats.formatTrainingYears(36))
        assertEquals("0.2", MineStats.formatTrainingYears(37))
        assertEquals("1.0", MineStats.formatTrainingYears(365))
        assertEquals("1.1", MineStats.formatTrainingYears(366))
    }

    @Test
    fun durationSumsTimerRecordsAndIgnoresNegative() {
        val total = MineStats.totalTimerMillis(
            listOf(
                timer("a", 1_500),
                timer("b", -20),
                timer("c", 2_500),
            ),
        )
        assertEquals(4_000L, total)
    }

    @Test
    fun durationFormatUsesMinutesThenHours() {
        assertEquals("0" to MineDurationUnit.MINUTE, text(0))
        assertEquals("40" to MineDurationUnit.SECOND, text(40_000))
        assertEquals("9" to MineDurationUnit.MINUTE, text(9 * 60_000L + 1_000))
        assertEquals("1.5" to MineDurationUnit.HOUR, text(90 * 60_000L))
        assertEquals("2" to MineDurationUnit.HOUR, text(2 * 3_600_000L))
    }

    @Test
    fun durationCanSwitchToWholeMinutes() {
        assertEquals("0" to MineDurationUnit.MINUTE, minutes(0))
        assertEquals("0" to MineDurationUnit.MINUTE, minutes(40_000))
        assertEquals("9" to MineDurationUnit.MINUTE, minutes(9 * 60_000L + 1_000))
        assertEquals("90" to MineDurationUnit.MINUTE, minutes(90 * 60_000L))
        assertEquals("120" to MineDurationUnit.MINUTE, minutes(2 * 3_600_000L))
    }

    @Test
    fun bodyFatUsesLatestPositiveValue() {
        val older = measurement(day = 1, fat = 20.0, createdAt = 1)
        val newerWithoutFat = measurement(day = 2, fat = null, createdAt = 2)
        val zero = measurement(day = 3, fat = 0.0, createdAt = 3)
        assertEquals(20.0, MineStats.latestBodyFatPercent(listOf(older, newerWithoutFat, zero))!!, 0.0)
        assertNull(MineStats.latestBodyFatPercent(listOf(newerWithoutFat, zero)))
        assertEquals(
            18.5,
            MineStats.latestBodyFatPercent(
                listOf(older, measurement(day = 4, fat = 18.5, createdAt = 4)),
            )!!,
            0.0,
        )
    }

    @Test
    fun bodyWeightUsesLatestPositiveValue() {
        val older = measurement(day = 1, fat = null, createdAt = 1, weight = 70.0)
        val newerWithoutWeight = measurement(day = 2, fat = null, createdAt = 2, weight = null)
        val zero = measurement(day = 3, fat = null, createdAt = 3, weight = 0.0)
        assertEquals(70.0, MineStats.latestBodyWeightKg(listOf(older, newerWithoutWeight, zero))!!, 0.0)
        assertNull(MineStats.latestBodyWeightKg(listOf(newerWithoutWeight, zero)))
        assertEquals(
            74.0,
            MineStats.latestBodyWeightKg(
                listOf(older, measurement(day = 4, fat = null, createdAt = 4, weight = 74.0)),
            )!!,
            0.0,
        )
    }

    private fun text(millis: Long): Pair<String, MineDurationUnit> {
        val formatted = MineStats.formatDuration(millis)
        return formatted.value to formatted.unit
    }

    private fun minutes(millis: Long): Pair<String, MineDurationUnit> {
        val formatted = MineStats.formatAsMinutes(millis)
        return formatted.value to formatted.unit
    }

    private fun timer(id: String, millis: Long) = TimerRecordEntity(
        id = id,
        durationMillis = millis,
        dateEpochDay = 1,
        sessionId = null,
        createdAt = 1,
    )

    private fun session(day: Long, sets: Int): SessionDetail {
        val exercise = ExerciseEntity(
            id = "ex",
            name = "深蹲",
            bodyPart = "腿",
            equipmentType = "FREE_WEIGHT",
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = false,
            aliases = null,
            createdAt = 1,
        )
        val setRows = List(sets) { index ->
            WorkoutSetEntity(
                id = "$day-$index",
                sessionId = "s-$day-$sets",
                exerciseId = exercise.id,
                exerciseOrder = 0,
                setIndex = index,
                weightMode = WeightMode.WEIGHTED.name,
                weightKg = 20.0,
                reps = 8,
                note = null,
            )
        }
        return SessionDetail(
            session = WorkoutSessionEntity(
                id = "s-$day-$sets",
                dateEpochDay = day,
                note = null,
                createdAt = day,
            ),
            exercises = if (sets == 0) {
                emptyList()
            } else {
                listOf(SessionExercise(exercise, setRows))
            },
        )
    }

    private fun measurement(
        day: Long,
        fat: Double?,
        createdAt: Long,
        weight: Double? = null,
    ) = BodyMeasurementEntity(
        id = "m-$createdAt",
        recordedEpochDay = day,
        heightCm = null,
        weightKg = weight,
        bodyFatPercent = fat,
        createdAt = createdAt,
    )
}
