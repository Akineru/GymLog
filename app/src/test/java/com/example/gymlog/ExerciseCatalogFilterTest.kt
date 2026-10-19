package com.example.gymlog

import com.example.gymlog.data.EquipmentRefinement
import com.example.gymlog.data.ExerciseCatalogQuery
import com.example.gymlog.data.ExerciseClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExerciseCatalogFilterTest {
    @Test
    fun blankQueryKeepsBodyPartAndPrimaryEquipment() {
        val args = ExerciseCatalogQuery(
            text = "  ",
            bodyPart = ExerciseClassification.UPPER_LEGS,
            equipment = ExerciseClassification.LEVERAGE_MACHINE,
        ).repositoryArguments()
        assertEquals("", args.query)
        assertEquals(ExerciseClassification.UPPER_LEGS.id, args.bodyCategoryId)
        assertEquals(ExerciseClassification.LEVERAGE_MACHINE.id, args.equipmentCategoryId)
    }

    @Test
    fun textSearchKeepsTheSelectedCategories() {
        val query = ExerciseCatalogQuery(
            text = "深蹲",
            bodyPart = ExerciseClassification.CHEST,
            equipment = ExerciseClassification.BARBELL,
        )
        assertFalse(query.searchingAll)
        val args = query.repositoryArguments()
        assertEquals("深蹲", args.query)
        assertEquals(ExerciseClassification.CHEST.id, args.bodyCategoryId)
        assertEquals(ExerciseClassification.BARBELL.id, args.equipmentCategoryId)
    }

    @Test
    fun refinementDoesNotGuessANarrowerEquipmentType() {
        val query = ExerciseCatalogQuery(
            bodyPart = ExerciseClassification.CHEST,
            equipment = ExerciseClassification.BARBELL,
            refinement = EquipmentRefinement("BARBELL"),
        )
        assertFalse(query.searchingAll)
        val args = query.repositoryArguments()
        assertEquals(ExerciseClassification.CHEST.id, args.bodyCategoryId)
        assertEquals(ExerciseClassification.BARBELL.id, args.equipmentCategoryId)
    }
}
