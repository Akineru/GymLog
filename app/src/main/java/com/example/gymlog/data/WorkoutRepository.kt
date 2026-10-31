package com.example.gymlog.data

import androidx.room.withTransaction
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
    /**
     * 做组 PR 的最低次数，默认取 [PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS]。
     *
     * 传的是取值函数而不是 Int：保存 PR 时（训练保存、删除、全量重建）都要读**当下**的设置，
     * 用户改完门槛马上点「刷新成绩」也能立刻生效，不会用到构造时的旧值。
     */
    private val workingPrMinRepsProvider: () -> Int = {
        PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS
    },
    /** 仅用于把批量 PR 写入放进一个事务；单测里可以传 null。 */
    private val database: GymLogDatabase? = null,
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

    suspend fun findExercise(id: String): ExerciseEntity? = exerciseDao.getById(id)

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

    /**
     * 按当前规则重算**所有**动作的 PR —— 「刷新成绩」按钮和升级后的自愈都走这里。
     *
     * 分两步：先只读地把结果全算出来，再统一写回。
     * - 计算阶段抛异常 ⇒ 一条都不写，旧 PR 原样保留；
     * - 写入阶段有 [database] 时包在一个事务里 ⇒ 不会出现「一半动作是新规则、一半是旧规则」。
     *
     * 只写真正发生变化的动作，重复点击不会产生多余写入。
     *
     * @return 实际发生变化的动作数量（用于「刷新成绩」的结果提示）。
     */
    suspend fun rebuildAllPersonalRecords(): Int {
        val byExercise = sessionDao.getAllSetsForPr().groupBy { it.exerciseId }
        val measurements = bodyRecordDao.listMeasurements()
        val threshold = workingPrMinRepsProvider()
        val pending = exerciseDao.allExercises().mapNotNull { exercise ->
            val computed = computeRecords(byExercise[exercise.id].orEmpty(), measurements, threshold)
            if (computed.matches(exercise)) null else PrWrite(exercise.id, computed)
        }
        if (pending.isEmpty()) return 0
        val write: suspend () -> Unit = {
            pending.forEach { item -> item.writeTo(exerciseDao) }
        }
        val db = database
        if (db != null) db.withTransaction { write() } else write()
        return pending.size
    }

    private suspend fun refreshPersonalRecords(exerciseIds: List<String>) {
        val ids = exerciseIds.distinct()
        if (ids.isEmpty()) return
        val byExercise = sessionDao.getSetsForPr(ids).groupBy { it.exerciseId }
        val measurements = bodyRecordDao.listMeasurements()
        val threshold = workingPrMinRepsProvider()
        ids.forEach { exerciseId ->
            PrWrite(
                exerciseId,
                computeRecords(byExercise[exerciseId].orEmpty(), measurements, threshold),
            ).writeTo(exerciseDao)
        }
    }

    private fun computeRecords(
        rows: List<SetForPrRow>,
        measurements: List<BodyMeasurementEntity>,
        minWorkingReps: Int,
    ): ExercisePersonalRecords {
        return PersonalRecordLogic.fromSessions(
            sets = rows.mapNotNull { row ->
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
            minWorkingReps = minWorkingReps,
        )
    }

    /** 一次 PR 写入的最小单元，避免把参数顺序写错。 */
    private data class PrWrite(
        val exerciseId: String,
        val records: ExercisePersonalRecords,
    ) {
        suspend fun writeTo(dao: ExerciseDao) {
            dao.updatePersonalRecords(
                id = exerciseId,
                workingPrWeightKg = records.working?.weightKg,
                workingPrReps = records.working?.reps,
                workingPrWeightMode = records.working?.weightMode,
                workingPrDateEpochDay = records.working?.dateEpochDay,
                maxPrWeightKg = records.max?.weightKg,
                maxPrReps = records.max?.reps,
                maxPrWeightMode = records.max?.weightMode,
                maxPrDateEpochDay = records.max?.dateEpochDay,
            )
        }
    }

    private fun ExercisePersonalRecords.matches(exercise: ExerciseEntity): Boolean {
        return working.sameAs(
            exercise.workingPrWeightKg,
            exercise.workingPrReps,
            exercise.workingPrWeightMode,
            exercise.workingPrDateEpochDay,
        ) && max.sameAs(
            exercise.maxPrWeightKg,
            exercise.maxPrReps,
            exercise.maxPrWeightMode,
            exercise.maxPrDateEpochDay,
        )
    }

    private fun PersonalRecord?.sameAs(
        weightKg: Double?,
        reps: Int?,
        weightMode: String?,
        dateEpochDay: Long?,
    ): Boolean {
        if (this == null) return weightKg == null && reps == null && dateEpochDay == null
        return this.weightKg == weightKg &&
            this.reps == reps &&
            this.weightMode == weightMode &&
            this.dateEpochDay == dateEpochDay
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
