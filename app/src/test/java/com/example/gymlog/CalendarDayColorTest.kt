package com.example.gymlog

import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarDayColorLogic
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.CalendarSpecialDay
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindLogic
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarDayColorTest {
    @Test
    fun singlePartUsesItsFixedColor() {
        assertEquals(0xFFE57373.toInt(), colorOf(day(chest(20.0, 10))))
        assertEquals(0xFF64B5F6.toInt(), colorOf(day(back(30.0, 8))))
        assertEquals(0xFFFFB74D.toInt(), colorOf(day(shoulders(15.0, 8))))
    }

    @Test
    fun legsAndArmsMergeBeforeComparing() {
        val legs = day(
            exercise(ExerciseClassification.UPPER_LEGS.id, 20.0, 5),
            exercise(ExerciseClassification.LOWER_LEGS.id, 10.0, 10),
        )
        assertEquals(CalendarColorCategory.LEGS, categoryOf(legs))
        assertEquals(0xFF81C784.toInt(), colorOf(legs))

        val arms = day(
            exercise(ExerciseClassification.UPPER_ARMS.id, 10.0, 4),
            exercise(ExerciseClassification.LOWER_ARMS.id, 8.0, 6),
        )
        assertEquals(CalendarColorCategory.ARMS, categoryOf(arms))
        assertEquals(0xFFBA68C8.toInt(), colorOf(arms))
    }

    @Test
    fun waistCardioNeckAbsAndUnknownAreOther() {
        val sessions = day(
            exercise(ExerciseClassification.WAIST.id, 5.0, 10),
            exercise(ExerciseClassification.CARDIO.id, 1.0, 20),
            exercise(ExerciseClassification.NECK.id, 4.0, 8),
            exercise("ABS", 6.0, 10),
            exercise("NOT_A_PART", 2.0, 5),
        )
        assertEquals(CalendarColorCategory.OTHER, categoryOf(sessions))
        assertEquals(0xFF90A4AE.toInt(), colorOf(sessions))
    }

    @Test
    fun highestCombinedCategoryWins() {
        val sessions = day(
            exercise(ExerciseClassification.CHEST.id, 20.0, 5),
            exercise(ExerciseClassification.UPPER_LEGS.id, 30.0, 5),
            exercise(ExerciseClassification.LOWER_LEGS.id, 20.0, 5),
        )
        assertEquals(CalendarColorCategory.LEGS, categoryOf(sessions))
    }

    @Test
    fun equalVolumeUsesFixedCategoryOrder() {
        val sessions = day(
            exercise(ExerciseClassification.BACK.id, 10.0, 10),
            exercise(ExerciseClassification.CHEST.id, 20.0, 5),
            exercise(ExerciseClassification.SHOULDERS.id, 25.0, 4),
        )
        assertEquals(CalendarColorCategory.CHEST, categoryOf(sessions))
        assertEquals(
            CalendarColorCategory.BACK,
            CalendarDayColorLogic.dominantCategory(
                mapOf(
                    CalendarColorCategory.OTHER to 40.0,
                    CalendarColorCategory.BACK to 40.0,
                    CalendarColorCategory.ARMS to 40.0,
                ),
            ),
        )
    }

    @Test
    fun dayWithoutSetsHasNoColor() {
        val paints = CalendarDayColorLogic.paintsFor(
            sessions = listOf(session(4, emptyList())),
            measurements = emptyList(),
            stored = emptyMap(),
        )
        assertNull(paints[4])
    }

    @Test
    fun specialDaysOverrideTrainingAndUseFixedColors() {
        val trained = day(chest(40.0, 5))
        assertEquals(0xFFFFEB3B.toInt(), colorOf(trained, DayKind.LAZY))
        assertEquals(0xFFEC407A.toInt(), colorOf(trained, DayKind.INDULGE))
        assertEquals(0xFFD8DDE3.toInt(), colorOf(trained, DayKind.REST))
    }

    @Test
    fun clearingSpecialDayRestoresPartColorOrEmptyBackground() {
        val trained = day(back(10.0, 10))
        val cleared = DayKindLogic.nextStored(hasTraining = true, stored = DayKind.LAZY, tapped = DayKind.LAZY)
        assertNull(cleared)
        assertEquals(0xFF64B5F6.toInt(), colorOf(trained, cleared))

        val emptyCleared = DayKindLogic.nextStored(hasTraining = false, stored = DayKind.REST, tapped = DayKind.REST)
        assertNull(
            CalendarDayColorLogic.paintsFor(emptyList(), emptyList(), emptyMap())[9],
        )
        assertNull(emptyCleared)
    }

    @Test
    fun conflictingSpecialMarksPreferLazyThenIndulgeThenRest() {
        assertEquals(
            CalendarSpecialDay.LAZY,
            CalendarDayColorLogic.specialDay(setOf(DayKind.REST, DayKind.INDULGE, DayKind.LAZY)),
        )
        assertEquals(
            CalendarSpecialDay.INDULGE,
            CalendarDayColorLogic.specialDay(setOf(DayKind.REST, DayKind.INDULGE, DayKind.TRAINING)),
        )
        assertEquals(
            CalendarSpecialDay.REST,
            CalendarDayColorLogic.specialDay(setOf(DayKind.REST, DayKind.TRAINING)),
        )
    }

    @Test
    fun bothViewsReadTheSamePaintAndEmptyDaysDoNotInherit() {
        val sessions = listOf(
            session(10, listOf(chest(20.0, 5))),
            session(40, listOf(shoulders(12.0, 8))),
        )
        val stored = mapOf(10L to DayKind.INDULGE)
        val first = CalendarDayColorLogic.paintsFor(sessions, emptyList(), stored)
        val second = CalendarDayColorLogic.paintsFor(sessions, emptyList(), stored)
        assertEquals(first, second)
        assertEquals(0xFFEC407A.toInt(), first.getValue(10).argb())
        assertEquals(0xFFFFB74D.toInt(), first.getValue(40).argb())
        assertNull(first[11])
        assertTrue(first.keys.none { it == 11L })
    }

    @Test
    fun explicitTrainingMarkWithoutSetsDoesNotInventAColor() {
        val paints = CalendarDayColorLogic.paintsFor(
            sessions = emptyList(),
            measurements = emptyList(),
            stored = mapOf(3L to DayKind.TRAINING),
        )
        assertNull(paints[3])
    }

    private fun colorOf(detail: SessionDetail, stored: DayKind? = null): Int? {
        return painted(detail, stored).argb()
    }

    private fun categoryOf(detail: SessionDetail): CalendarColorCategory {
        return (painted(detail, null) as CalendarDayPaint.Training).category
    }

    private fun painted(detail: SessionDetail, stored: DayKind?): CalendarDayPaint {
        val marks = if (stored == null) {
            emptyMap()
        } else {
            mapOf(detail.session.dateEpochDay to stored)
        }
        return CalendarDayColorLogic.paintsFor(listOf(detail), emptyList(), marks)
            .getValue(detail.session.dateEpochDay)
    }

    private fun day(vararg items: SessionExercise): SessionDetail = session(1, items.toList())

    private fun chest(kg: Double, reps: Int) = exercise(ExerciseClassification.CHEST.id, kg, reps)

    private fun back(kg: Double, reps: Int) = exercise(ExerciseClassification.BACK.id, kg, reps)

    private fun shoulders(kg: Double, reps: Int) = exercise(ExerciseClassification.SHOULDERS.id, kg, reps)

    private fun exercise(part: String, kg: Double, reps: Int): SessionExercise {
        val entity = ExerciseEntity(
            id = "exercise-$part",
            name = part,
            bodyPart = "OTHER",
            equipmentType = "OTHER",
            weightMode = "WEIGHTED",
            isCustom = true,
            aliases = null,
            createdAt = 1L,
            bodyCategoryId = part.takeIf { it.startsWith("body_") },
        ).let { created ->
            if (part.startsWith("body_")) created else created.copy(bodyPart = part, bodyCategoryId = null)
        }
        return SessionExercise(
            exercise = entity,
            sets = listOf(
                WorkoutSetEntity(
                    id = "set-$part",
                    sessionId = "session",
                    exerciseId = entity.id,
                    exerciseOrder = 0,
                    setIndex = 1,
                    weightMode = "WEIGHTED",
                    weightKg = kg,
                    reps = reps,
                    note = null,
                ),
            ),
        )
    }

    private fun session(day: Long, items: List<SessionExercise>): SessionDetail {
        return SessionDetail(
            session = WorkoutSessionEntity(
                id = "session-$day",
                dateEpochDay = day,
                note = null,
                createdAt = 1L,
            ),
            exercises = items,
        )
    }
}
