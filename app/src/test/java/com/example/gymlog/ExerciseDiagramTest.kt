package com.example.gymlog

import com.example.gymlog.data.PresetExercises
import com.example.gymlog.ui.exercise.hasSpecificDiagram
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDiagramTest {
    @Test
    fun everyPresetHasItsOwnDiagram() {
        val missing = PresetExercises.all(0L)
            .map { it.id }
            .filterNot(::hasSpecificDiagram)
        assertTrue(missing.joinToString(), missing.isEmpty())
    }
}
