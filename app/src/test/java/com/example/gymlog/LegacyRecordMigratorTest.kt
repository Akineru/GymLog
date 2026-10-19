package com.example.gymlog

import com.example.gymlog.data.LegacyRecordMigrator
import com.example.gymlog.data.WeightMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRecordMigratorTest {
    @Test
    fun splitsAggregatedSetsAndKeepsOrdinaryWeight() {
        val result = LegacyRecordMigrator.convert(
            recordId = 7L,
            dateEpochDay = 20000L,
            exerciseName = "杠铃卧推",
            weightKg = 0.0,
            reps = 8,
            sets = 3,
            note = "旧记录",
            existingExerciseId = "preset_barbell_bench_press",
            createdAt = 1L,
        )
        assertEquals("migrated_session_7", result.session.id)
        assertEquals("旧记录", result.session.note)
        assertNull(result.newExercise)
        assertEquals(3, result.sets.size)
        result.sets.forEachIndexed { index, set ->
            assertEquals(index + 1, set.setIndex)
            assertEquals(WeightMode.WEIGHTED.name, set.weightMode)
            assertEquals(0.0, set.weightKg!!, 0.0)
            assertEquals(8, set.reps)
            assertEquals("preset_barbell_bench_press", set.exerciseId)
        }
    }

    @Test
    fun createsCustomExerciseWhenNameUnknown() {
        val result = LegacyRecordMigrator.convert(
            recordId = 3L,
            dateEpochDay = 1L,
            exerciseName = "自制动作",
            weightKg = 15.5,
            reps = 10,
            sets = 0,
            note = "",
            existingExerciseId = null,
            createdAt = 9L,
        )
        assertTrue(result.newExercise!!.isCustom)
        assertEquals("自制动作", result.newExercise!!.name)
        assertEquals(1, result.sets.size)
        assertEquals("migrated_ex_3", result.sets.first().exerciseId)
    }
}
