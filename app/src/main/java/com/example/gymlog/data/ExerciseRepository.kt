package com.example.gymlog.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val muscleDao: ExerciseMuscleDao,
) {
    fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    fun observeExercises(
        query: String,
        bodyCategoryId: String?,
        equipmentCategoryId: String?,
    ): Flow<List<ExerciseEntity>> {
        return combine(
            exerciseDao.observeFiltered(
                query = query.trim(),
                bodyCategoryId = bodyCategoryId.orEmpty(),
                equipmentCategoryId = equipmentCategoryId.orEmpty(),
                datasetSource = ExerciseSource.DATASET,
            ),
            exerciseDao.observeUsage(),
        ) { exercises, usage ->
            exercises.sortedByTrainingCount(usage)
        }
    }

    fun observeMuscleLinks(): Flow<List<MuscleLink>> {
        return muscleDao.observeAll().map { rows -> rows.map { it.toLink() } }
    }

    suspend fun addCustomExercise(
        name: String,
        bodyCategory: ExerciseClassification.BodyCategory,
        equipmentCategory: ExerciseClassification.EquipmentCategory,
        weightMode: WeightMode,
        aliases: String?,
        muscles: List<MuscleLink> = emptyList(),
    ): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("EMPTY_NAME"))
        }
        if (!bodyCategory.standard || !equipmentCategory.standard) {
            return Result.failure(IllegalArgumentException("INVALID_CATEGORY"))
        }
        if (exerciseDao.countByName(trimmedName) > 0) {
            return Result.failure(IllegalStateException("DUPLICATE_NAME"))
        }
        val id = "custom_${UUID.randomUUID()}"
        val trimmedAliases = aliases?.trim().orEmpty().ifEmpty { null }
        val links = muscles.mapIndexed { index, link -> link.copy(exerciseId = id, sortOrder = index) }
        MuscleMapLogic.validateLinks(setOf(id), links)
        exerciseDao.insertCustom(
            ExerciseEntity(
                id = id,
                name = trimmedName,
                // Keep the legacy broad fields for historical grouping/fallbacks,
                // but the new stable classification ids are authoritative.
                bodyPart = ExerciseClassification.legacyBodyPartFor(bodyCategory).name,
                equipmentType = ExerciseClassification.legacyEquipmentFor(equipmentCategory).name,
                bodyPartDetail = bodyCategory.rawValue,
                equipmentDetail = equipmentCategory.rawValue,
                bodyCategoryId = bodyCategory.id,
                equipmentCategoryId = equipmentCategory.id,
                classificationStatus = ExerciseClassification.STATUS_MAPPED,
                classificationVersion = ExerciseClassification.VERSION,
                weightMode = weightMode.name,
                isCustom = true,
                aliases = trimmedAliases,
                createdAt = System.currentTimeMillis(),
                source = ExerciseSource.CUSTOM,
            ),
        )
        if (links.isNotEmpty()) muscleDao.insertAll(links.map { it.toEntity() })
        return Result.success(Unit)
    }

    suspend fun replaceCustomMuscles(exerciseId: String, muscles: List<MuscleLink>) {
        val exercise = exerciseDao.getById(exerciseId)
        if (exercise == null || !exercise.isCustom) return
        val links = muscles.mapIndexed { index, link -> link.copy(exerciseId = exerciseId, sortOrder = index) }
        MuscleMapLogic.validateLinks(setOf(exerciseId), links)
        muscleDao.deleteForExercise(exerciseId)
        if (links.isNotEmpty()) muscleDao.insertAll(links.map { it.toEntity() })
    }

    suspend fun deleteExercise(id: String) {
        exerciseDao.archive(id)
    }
}
