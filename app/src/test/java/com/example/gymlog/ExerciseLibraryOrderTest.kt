package com.example.gymlog

import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseUsageRow
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.sortedByTrainingCount
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseLibraryOrderTest {
    @Test
    fun moreTrainedExercisesComeFirst() {
        val ordered = listOf(
            exercise("squat", "深蹲"),
            exercise("bench", "卧推"),
            exercise("row", "划船"),
        ).sortedByTrainingCount(
            listOf(
                ExerciseUsageRow("bench", sessionCount = 1, lastEpochDay = 30),
                ExerciseUsageRow("squat", sessionCount = 4, lastEpochDay = 10),
            ),
        )
        assertEquals(listOf("squat", "bench", "row"), ordered.map { it.id })
    }

    @Test
    fun sameCountUsesMoreRecentTraining() {
        val ordered = listOf(
            exercise("curl", "弯举"),
            exercise("press", "推举"),
        ).sortedByTrainingCount(
            listOf(
                ExerciseUsageRow("curl", sessionCount = 2, lastEpochDay = 5),
                ExerciseUsageRow("press", sessionCount = 2, lastEpochDay = 20),
            ),
        )
        assertEquals(listOf("press", "curl"), ordered.map { it.id })
    }

    private fun exercise(id: String, name: String): ExerciseEntity {
        return ExerciseEntity(
            id = id,
            name = name,
            bodyPart = BodyPart.CHEST.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = false,
            aliases = null,
            createdAt = 1L,
        )
    }
}
