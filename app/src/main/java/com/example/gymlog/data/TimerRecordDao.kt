package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TimerRecordDao {
    @Transaction
    @Query("SELECT * FROM timer_records ORDER BY createdAt DESC")
    abstract fun observeAll(): Flow<List<TimerRecordWithLaps>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRecord(record: TimerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRecord(record: TimerRecordEntity)

    @Query("DELETE FROM timer_records WHERE sessionId = :sessionId")
    abstract suspend fun deleteBySession(sessionId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertLaps(laps: List<TimerLapEntity>)

    @Query("DELETE FROM timer_records WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Query("UPDATE timer_records SET durationMillis = :durationMillis WHERE id = :id")
    abstract suspend fun updateDuration(id: String, durationMillis: Long)

    @Transaction
    open suspend fun insert(record: TimerRecordEntity, laps: List<TimerLapEntity>) {
        insertRecord(record)
        if (laps.isNotEmpty()) {
            insertLaps(laps)
        }
    }

    @Transaction
    open suspend fun replaceSessionRecord(record: TimerRecordEntity) {
        record.sessionId?.let { deleteBySession(it) }
        upsertRecord(record)
    }
}
