package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExerciseDurationDao {
    @Query("SELECT * FROM exercise_durations ORDER BY createdAt ASC")
    abstract fun observeAll(): Flow<List<ExerciseDurationEntity>>

    @Query("SELECT * FROM exercise_durations WHERE sessionId = :sessionId ORDER BY exerciseOrder ASC")
    abstract suspend fun forSession(sessionId: String): List<ExerciseDurationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(rows: List<ExerciseDurationEntity>)

    @Query("DELETE FROM exercise_durations WHERE sessionId = :sessionId")
    abstract suspend fun deleteForSession(sessionId: String)

    @Transaction
    open suspend fun replaceForSession(sessionId: String, rows: List<ExerciseDurationEntity>) {
        deleteForSession(sessionId)
        if (rows.isNotEmpty()) {
            upsertAll(rows)
        }
    }
}
