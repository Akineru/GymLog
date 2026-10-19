package com.example.gymlog.data

/**
 * Browse filters for the exercise library.
 *
 * [equipment] is the stored primary category ([EquipmentType]). A future second
 * level, such as barbell or dumbbell, can be carried in [refinement]. Current
 * rows do not store that level, so a refinement must not narrow results by
 * guessing from the exercise name. Until a real field exists, refinement leaves
 * the primary category unchanged.
 */
data class EquipmentRefinement(val id: String)

data class ExerciseCatalogQuery(
    val text: String = "",
    val bodyPart: ExerciseClassification.BodyCategory = ExerciseClassification.ALL_BODY,
    val equipment: ExerciseClassification.EquipmentCategory = ExerciseClassification.ALL_EQUIPMENT,
    val refinement: EquipmentRefinement? = null,
) {
    val searchingAll: Boolean
        get() = text.isNotBlank() &&
            bodyPart.id.isBlank() &&
            equipment.id.isBlank()

    fun repositoryArguments(): CatalogRepositoryArguments {
        return CatalogRepositoryArguments(
            query = text.trim(),
            bodyCategoryId = bodyPart.id.ifBlank { null },
            equipmentCategoryId = equipment.id.ifBlank { null },
        )
    }
}

data class CatalogRepositoryArguments(
    val query: String,
    val bodyCategoryId: String?,
    val equipmentCategoryId: String?,
)
