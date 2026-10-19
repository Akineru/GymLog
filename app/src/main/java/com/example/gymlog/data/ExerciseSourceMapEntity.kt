package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_source_map",
    primaryKeys = ["source", "sourceId"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["localExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["localExerciseId"]),
        Index(value = ["source", "localExerciseId"]),
    ],
)
data class ExerciseSourceMapEntity(
    val source: String,
    val sourceId: String,
    val localExerciseId: String,
    val matchType: String,
    val confidence: Double = 1.0,
)
