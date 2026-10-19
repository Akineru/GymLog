package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Raw muscle tokens from the external dataset. Unlike exercise_muscles, this
 * table keeps even ambiguous/unmapped tokens so they are never silently lost.
 *
 * [bucket] is TARGET, MUSCLE_GROUP or SECONDARY.
 */
@Entity(
    tableName = "exercise_muscle_tokens",
    primaryKeys = ["exerciseId", "bucket", "token"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exerciseId"])],
)
data class ExerciseMuscleTokenEntity(
    val exerciseId: String,
    val bucket: String,
    val token: String,
    val sortOrder: Int,
    val mappingStatus: String,
    val candidateMuscleIds: String?,
)
