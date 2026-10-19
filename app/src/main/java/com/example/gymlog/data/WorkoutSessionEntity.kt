package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Reserved for future workout-day records. Not used by the exercise library UI. */
@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["dateEpochDay"])],
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val note: String?,
    val createdAt: Long,
)
