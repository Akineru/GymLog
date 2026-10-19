package com.example.gymlog.data

/**
 * Converts a v1 aggregated workout_records row into one session and per-set rows.
 * Old rows stored one weight/reps for every set, so each generated set copies those values
 * as ordinary weight. 0 kg stays ordinary weight — it is never treated as bodyweight.
 */
object LegacyRecordMigrator {
    data class Result(
        val session: WorkoutSessionEntity,
        val newExercise: ExerciseEntity?,
        val exerciseId: String,
        val sets: List<WorkoutSetEntity>,
    )

    fun convert(
        recordId: Long,
        dateEpochDay: Long,
        exerciseName: String,
        weightKg: Double,
        reps: Int,
        sets: Int,
        note: String,
        existingExerciseId: String?,
        createdAt: Long,
    ): Result {
        val sessionId = "migrated_session_$recordId"
        val exerciseId = existingExerciseId ?: "migrated_ex_$recordId"
        val newExercise = if (existingExerciseId == null) {
            ExerciseEntity(
                id = exerciseId,
                name = exerciseName,
                bodyPart = BodyPart.OTHER.name,
                equipmentType = EquipmentType.OTHER.name,
                weightMode = WeightMode.WEIGHTED.name,
                isCustom = true,
                aliases = null,
                createdAt = createdAt,
                bodyCategoryId = ExerciseClassification.LEGACY_BODY_OTHER.id,
                equipmentCategoryId = ExerciseClassification.LEGACY_EQUIPMENT_OTHER.id,
                classificationStatus = ExerciseClassification.STATUS_AMBIGUOUS,
                classificationVersion = ExerciseClassification.VERSION,
            )
        } else {
            null
        }
        val setCount = maxOf(sets, 1)
        val setEntities = (1..setCount).map { index ->
            WorkoutSetEntity(
                id = "migrated_set_${recordId}_$index",
                sessionId = sessionId,
                exerciseId = exerciseId,
                exerciseOrder = 0,
                setIndex = index,
                weightKg = weightKg,
                reps = reps,
                note = null,
                weightMode = WeightMode.WEIGHTED.name,
            )
        }
        return Result(
            session = WorkoutSessionEntity(
                id = sessionId,
                dateEpochDay = dateEpochDay,
                note = note.ifBlank { null },
                createdAt = createdAt,
            ),
            newExercise = newExercise,
            exerciseId = exerciseId,
            sets = setEntities,
        )
    }
}
