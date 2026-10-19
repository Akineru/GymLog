package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyRecordDao {
    @Query("SELECT * FROM body_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<BodyProfileEntity?>

    @Query("SELECT * FROM body_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): BodyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: BodyProfileEntity)

    @Query("DELETE FROM body_profile WHERE id = 1")
    suspend fun clearProfile()

    @Query("SELECT * FROM body_measurements ORDER BY recordedEpochDay DESC, createdAt DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements")
    suspend fun listMeasurements(): List<BodyMeasurementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity)
}
