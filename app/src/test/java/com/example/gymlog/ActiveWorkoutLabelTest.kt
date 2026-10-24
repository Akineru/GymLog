package com.example.gymlog

import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.WeightMode
import com.example.gymlog.ui.session.DraftExerciseUi
import com.example.gymlog.ui.session.DraftSetUi
import com.example.gymlog.ui.train.currentExerciseLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveWorkoutLabelTest {
    @Test
    fun firstUnfinishedExerciseIsTheLabel() {
        val exercises = listOf(
            finished(ExerciseClassification.CHEST.id),
            unfinished(ExerciseClassification.BACK.id),
            unfinished(ExerciseClassification.UPPER_LEGS.id),
        )
        assertEquals("背部-划船", currentExerciseLabel(exercises))
    }

    @Test
    fun onlyExerciseIsTheLabel() {
        assertEquals(
            "胸部-卧推",
            currentExerciseLabel(listOf(unfinished(ExerciseClassification.CHEST.id))),
        )
    }

    @Test
    fun everyExerciseFinishedLeavesNoLabel() {
        val exercises = listOf(
            finished(ExerciseClassification.CHEST.id),
            finished(ExerciseClassification.BACK.id),
        )
        assertNull(currentExerciseLabel(exercises))
    }

    @Test
    fun nothingAddedYetLeavesNoLabel() {
        assertNull(currentExerciseLabel(emptyList()))
    }

    @Test
    fun labelFollowsTheOrderOfTheDraft() {
        val chest = unfinished(ExerciseClassification.CHEST.id)
        val back = unfinished(ExerciseClassification.BACK.id)
        assertEquals("胸部-卧推", currentExerciseLabel(listOf(chest, back)))
        assertEquals("背部-划船", currentExerciseLabel(listOf(back, chest)))
    }

    @Test
    fun tickingEverySetWithoutFinishingKeepsTheExerciseCurrent() {
        val exercise = DraftExerciseUi(
            id = "draft",
            exercise = exerciseEntity(ExerciseClassification.CHEST.id),
            sets = listOf(
                DraftSetUi(
                    id = "set",
                    mode = WeightMode.WEIGHTED,
                    normalKgText = "60",
                    extraKgText = "",
                    repsText = "8",
                    completed = true,
                ),
            ),
        )
        assertEquals("胸部-卧推", currentExerciseLabel(listOf(exercise)))
    }

    private fun unfinished(part: String) = DraftExerciseUi(
        id = "draft-$part",
        exercise = exerciseEntity(part),
        sets = emptyList(),
    )

    private fun finished(part: String) = unfinished(part).copy(completedAtMillis = 1_000L)

    private fun exerciseEntity(part: String): ExerciseEntity {
        val name = if (part == ExerciseClassification.BACK.id) "划船" else "卧推"
        return ExerciseEntity(
            id = "exercise-$part",
            name = name,
            bodyPart = "OTHER",
            equipmentType = "OTHER",
            weightMode = "WEIGHTED",
            isCustom = true,
            aliases = null,
            createdAt = 1L,
            bodyCategoryId = part,
        )
    }
}
