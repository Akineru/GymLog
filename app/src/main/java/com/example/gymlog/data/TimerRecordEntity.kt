package com.example.gymlog.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "timer_records")
data class TimerRecordEntity(
    @PrimaryKey val id: String,
    val durationMillis: Long,
    val dateEpochDay: Long,
    val sessionId: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "timer_laps",
    foreignKeys = [
        ForeignKey(
            entity = TimerRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["recordId"])],
)
data class TimerLapEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    val lapIndex: Int,
    val intervalMillis: Long,
    val elapsedMillis: Long,
)

data class TimerRecordWithLaps(
    @Embedded val record: TimerRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val laps: List<TimerLapEntity>,
)
