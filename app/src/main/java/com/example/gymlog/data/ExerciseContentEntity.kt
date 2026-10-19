package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_content",
    primaryKeys = ["exerciseId", "locale"],
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
data class ExerciseContentEntity(
    val exerciseId: String,
    val locale: String,
    val displayName: String?,
    val instructions: String?,
    val instructionStepsJson: String?,
)
