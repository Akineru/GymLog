package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayKindDao {
    @Query("SELECT * FROM day_kinds")
    fun observeAll(): Flow<List<DayKindEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(kind: DayKindEntity)

    @Query("DELETE FROM day_kinds WHERE dateEpochDay = :dateEpochDay")
    suspend fun delete(dateEpochDay: Long)
}
