package com.example.gymlog

import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.data.orderedForDay
import com.example.gymlog.data.partVolumes
import org.junit.Assert.assertEquals
import org.junit.Test

class DayDisplayOrderTest {
    @Test
    fun largerVolumeComesFirstAndSamePartStaysTogether() {
        val chestSmall = session("chest-small", exercise("bench", BodyPart.CHEST, 20.0, 5))
        val back = session("back", exercise("row", BodyPart.BACK, 50.0, 8))
        val chestLarge = session("chest-large", exercise("press", BodyPart.CHEST, 40.0, 8))

        val ordered = listOf(chestSmall, back, chestLarge).orderedForDay(emptyList())

        assertEquals(listOf("chest-large", "chest-small", "back"), ordered.map { it.session.id })
    }

    @Test
    fun partVolumesSumTheSamePartAcrossSessions() {
        val chestSmall = session("chest-small", exercise("bench", BodyPart.CHEST, 20.0, 5))
        val back = session("back", exercise("row", BodyPart.BACK, 50.0, 8))
        val chestLarge = session("chest-large", exercise("press", BodyPart.CHEST, 40.0, 8))

        val volumes = listOf(chestSmall, back, chestLarge).partVolumes(emptyList())

        assertEquals(listOf("胸" to 420.0, "背" to 400.0), volumes)
    }

    @Test
    fun zeroVolumeSetsStillCountForThePartSummary() {
        val zero = session("zero", exercise("empty", BodyPart.CHEST, 0.0, 0))

        assertEquals(listOf("胸" to 0.0), listOf(zero).partVolumes(emptyList()))
        assertEquals(listOf("zero"), listOf(zero).orderedForDay(emptyList()).map { it.session.id })
    }

    @Test
    fun exercisesInsideASessionFollowTheSameOrder() {
        val mixed = session(
            "mixed",
            exercise("curl", BodyPart.ARMS, 10.0, 10),
            exercise("squat", BodyPart.LEGS, 100.0, 5),
            exercise("extension", BodyPart.ARMS, 20.0, 8),
        )

        val ordered = listOf(mixed).orderedForDay(emptyList()).single()

        assertEquals(
            listOf("squat", "extension", "curl"),
            ordered.exercises.map { it.exercise.id },
        )
    }

    private fun session(id: String, vararg exercises: SessionExercise): SessionDetail {
        return SessionDetail(
            session = WorkoutSessionEntity(id = id, dateEpochDay = 0, note = null, createdAt = 0),
            exercises = exercises.toList(),
        )
    }

    private fun exercise(id: String, part: BodyPart, weightKg: Double, reps: Int): SessionExercise {
        return SessionExercise(
            exercise = ExerciseEntity(
                id = id,
                name = id,
                bodyPart = part.name,
                equipmentType = EquipmentType.FREE_WEIGHT.name,
                weightMode = WeightMode.WEIGHTED.name,
                isCustom = false,
                aliases = null,
                createdAt = 0,
            ),
            sets = listOf(
                WorkoutSetEntity(
                    id = "$id-set",
                    sessionId = "session",
                    exerciseId = id,
                    exerciseOrder = 0,
                    setIndex = 0,
                    weightMode = WeightMode.WEIGHTED.name,
                    weightKg = weightKg,
                    reps = reps,
                    note = null,
                ),
            ),
        )
    }
}
