package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_measurements",
    indices = [Index(value = ["recordedEpochDay"])],
)
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val recordedEpochDay: Long,
    val heightCm: Double?,
    val weightKg: Double?,
    val bodyFatPercent: Double?,
    val waistCm: Double? = null,
    val armCm: Double? = null,
    val armLeftCm: Double? = null,
    val armRightCm: Double? = null,
    val forearmLeftCm: Double? = null,
    val forearmRightCm: Double? = null,
    val chestCm: Double? = null,
    val hipCm: Double? = null,
    val thighLeftCm: Double? = null,
    val thighRightCm: Double? = null,
    val calfLeftCm: Double? = null,
    val calfRightCm: Double? = null,
    val createdAt: Long,
)
