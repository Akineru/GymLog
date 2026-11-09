package com.example.gymlog

import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseDurationEntity
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseSource
import com.example.gymlog.data.GymLogSnapshot
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.data.withoutRetiredDatasetExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 旧备份里还带着已下线的重复动作，恢复前要过滤掉，并把训练组 / 动作用时改挂到保留动作。
 */
class RetiredDatasetBackupTest {

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        name = "杠杆机 胸部推举",
        bodyPart = BodyPart.CHEST.name,
        equipmentType = EquipmentType.MACHINE.name,
        weightMode = WeightMode.WEIGHTED.name,
        isCustom = false,
        aliases = null,
        createdAt = 1L,
        source = ExerciseSource.DATASET,
    )

    private fun set(id: String, exerciseId: String) = WorkoutSetEntity(
        id = id,
        sessionId = "s1",
        exerciseId = exerciseId,
        exerciseOrder = 0,
        setIndex = 1,
        weightMode = WeightMode.WEIGHTED.name,
        weightKg = 70.0,
        reps = 10,
        note = null,
    )

    private fun duration(id: String, exerciseId: String) = ExerciseDurationEntity(
        id = id,
        sessionId = "s1",
        exerciseId = exerciseId,
        exerciseOrder = 0,
        durationMillis = 60_000L,
        createdAt = 1L,
    )

    private fun snapshot(exercises: List<ExerciseEntity>, sets: List<WorkoutSetEntity>) = GymLogSnapshot(
        exercises = exercises,
        sessions = emptyList(),
        sets = sets,
        profile = null,
        measurements = emptyList(),
        legacyRecords = emptyList(),
        exerciseDurations = sets.map { duration("dur-${it.id}", it.exerciseId) },
    )

    @Test
    fun retiredExerciseIsDroppedAndItsSetsFollowTheKeptTwin() {
        val result = snapshot(
            exercises = listOf(exercise("ds_0576"), exercise("ds_0577")),
            sets = listOf(set("set1", "ds_0576"), set("set2", "ds_0577")),
        ).withoutRetiredDatasetExercises()

        assertEquals(listOf("ds_0577"), result.exercises.map { it.id })
        assertEquals(listOf("ds_0577", "ds_0577"), result.sets.map { it.exerciseId })
        assertEquals(listOf("ds_0577", "ds_0577"), result.exerciseDurations.map { it.exerciseId })
    }

    @Test
    fun snapshotWithoutRetiredExercisesIsReturnedAsIs() {
        val original = snapshot(
            exercises = listOf(exercise("ds_0577")),
            sets = listOf(set("set2", "ds_0577")),
        )

        assertTrue(original.withoutRetiredDatasetExercises() === original)
    }

    @Test
    fun setsReferencingARetiredExerciseWithoutKeeperAreDropped() {
        val result = snapshot(
            exercises = listOf(exercise("ds_0576")),
            sets = listOf(set("set1", "ds_0576")),
        ).withoutRetiredDatasetExercises()

        assertTrue(result.exercises.isEmpty())
        assertTrue(result.sets.isEmpty())
        assertTrue(result.exerciseDurations.isEmpty())
    }
}
