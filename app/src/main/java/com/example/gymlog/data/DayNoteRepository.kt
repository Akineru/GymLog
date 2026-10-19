package com.example.gymlog.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DayNoteRepository(private val dao: DayNoteDao) {
    fun observeMessages(): Flow<Map<Long, String>> {
        return dao.observeAll().map { notes ->
            notes.associate { it.dateEpochDay to it.message }
        }
    }

    suspend fun save(dateEpochDay: Long, message: String?) {
        val trimmed = message?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            dao.delete(dateEpochDay)
        } else {
            dao.upsert(DayNoteEntity(dateEpochDay = dateEpochDay, message = trimmed))
        }
    }
}
