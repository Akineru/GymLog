package com.example.gymlog.data

import androidx.room.withTransaction

/**
 * Fills the stable category columns for rows that predate the classification
 * schema, or for rows restored from an older backup.
 *
 * The migration is per-row and idempotent: a row at
 * [ExerciseClassification.VERSION] with both category ids is never rewritten.
 * Broad legacy categories are preserved in compatibility buckets instead of
 * being guessed into a narrower dataset category.
 */
class ExerciseClassificationMigrator(
    private val database: GymLogDatabase,
) {
    suspend fun migrateAll() {
        val dao = database.exerciseDao()
        val rows = dao.allExercises()
            .filter { row ->
                row.classificationVersion < ExerciseClassification.VERSION ||
                    row.bodyCategoryId == null ||
                    row.equipmentCategoryId == null
            }
        if (rows.isEmpty()) return
        database.withTransaction {
            rows.forEach { row ->
                val body = if (row.source == ExerciseSource.DATASET) {
                    ExerciseClassification.bodyForRaw(row.bodyPartDetail)
                } else {
                    ExerciseClassification.bodyForRaw(row.bodyPartDetail)
                        ?: ExerciseClassification.bodyForLegacy(row.bodyPart)
                }
                val equipment = if (row.source == ExerciseSource.DATASET) {
                    ExerciseClassification.equipmentForRaw(row.equipmentDetail)
                } else {
                    ExerciseClassification.equipmentForRaw(row.equipmentDetail)
                        ?: ExerciseClassification.equipmentForLegacy(row.equipmentType)
                }
                val status = when {
                    row.source == ExerciseSource.DATASET ->
                        if (body != null && equipment != null) {
                            ExerciseClassification.STATUS_DATASET
                        } else {
                            ExerciseClassification.STATUS_UNMAPPED
                        }
                    body == null || equipment == null -> ExerciseClassification.STATUS_UNMAPPED
                    else -> ExerciseClassification.statusFor(body, equipment)
                }
                dao.updateClassification(
                    id = row.id,
                    bodyCategoryId = (body ?: ExerciseClassification.UNKNOWN_BODY).id,
                    equipmentCategoryId =
                        (equipment ?: ExerciseClassification.UNKNOWN_EQUIPMENT).id,
                    classificationStatus = status,
                    classificationVersion = ExerciseClassification.VERSION,
                )
            }
        }
    }
}
