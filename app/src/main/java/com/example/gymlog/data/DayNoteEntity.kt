package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_notes")
data class DayNoteEntity(
    @PrimaryKey val dateEpochDay: Long,
    val message: String,
)
