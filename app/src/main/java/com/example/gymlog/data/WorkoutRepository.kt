package com.example.gymlog.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import java.time.LocalDate
import java.util.UUID

data class SessionExercise(
    val exercise: ExerciseEntity,
    val sets: List<WorkoutSetEntity>,
    val durationMillis: Long? = null,
)

data class SessionDetail(
    val session: WorkoutSessionEntity,
    val exercises: List<SessionExercise>,
)

data class SessionSetDraft(
    val id: String,
    val mode: WeightMode,
    val normalKgText: String,
    val extraKgText: String,
    val repsText: String,
)

data class SessionExerciseDraft(
    val exercise: ExerciseEntity,
    val sets: List<SessionSetDraft>,
    val durationMillis: Long? = null,
)

class WorkoutRepository(
    private val sessionDao: WorkoutSessionDao,
    private val exerciseDao: ExerciseDao,
    private val bodyRecordDao: BodyRecordDao,
    private val exerciseDurationDao: ExerciseDurationDao? = null,
) {
    fun observeHistory(): Flow<List<SessionDetail>> {
        val durations = exerciseDurationDao?.observeAll() ?: flowOf(emptyList())
        return combine(sessionDao.observeAllWithSets(), durations) { rows, durationRows ->
            rows.toDetails(durationRows.groupBy { it.sessionId })
        }.flowOn(Dispatchers.IO)
    }

    fun observeAllSetsForPr(): Flow<List<SetForPrRow>> {
        return sessionDao.observeAllSetsForPr().flowOn(Dispatchers.IO)
    }

    fun observeTrainedPartsByDate(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Map<LocalDate, List<String>>> {
        return combine(
            sessionDao.observeSetsInRange(
                startEpochDay = startDate.toEpochDay(),
                endEpochDay = endDate.toEpochDay(),
            ),
            bodyRecordDao.observeMeasurements(),
        ) { sets, measurements ->
            val rows = sets.groupBy { it.dateEpochDay to it.part }.map { (key, daySets) ->
                DatePartRow(
                    dateEpochDay = key.first,
                    part = key.second,
                    volume = daySets.sumOf { set ->
                        val bodyWeight = WeightLogic.bodyWeightKg(measurements, set.dateEpochDay)
                        val load = WeightLogic.effectiveKg(
                            WeightMode.fromStorage(set.weightMode),
                            set.weightKg,
                            bodyWeight,
                        )
                        WeightLogic.setVolume(load, set.reps)
                    },
                )
            }
            groupDateParts(rows)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun latestSetForExercise(exerciseId: String): WorkoutSetEntity? {
        return sessionDao.latestSetForExercise(exerciseId)
    }

    suspend fun loadSession(id: String): SessionDetail? {
        val row = sessionDao.getWithSets(id) ?: return null
        val durations = exerciseDurationDao?.forSession(id).orEmpty()
        return listOf(row)
            .toDetails(mapOf(id to durations))
            .firstOrNull()
    }

    suspend fun saveSession(
        sessionId: String?,
        dateEpochDay: Long,
        note: String,
        exercises: List<SessionExerciseDraft>,
        durationMillis: Long = 0L,
    ): Result<String> {
        if (exercises.isEmpty()) {
            return Result.failure(IllegalArgumentException("NO_EXERCISES"))
        }
        val validated = ArrayList<WorkoutSetEntity>()
        exercises.forEachIndexed { exerciseIndex, draft ->
            if (draft.sets.isEmpty()) {
                return Result.failure(IllegalArgumentException("NO_SETS"))
            }
            draft.sets.forEachIndexed { index, set ->
                val weight = WeightLogic.storedWeightKg(
                    set.mode,
                    set.normalKgText,
                    set.extraKgText,
                )
                if (weight.isFailure) {
                    return Result.failure(weight.exceptionOrNull()!!)
                }
                val storedKg = weight.getOrNull()
                if (set.mode == WeightMode.BODYWEIGHT && storedKg != null) {
                    return Result.failure(IllegalArgumentException("BODYWEIGHT_HAS_KG"))
                }
                val reps = set.repsText.trim().toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("REPS_REQUIRED"))
                if (reps < 0) {
                    return Result.failure(IllegalArgumentException("REPS_REQUIRED"))
                }
                validated += WorkoutSetEntity(
                    id = set.id,
                    sessionId = "",
                    exerciseId = draft.exercise.id,
                    exerciseOrder = exerciseIndex,
                    setIndex = index + 1,
                    weightMode = set.mode.name,
                    weightKg = storedKg,
                    reps = reps,
                    note = null,
                )
            }
        }

        val previousExerciseIds = sessionId?.let { loadSession(it) }
            ?.exercises
            ?.map { it.exercise.id }
            .orEmpty()
        val id = sessionId ?: UUID.randomUUID().toString()
        val createdAt = sessionId?.let { sessionDao.getCreatedAt(it) } ?: System.currentTimeMillis()
        val session = WorkoutSessionEntity(
            id = id,
            dateEpochDay = dateEpochDay,
            note = note.trim().ifEmpty { null },
            createdAt = createdAt,
            durationMillis = durationMillis.coerceAtLeast(0L),
        )
        sessionDao.replaceSession(session, validated.map { it.copy(sessionId = id) })
        val durationRows = exercises.mapIndexedNotNull { order, draft ->
            val duration = draft.durationMillis?.takeIf { it > 0L } ?: return@mapIndexedNotNull null
            ExerciseDurationEntity(
                id = "$id-$order",
                sessionId = id,
                exerciseId = draft.exercise.id,
                exerciseOrder = order,
                durationMillis = duration,
                createdAt = System.currentTimeMillis(),
            )
        }
        exerciseDurationDao?.replaceForSession(id, durationRows)
        refreshPersonalRecords(
            (previousExerciseIds + exercises.map { it.exercise.id }).distinct(),
        )
        return Result.success(id)
    }

    suspend fun deleteSession(session: WorkoutSessionEntity) {
        val exerciseIds = loadSession(session.id)
            ?.exercises
            ?.map { it.exercise.id }
            .orEmpty()
        sessionDao.deleteSession(session)
        refreshPersonalRecords(exerciseIds)
    }

    suspend fun rebuildAllPersonalRecords() {
        val rows = sessionDao.getAllSetsForPr()
        val byExercise = rows.groupBy { it.exerciseId }
        exerciseDao.allIds().forEach { exerciseId ->
            writePersonalRecords(exerciseId, byExercise[exerciseId].orEmpty())
        }
    }

    private suspend fun refreshPersonalRecords(exerciseIds: List<String>) {
        val ids = exerciseIds.distinct()
        if (ids.isEmpty()) return
        val rows = sessionDao.getSetsForPr(ids).groupBy { it.exerciseId }
        ids.forEach { exerciseId ->
            writePersonalRecords(exerciseId, rows[exerciseId].orEmpty())
        }
    }

    private suspend fun writePersonalRecords(exerciseId: String, rows: List<SetForPrRow>) {
        val measurements = bodyRecordDao.listMeasurements()
        val computed = PersonalRecordLogic.fromSessions(
            rows.mapNotNull { row ->
                val reps = row.reps ?: return@mapNotNull null
                val bodyWeight = WeightLogic.bodyWeightKg(measurements, row.dateEpochDay)
                PerformedSet(
                    sessionId = row.sessionId,
                    dateEpochDay = row.dateEpochDay,
                    weightKg = WeightLogic.effectiveKg(
                        WeightMode.fromStorage(row.weightMode),
                        row.weightKg,
                        bodyWeight,
                    ),
                    reps = reps,
                    weightMode = row.weightMode,
                )
            },
        )
        exerciseDao.updatePersonalRecords(
            id = exerciseId,
            workingPrWeightKg = computed.working?.weightKg,
            workingPrReps = computed.working?.reps,
            workingPrWeightMode = computed.working?.weightMode,
            workingPrDateEpochDay = computed.working?.dateEpochDay,
            maxPrWeightKg = computed.max?.weightKg,
            maxPrReps = computed.max?.reps,
            maxPrWeightMode = computed.max?.weightMode,
            maxPrDateEpochDay = computed.max?.dateEpochDay,
        )
    }

    private suspend fun List<SessionWithSets>.toDetails(
        durationsBySession: Map<String, List<ExerciseDurationEntity>> = emptyMap(),
    ): List<SessionDetail> {
        val ids = flatMap { row -> row.sets.map { it.exerciseId } }.distinct()
        val exercises = if (ids.isEmpty()) {
            emptyMap()
        } else {
            sessionDao.getExercisesByIds(ids).associateBy { it.id }
        }
        return map { it.toDetail(exercises, durationsBySession[it.session.id].orEmpty()) }
    }

    private fun SessionWithSets.toDetail(
        exercises: Map<String, ExerciseEntity>,
        durations: List<ExerciseDurationEntity>,
    ): SessionDetail {
        val grouped = sets.groupBy { it.exerciseId }
        val order = sets.sortedWith(compareBy({ it.exerciseOrder }, { it.setIndex }))
            .map { it.exerciseId }
            .distinct()
        return SessionDetail(
            session = session,
            exercises = order.mapIndexed { index, exerciseId ->
                val entity = exercises[exerciseId] ?: ExerciseEntity(
                    id = exerciseId,
                    name = "未知动作",
                    bodyPart = BodyPart.OTHER.name,
                    equipmentType = EquipmentType.OTHER.name,
                    weightMode = WeightMode.WEIGHTED.name,
                    isCustom = true,
                    aliases = null,
                    createdAt = 0L,
                )
                SessionExercise(
                    exercise = entity,
                    sets = grouped[exerciseId].orEmpty().sortedBy { it.setIndex },
                    durationMillis = durations
                        .firstOrNull { it.exerciseOrder == index }
                        ?.durationMillis,
                )
            }.sortedGroupingBodyPart {
                ExerciseClassification.exerciseGroupingKey(it.exercise)
            },
        )
    }

    companion object {
        internal fun groupDateParts(rows: List<DatePartRow>): Map<LocalDate, List<String>> {
            return rows
                .groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
                .toSortedMap()
                .mapValues { (_, dayRows) ->
                    dayRows
                        .sortedWith(
                            compareByDescending<DatePartRow> { it.volume }
                                .thenBy { BodyPart.rank(it.part) },
                        )
                        .map { it.part }
                        .distinct()
                }
        }
    }
}
