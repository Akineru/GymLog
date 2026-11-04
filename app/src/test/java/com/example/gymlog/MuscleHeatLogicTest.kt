package com.example.gymlog

import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.MuscleHeatLogic
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHeatRange
import com.example.gymlog.data.MuscleHeatLevel
import com.example.gymlog.data.MuscleHeatTouch
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.ui.muscle.MuscleMaskCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MuscleHeatLogicTest {
    private val today = LocalDate.of(2026, 1, 2)

    @Test
    fun weekIsSevenDaysIncludingTodayAndCrossesTheYear() {
        val period = MuscleHeatLogic.period(today, MuscleHeatRange.WEEK)
        assertEquals(LocalDate.of(2025, 12, 27), period.start)
        assertEquals(today, period.endInclusive)
        assertEquals(7, period.endInclusive.toEpochDay() - period.start.toEpochDay() + 1)
    }

    @Test
    fun monthIsThirtyDaysIncludingTodayAndCrossesTheYear() {
        val period = MuscleHeatLogic.period(today, MuscleHeatRange.MONTH)
        assertEquals(LocalDate.of(2025, 12, 4), period.start)
        assertEquals(today, period.endInclusive)
        assertEquals(30, period.endInclusive.toEpochDay() - period.start.toEpochDay() + 1)
    }

    @Test
    fun sameSessionDoesNotCountAMuscleTwice() {
        val start = today.minusDays(6).toEpochDay()
        val end = today.toEpochDay()
        val stats = MuscleHeatLogic.aggregate(
            listOf(
                touch("s1", end, "pull", "引体向上", "latissimus_dorsi", MuscleRole.PRIMARY, 4),
                touch("s1", end, "row", "划船", "latissimus_dorsi", MuscleRole.SECONDARY, 3),
            ),
            start,
            end,
        )
        val lat = stats.getValue("latissimus_dorsi")
        assertEquals(1, lat.sessionCount)
        assertEquals(1.0, lat.score, 0.001)
        assertEquals(7, lat.setCount)
        assertEquals(2, lat.exercises.size)
        assertEquals(MuscleHeatLevel.FAIR, lat.level)
    }

    @Test
    fun secondaryOnlySessionScoresHalfAndStaysLighterThanPrimary() {
        val day = today.toEpochDay()
        val secondary = MuscleHeatLogic.aggregate(
            listOf(touch("s1", day, "curl", "弯举", "biceps_brachii", MuscleRole.SECONDARY, 3)),
            day,
            day,
        ).getValue("biceps_brachii")
        val primary = MuscleHeatLogic.aggregate(
            listOf(touch("s1", day, "curl", "弯举", "biceps_brachii", MuscleRole.PRIMARY, 3)),
            day,
            day,
        ).getValue("biceps_brachii")
        assertEquals(0.5, secondary.score, 0.001)
        assertEquals(MuscleHeatLevel.LOW, secondary.level)
        assertEquals(1.0, primary.score, 0.001)
        assertEquals(MuscleHeatLevel.FAIR, primary.level)
    }

    @Test
    fun moreSessionsMapToDarkerLevels() {
        assertEquals(MuscleHeatLevel.NONE, MuscleHeatLogic.levelFor(0.0))
        assertEquals(MuscleHeatLevel.LOW, MuscleHeatLogic.levelFor(0.5))
        assertEquals(MuscleHeatLevel.FAIR, MuscleHeatLogic.levelFor(1.0))
        assertEquals(MuscleHeatLevel.MEDIUM, MuscleHeatLogic.levelFor(2.0))
        assertEquals(MuscleHeatLevel.MEDIUM, MuscleHeatLogic.levelFor(3.5))
        assertEquals(MuscleHeatLevel.HIGH, MuscleHeatLogic.levelFor(4.0))
        assertEquals(MuscleHeatLevel.HIGH, MuscleHeatLogic.levelFor(6.5))
        assertEquals(MuscleHeatLevel.PEAK, MuscleHeatLogic.levelFor(7.0))
    }

    @Test
    fun sessionsOutsideTheWindowAreIgnored() {
        val start = today.minusDays(6).toEpochDay()
        val end = today.toEpochDay()
        val stats = MuscleHeatLogic.aggregate(
            listOf(touch("old", start - 1, "squat", "深蹲", "quadriceps", MuscleRole.PRIMARY, 5)),
            start,
            end,
        )
        assertTrue(stats.isEmpty())
    }

    @Test
    fun removingASessionDropsItsContribution() {
        val day = today.toEpochDay()
        val both = listOf(
            touch("s1", day, "squat", "深蹲", "quadriceps", MuscleRole.PRIMARY, 5),
            touch("s2", day, "lunge", "弓步", "quadriceps", MuscleRole.PRIMARY, 3),
        )
        val kept = MuscleHeatLogic.aggregate(both.drop(1), day, day).getValue("quadriceps")
        assertEquals(1, kept.sessionCount)
        assertEquals(1.0, kept.score, 0.001)
        assertEquals(listOf("lunge"), kept.exercises.map { it.exerciseId })
    }

    @Test
    fun everyCatalogMuscleHasAFigureRegion() {
        val missing = MuscleCatalog.all.filter { muscle ->
            val spec = MuscleMaskCatalog.spec(muscle.id)
            spec == null || (!spec.front && !spec.back)
        }
        assertEquals(emptyList<String>(), missing.map { it.id })
    }

    @Test
    fun touchesUseStoredLinksAndIgnoreExercisesWithoutSets() {
        val day = today.toEpochDay()
        val exercise = exercise("custom_row", custom = true)
        val preset = exercise("preset_pull_up", custom = false)
        val sessions = listOf(
            SessionDetail(
                session = WorkoutSessionEntity("s1", day, null, 1L),
                exercises = listOf(
                    SessionExercise(exercise, listOf(set("a", "s1", exercise.id))),
                    SessionExercise(preset, emptyList()),
                ),
            ),
        )
        val links = listOf(
            MuscleLink(exercise.id, "latissimus_dorsi", MuscleRole.PRIMARY),
            MuscleLink(exercise.id, "biceps_brachii", MuscleRole.SECONDARY),
        )
        val touches = MuscleHeatLogic.touches(sessions, links)
        assertEquals(setOf("latissimus_dorsi", "biceps_brachii"), touches.map { it.muscleId }.toSet())
        assertTrue(touches.all { it.exerciseId == exercise.id && it.setCount == 1 })
    }

    private fun touch(
        sessionId: String,
        day: Long,
        exerciseId: String,
        name: String,
        muscleId: String,
        role: MuscleRole,
        sets: Int,
    ) = MuscleHeatTouch(sessionId, day, exerciseId, name, muscleId, role, sets)

    private fun exercise(id: String, custom: Boolean) = ExerciseEntity(
        id = id,
        name = id,
        bodyPart = "BACK",
        equipmentType = "FREE_WEIGHT",
        weightMode = WeightMode.BODYWEIGHT.name,
        isCustom = custom,
        aliases = null,
        createdAt = 1L,
    )

    private fun set(id: String, sessionId: String, exerciseId: String) = WorkoutSetEntity(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseOrder = 0,
        setIndex = 1,
        weightMode = WeightMode.BODYWEIGHT.name,
        weightKg = null,
        reps = 8,
        note = null,
    )
}
