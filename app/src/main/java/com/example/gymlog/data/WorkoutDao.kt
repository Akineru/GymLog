package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_records ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutRecord>>

    @Insert
    suspend fun insert(record: WorkoutRecord)

    @Update
    suspend fun update(record: WorkoutRecord)

    @Delete
    suspend fun delete(record: WorkoutRecord)
}
