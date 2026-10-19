package com.example.gymlog.data

import kotlinx.coroutines.flow.Flow

class TimerRecordRepository(
    private val dao: TimerRecordDao,
) {
    fun observeAll(): Flow<List<TimerRecordWithLaps>> = dao.observeAll()

    suspend fun save(record: TimerRecordEntity, laps: List<TimerLapEntity>) {
        dao.insert(record, laps)
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }

    suspend fun updateDuration(id: String, durationMillis: Long) {
        dao.updateDuration(id, durationMillis)
    }
}
