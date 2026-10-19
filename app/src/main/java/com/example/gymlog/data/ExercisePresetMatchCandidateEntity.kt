package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Explicit, auditable candidate list for the 89 legacy presets. Nothing here
 * is applied automatically: the rows are PENDING_CONFIRMATION, AMBIGUOUS or
 * NO_MATCH until a human mapping decision is made.
 */
@Entity(tableName = "exercise_preset_match_candidate")
data class ExercisePresetMatchCandidateEntity(
    @PrimaryKey val presetExerciseId: String,
    val nameZh: String,
    val datasetId: String?,
    val candidateDatasetIds: String,
    val status: String,
    val evidence: String?,
)
