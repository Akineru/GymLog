package com.example.gymlog.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DayKindRepository(private val dao: DayKindDao) {
    fun observe(): Flow<Map<Long, DayKind>> {
        return dao.observeAll().map { rows ->
            rows.mapNotNull { row ->
                DayKind.fromStored(row.kind)?.let { row.dateEpochDay to it }
            }.toMap()
        }
    }

    suspend fun save(dateEpochDay: Long, kind: DayKind?) {
        if (kind == null) {
            dao.delete(dateEpochDay)
        } else {
            dao.upsert(DayKindEntity(dateEpochDay = dateEpochDay, kind = kind.name))
        }
    }
}
