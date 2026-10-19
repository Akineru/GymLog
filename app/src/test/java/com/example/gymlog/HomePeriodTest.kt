package com.example.gymlog

import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.TimerRecordEntity
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.ui.home.HomePeriod
import com.example.gymlog.ui.home.HomeRangeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HomePeriodTest {
    @Test
    fun weekStartsMondayAndIncludesSunday() {
        val saturday = LocalDate.of(2026, 10, 17)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        val start = HomePeriod.weekStart(saturday)
        val end = HomePeriod.weekEnd(saturday)
        assertEquals(LocalDate.of(2026, 10, 12), start)
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 18), end)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertEquals(start to end, HomePeriod.statsRange(HomeRangeMode.WEEK, saturday))
    }

    @Test
    fun shiftingWeekKeepsTheSameWeekday() {
        val thursday = LocalDate.of(2026, 10, 15)
        val previous = HomePeriod.shift(HomeRangeMode.WEEK, thursday, forward = false)
        val next = HomePeriod.shift(HomeRangeMode.WEEK, thursday, forward = true)
        assertEquals(LocalDate.of(2026, 10, 8), previous)
        assertEquals(DayOfWeek.THURSDAY, previous.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 22), next)
        assertEquals(DayOfWeek.THURSDAY, next.dayOfWeek)
    }

    @Test
    fun shiftingMonthClampsAndWeekCanCrossMonths() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            HomePeriod.shift(HomeRangeMode.MONTH, LocalDate.of(2026, 1, 31), forward = true),
        )
        assertEquals(
            LocalDate.of(2026, 10, 30),
            HomePeriod.shift(HomeRangeMode.MONTH, LocalDate.of(2026, 11, 30), forward = false),
        )
        val sunday = LocalDate.of(2026, 11, 1)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 26), HomePeriod.weekStart(sunday))
        assertEquals(LocalDate.of(2026, 11, 1), HomePeriod.weekEnd(sunday))
        assertEquals(
            LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 11, 30),
            HomePeriod.statsRange(HomeRangeMode.MONTH, sunday),
        )
        val crossing = listOf(
            session("cross", HomePeriod.weekStart(sunday).toEpochDay(), sets = listOf(set("z", 10.0, 2))),
        )
        assertEquals(
            1,
            HomePeriod.summarize(
                crossing,
                emptyList(),
                emptyList(),
                HomePeriod.weekStart(sunday),
                HomePeriod.weekEnd(sunday),
            ).trainingDays,
        )
        assertEquals(
            0,
            HomePeriod.summarize(
                crossing,
                emptyList(),
                emptyList(),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
            ).trainingDays,
        )
    }

    @Test
    fun emptyDataIsZeroAndIgnoresSessionsOutsideTheRange() {
        val empty = HomePeriod.summarize(
            sessions = emptyList(),
            timers = emptyList(),
            measurements = emptyList(),
            start = LocalDate.of(2026, 10, 12),
            endInclusive = LocalDate.of(2026, 10, 18),
        )
        assertEquals(0L, empty.durationMillis)
        assertEquals(0, empty.trainingDays)
        assertEquals(0, empty.setCount)
        assertEquals(0.0, empty.volumeKg, 0.0)

        val summary = HomePeriod.summarize(
            sessions = listOf(
                session("in", day(2026, 10, 12), sets = listOf(set("a", 20.0, 10))),
                session("edge", day(2026, 10, 18), sets = listOf(set("b", 10.0, 5), set("c", 10.0, 5))),
                session("before", day(2026, 10, 11), sets = listOf(set("d", 100.0, 10))),
                session("after", day(2026, 10, 19), sets = listOf(set("e", 100.0, 10))),
                session("empty", day(2026, 10, 13), sets = emptyList()),
            ),
            timers = listOf(
                timer("t-in", 1_500, day(2026, 10, 12)),
                timer("t-out", 9_000, day(2026, 10, 11)),
                timer("t-negative", -20, day(2026, 10, 18)),
            ),
            measurements = emptyList(),
            start = LocalDate.of(2026, 10, 12),
            endInclusive = LocalDate.of(2026, 10, 18),
        )
        assertEquals(1_500L, summary.durationMillis)
        assertEquals(2, summary.trainingDays)
        assertEquals(3, summary.setCount)
        assertEquals(300.0, summary.volumeKg, 0.0)
        assertEquals(
            setOf(day(2026, 10, 12)),
            HomePeriod.trainedEpochDays(
                listOf(
                    session("in", day(2026, 10, 12), sets = listOf(set("a", 20.0, 10))),
                    session("empty", day(2026, 10, 13), sets = emptyList()),
                    session("out", day(2026, 10, 11), sets = listOf(set("d", 1.0, 1))),
                ),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 18),
            ),
        )
    }

    @Test
    fun timerWithoutSetsAddsDurationOnly() {
        val day = day(2026, 10, 15)
        val summary = HomePeriod.summarize(
            sessions = emptyList(),
            timers = listOf(timer("only", 8_000, day)),
            measurements = emptyList(),
            start = LocalDate.of(2026, 10, 12),
            endInclusive = LocalDate.of(2026, 10, 18),
        )
        assertEquals(8_000L, summary.durationMillis)
        assertEquals(0, summary.trainingDays)
        assertEquals(0, summary.setCount)
        assertEquals(0.0, summary.volumeKg, 0.0)
        assertTrue(
            HomePeriod.trainedEpochDays(
                emptyList(),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 18),
            ).isEmpty(),
        )
        assertTrue(HomePeriod.dayGroups(emptyList(), emptyList(), LocalDate.ofEpochDay(day)).isEmpty())
    }

    @Test
    fun sameDaySessionsStaySeparateAndSameExerciseKeepsBothSessions() {
        val date = LocalDate.of(2026, 10, 15)
        val epoch = date.toEpochDay()
        val bench = exercise("bench", "卧推", BodyPart.CHEST)
        val row = exercise("row", "划船", BodyPart.BACK)
        val sessions = listOf(
            session(
                "s1",
                epoch,
                exercises = listOf(
                    SessionExercise(bench, listOf(set("s1-a", 30.0, 12))),
                ),
            ),
            session(
                "s2",
                epoch,
                exercises = listOf(
                    SessionExercise(bench, listOf(set("s2-a", 40.0, 8), set("s2-b", 40.0, 8))),
                    SessionExercise(row, listOf(set("s2-c", 20.0, 10))),
                ),
            ),
            session(
                "other-day",
                day(2026, 10, 16),
                exercises = listOf(SessionExercise(bench, listOf(set("x", 100.0, 10)))),
            ),
        )
        val groups = HomePeriod.dayGroups(sessions, emptyList(), date)
        assertEquals(listOf("胸", "背"), groups.map { it.label })
        val chest = groups.first()
        assertEquals(listOf("s2", "s1"), chest.exercises.map { it.sessionId })
        assertEquals(listOf("卧推", "卧推"), chest.exercises.map { it.name })
        assertEquals(listOf(2, 1), chest.exercises.map { it.setCount })
        assertEquals(640.0, chest.exercises[0].volumeKg, 0.0)
        assertEquals(360.0, chest.exercises[1].volumeKg, 0.0)
        assertEquals("s2", groups[1].exercises.single().sessionId)
        assertEquals("划船", groups[1].exercises.single().name)
    }

    @Test
    fun bodyweightVolumeUsesRecordedBodyWeight() {
        val date = LocalDate.of(2026, 10, 15)
        val pullup = exercise("pull", "引体", BodyPart.BACK).copy(weightMode = WeightMode.BODYWEIGHT.name)
        val sessions = listOf(
            session(
                "bw",
                date.toEpochDay(),
                exercises = listOf(
                    SessionExercise(
                        pullup,
                        listOf(set("bw-1", storedKg = 99.0, reps = 10, mode = WeightMode.BODYWEIGHT)),
                    ),
                ),
            ),
        )
        val measurements = listOf(
            BodyMeasurementEntity(
                id = "m",
                recordedEpochDay = date.toEpochDay(),
                heightCm = null,
                weightKg = 70.0,
                bodyFatPercent = null,
                createdAt = 1,
            ),
        )
        val summary = HomePeriod.summarize(
            sessions,
            emptyList(),
            measurements,
            date,
            date,
        )
        assertEquals(700.0, summary.volumeKg, 0.0)
        assertEquals(700.0, HomePeriod.dayGroups(sessions, measurements, date).single().volumeKg, 0.0)
    }

    private fun day(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day).toEpochDay()

    private fun timer(id: String, millis: Long, epochDay: Long) = TimerRecordEntity(
        id = id,
        durationMillis = millis,
        dateEpochDay = epochDay,
        sessionId = null,
        createdAt = 1,
    )

    private fun exercise(id: String, name: String, part: BodyPart) = ExerciseEntity(
        id = id,
        name = name,
        bodyPart = part.name,
        equipmentType = "FREE_WEIGHT",
        weightMode = WeightMode.WEIGHTED.name,
        isCustom = false,
        aliases = null,
        createdAt = 1,
    )

    private fun set(
        id: String,
        storedKg: Double?,
        reps: Int?,
        mode: WeightMode = WeightMode.WEIGHTED,
    ) = WorkoutSetEntity(
        id = id,
        sessionId = "pending",
        exerciseId = "pending",
        exerciseOrder = 0,
        setIndex = 0,
        weightMode = mode.name,
        weightKg = storedKg,
        reps = reps,
        note = null,
    )

    private fun session(
        id: String,
        epochDay: Long,
        sets: List<WorkoutSetEntity>? = null,
        exercises: List<SessionExercise>? = null,
    ): SessionDetail {
        val exerciseRows = when {
            exercises != null -> exercises
            sets.isNullOrEmpty() -> emptyList()
            else -> listOf(
                SessionExercise(
                    exercise("ex", "深蹲", BodyPart.LEGS),
                    sets.map { it.copy(sessionId = id, exerciseId = "ex") },
                ),
            )
        }
        return SessionDetail(
            session = WorkoutSessionEntity(
                id = id,
                dateEpochDay = epochDay,
                note = null,
                createdAt = epochDay,
            ),
            exercises = exerciseRows,
        )
    }
}
