package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayNoteDao {
    @Query("SELECT * FROM day_notes")
    fun observeAll(): Flow<List<DayNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: DayNoteEntity)

    @Query("DELETE FROM day_notes WHERE dateEpochDay = :dateEpochDay")
    suspend fun delete(dateEpochDay: Long)
}
