package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_import_state")
data class ExerciseImportStateEntity(
    @PrimaryKey val source: String,
    val catalogVersion: Int,
    val importedAt: Long,
    val recordCount: Int,
    val added: Int,
    val linked: Int,
    val skipped: Int,
    val pendingMatches: Int,
    val unmappedTokens: Int,
    val failed: Int,
)
