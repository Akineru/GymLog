package com.example.gymlog

import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindLogic
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayKindLogicTest {
    @Test
    fun trainingRecordDefaultsToTrainingDay() {
        assertEquals(DayKind.TRAINING, DayKindLogic.resolved(hasTraining = true, stored = null))
        assertNull(DayKindLogic.resolved(hasTraining = false, stored = null))
    }

    @Test
    fun explicitChoiceOverridesTrainingDefault() {
        assertEquals(DayKind.REST, DayKindLogic.resolved(hasTraining = true, stored = DayKind.REST))
        assertEquals(DayKind.INDULGE, DayKindLogic.resolved(hasTraining = false, stored = DayKind.INDULGE))
        assertEquals(DayKind.LAZY, DayKindLogic.resolved(hasTraining = false, stored = DayKind.LAZY))
    }

    @Test
    fun choosingTrainingOnATrainedDayDoesNotStoreAnOverride() {
        assertNull(DayKindLogic.nextStored(hasTraining = true, stored = null, tapped = DayKind.TRAINING))
        assertNull(DayKindLogic.nextStored(hasTraining = true, stored = DayKind.REST, tapped = DayKind.TRAINING))
    }

    @Test
    fun otherThreeCanBeChosenAndCleared() {
        assertEquals(DayKind.REST, DayKindLogic.nextStored(true, null, DayKind.REST))
        assertEquals(DayKind.INDULGE, DayKindLogic.nextStored(false, null, DayKind.INDULGE))
        assertEquals(DayKind.LAZY, DayKindLogic.nextStored(true, DayKind.REST, DayKind.LAZY))
        assertNull(DayKindLogic.nextStored(true, DayKind.REST, DayKind.REST))
        assertNull(DayKindLogic.nextStored(false, DayKind.LAZY, DayKind.LAZY))
    }

    @Test
    fun trainingDayCanBeChosenWithoutARecord() {
        assertEquals(DayKind.TRAINING, DayKindLogic.nextStored(false, null, DayKind.TRAINING))
        assertNull(DayKindLogic.nextStored(false, DayKind.TRAINING, DayKind.TRAINING))
    }

    @Test
    fun emptySessionDoesNotCountAsTraining() {
        val sessions = listOf(
            session(day = 10, sets = 0),
            session(day = 11, sets = 1),
        )
        assertEquals(setOf(11L), DayKindLogic.trainedEpochDays(sessions))
        val resolved = DayKindLogic.resolveAll(sessions, mapOf(12L to DayKind.LAZY))
        assertEquals(DayKind.TRAINING, resolved[11L])
        assertEquals(DayKind.LAZY, resolved[12L])
        assertNull(resolved[10L])
    }

    private fun session(day: Long, sets: Int): SessionDetail {
        val exercise = ExerciseEntity(
            id = "curl",
            name = "弯举",
            bodyPart = "ARMS",
            equipmentType = "FREE_WEIGHT",
            weightMode = "WEIGHTED",
            isCustom = true,
            aliases = null,
            createdAt = 1L,
        )
        val setRows = List(sets) { index ->
            WorkoutSetEntity(
                id = "set-$day-$index",
                sessionId = "session-$day",
                exerciseId = exercise.id,
                exerciseOrder = 0,
                setIndex = index,
                weightMode = "WEIGHTED",
                weightKg = 10.0,
                reps = 8,
                note = null,
            )
        }
        return SessionDetail(
            session = WorkoutSessionEntity(id = "session-$day", dateEpochDay = day, note = null, createdAt = 1L),
            exercises = if (sets == 0) {
                emptyList()
            } else {
                listOf(SessionExercise(exercise = exercise, sets = setRows))
            },
        )
    }
}
