package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_kinds")
data class DayKindEntity(
    @PrimaryKey val dateEpochDay: Long,
    val kind: String,
)
