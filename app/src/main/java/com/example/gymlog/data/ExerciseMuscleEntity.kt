package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseId", "muscleId"],
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
data class ExerciseMuscleEntity(
    val exerciseId: String,
    val muscleId: String,
    val role: String,
    val sortOrder: Int,
)

fun ExerciseMuscleEntity.toLink(): MuscleLink {
    return MuscleLink(
        exerciseId = exerciseId,
        muscleId = muscleId,
        role = MuscleRole.valueOf(role),
        sortOrder = sortOrder,
    )
}

fun MuscleLink.toEntity(): ExerciseMuscleEntity {
    return ExerciseMuscleEntity(
        exerciseId = exerciseId,
        muscleId = muscleId,
        role = role.name,
        sortOrder = sortOrder,
    )
}
