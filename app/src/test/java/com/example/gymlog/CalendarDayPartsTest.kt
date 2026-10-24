package com.example.gymlog

import com.example.gymlog.data.CalendarDayPartsLogic
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarDayPartsTest {
    @Test
    fun benchAndSquatReadAsChestPlusLegs() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    1,
                    exercise(ExerciseClassification.CHEST.id, 60.0, 5),
                    exercise(ExerciseClassification.UPPER_LEGS.id, 40.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸+腿", labels[1])
    }

    @Test
    fun heavierSquatThanBenchPutsLegsFirst() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    1,
                    exercise(ExerciseClassification.CHEST.id, 60.0, 5),
                    exercise(ExerciseClassification.UPPER_LEGS.id, 100.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("腿+胸", labels[1])
    }

    @Test
    fun upperAndLowerLegsMergeIntoOneLegsLabel() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    2,
                    exercise(ExerciseClassification.UPPER_LEGS.id, 100.0, 5),
                    exercise(ExerciseClassification.LOWER_LEGS.id, 20.0, 20),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("腿", labels[2])
    }

    @Test
    fun upperAndLowerArmsMergeIntoOneArmsLabel() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    3,
                    exercise(ExerciseClassification.UPPER_ARMS.id, 20.0, 8),
                    exercise(ExerciseClassification.LOWER_ARMS.id, 10.0, 15),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("手臂", labels[3])
    }

    @Test
    fun waistCardioAndNeckReadAsOther() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    4,
                    exercise(ExerciseClassification.WAIST.id, 20.0, 10),
                    exercise(ExerciseClassification.CARDIO.id, 1.0, 50),
                    exercise(ExerciseClassification.NECK.id, 5.0, 10),
                    exercise("NOT_A_PART", 5.0, 10),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("其他", labels[4])
    }

    @Test
    fun highestVolumeComesFirst() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    5,
                    exercise(ExerciseClassification.CHEST.id, 20.0, 5),
                    exercise(ExerciseClassification.UPPER_LEGS.id, 100.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("腿+胸", labels[5])
    }

    @Test
    fun equalVolumeFallsBackToCategoryOrder() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    6,
                    exercise(ExerciseClassification.UPPER_LEGS.id, 50.0, 5),
                    exercise(ExerciseClassification.CHEST.id, 50.0, 5),
                    exercise(ExerciseClassification.BACK.id, 50.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸+背+腿", labels[6])
    }

    @Test
    fun moreThanThreePartsAreFoldedIntoAnEllipsis() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    7,
                    exercise(ExerciseClassification.CHEST.id, 50.0, 5),
                    exercise(ExerciseClassification.BACK.id, 40.0, 5),
                    exercise(ExerciseClassification.SHOULDERS.id, 30.0, 5),
                    exercise(ExerciseClassification.UPPER_LEGS.id, 20.0, 5),
                    exercise(ExerciseClassification.UPPER_ARMS.id, 10.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸+背…", labels[7])
    }

    @Test
    fun threeSingleCharPartsFitWithoutEllipsis() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    71,
                    exercise(ExerciseClassification.CHEST.id, 50.0, 5),
                    exercise(ExerciseClassification.BACK.id, 40.0, 5),
                    exercise(ExerciseClassification.SHOULDERS.id, 30.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸+背+肩", labels[71])
    }

    @Test
    fun twoCharLabelStillFitsInTwoParts() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(
                    72,
                    exercise(ExerciseClassification.CHEST.id, 50.0, 5),
                    exercise(ExerciseClassification.UPPER_ARMS.id, 40.0, 5),
                ),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸+手臂", labels[72])
    }

    @Test
    fun dayWithoutSetsHasNoLabel() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(session(8, emptyList())),
            measurements = emptyList(),
        )
        assertNull(labels[8])
    }

    @Test
    fun severalDaysAreIndependent() {
        val labels = CalendarDayPartsLogic.labelsFor(
            sessions = listOf(
                session(10, exercise(ExerciseClassification.CHEST.id, 60.0, 5)),
                session(11, exercise(ExerciseClassification.UPPER_LEGS.id, 80.0, 5)),
            ),
            measurements = emptyList(),
        )
        assertEquals("胸", labels[10])
        assertEquals("腿", labels[11])
        assertNull(labels[12])
    }

    private fun exercise(part: String, kg: Double, reps: Int): SessionExercise {
        val entity = ExerciseEntity(
            id = "exercise-$part",
            name = part,
            bodyPart = if (part.startsWith("body_")) "OTHER" else part,
            equipmentType = "OTHER",
            weightMode = "WEIGHTED",
            isCustom = true,
            aliases = null,
            createdAt = 1L,
            bodyCategoryId = part.takeIf { it.startsWith("body_") },
        )
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

    private fun session(day: Long, vararg items: SessionExercise): SessionDetail =
        session(day, items.toList())
}
