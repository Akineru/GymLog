package com.example.gymlog

import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.PresetExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetExercisesTest {
    @Test
    fun presetIdsAreStableAndUnique() {
        val presets = PresetExercises.all(0L)
        val ids = presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(presets.all { it.id.startsWith("preset_") })
        assertTrue(presets.none { it.isCustom })
        assertTrue(presets.any { it.bodyPart == BodyPart.CHEST.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.SHOULDERS.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.BACK.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.LEGS.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.ARMS.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.ABS.name })
        assertTrue(presets.any { it.bodyPart == BodyPart.OTHER.name })
        assertTrue(presets.any { it.equipmentType == EquipmentType.FREE_WEIGHT.name })
        assertTrue(presets.any { it.equipmentType == EquipmentType.MACHINE.name })
        assertTrue(presets.any { it.equipmentType == EquipmentType.OTHER.name })
    }
}
