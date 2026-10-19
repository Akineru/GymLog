package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val sets: Int,
    val note: String,
)
