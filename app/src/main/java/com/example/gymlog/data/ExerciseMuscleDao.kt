package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseMuscleDao {
    @Query("SELECT * FROM exercise_muscles")
    fun observeAll(): Flow<List<ExerciseMuscleEntity>>

    @Query("SELECT * FROM exercise_muscles")
    suspend fun all(): List<ExerciseMuscleEntity>

    @Query("SELECT COUNT(*) FROM exercise_muscles")
    suspend fun countAll(): Int

    @Query("DELETE FROM exercise_muscles WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ExerciseMuscleEntity>)
}
