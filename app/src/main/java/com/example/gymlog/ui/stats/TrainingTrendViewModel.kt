package com.example.gymlog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.PerformedSet
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.TrendLogic
import com.example.gymlog.data.TrendPoint
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.roundToInt

enum class TrainingTrendMetric {
    VOLUME,
    WORKING_PR,
    MAX_PR,
}

/** 选中动作的历史汇总，显示在图卡下方的三格统计里。 */
data class TrendStats(
    val sessionCount: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val bestWeightKg: Double = 0.0,
    val bestReps: Int = 0,
)

/** 某天的一次训练（只统计当前选中动作）：显示在「训练记录」列表里。 */
data class TrendSessionRow(
    val sessionId: String,
    val dateEpochDay: Long,
    val setCount: Int,
    val totalReps: Int,
    val bestWeightKg: Double,
    val bestReps: Int,
    val volumeKg: Double,
)

/** 「练习频率」里的一周，[days] 是这周练过这个动作的天数。 */
data class TrainingWeek(val startEpochDay: Long, val days: Int)

data class TrainingTrendUiState(
    val metric: TrainingTrendMetric = TrainingTrendMetric.VOLUME,
    val bodyPart: ExerciseClassification.BodyCategory = ExerciseClassification.ALL_BODY,
    val equipmentType: ExerciseClassification.EquipmentCategory =
        ExerciseClassification.ALL_EQUIPMENT,
    val query: String = "",
    /**
     * 选择面板里的候选：已按部位 / 器械 / 搜索词过滤。
     * 搜索为空时只有练过的（最近若干条）；有搜索词时也会带出没练过的，由 [trainedExerciseIds] 区分。
     */
    val exercises: List<ExerciseEntity> = emptyList(),
    /** 有训练记录的动作 id，列表里用来给没练过的打「未练过」标记。 */
    val trainedExerciseIds: Set<String> = emptySet(),
    val selectedExercise: ExerciseEntity? = null,
    val points: List<TrendPoint> = emptyList(),
    val stats: TrendStats = TrendStats(),
    /** 每次训练一行，按日期倒序。 */
    val history: List<TrendSessionRow> = emptyList(),
    /** 近 [WEEKS_SHOWN] 周的练习频率，最早的在前、本周在最后。 */
    val weeks: List<TrainingWeek> = emptyList(),
    /** 窗口内训练间隔的平均天数；练过不到两次时为 null。 */
    val averageGapDays: Int? = null,
) {
    val selectedExerciseId: String? get() = selectedExercise?.id
}

private data class TrendSelection(
    val metric: TrainingTrendMetric = TrainingTrendMetric.VOLUME,
    val exerciseId: String? = null,
    val bodyPart: ExerciseClassification.BodyCategory = ExerciseClassification.ALL_BODY,
    val equipment: ExerciseClassification.EquipmentCategory =
        ExerciseClassification.ALL_EQUIPMENT,
    val query: String = "",
)

/** 不输入搜索词时，选择面板默认列出最近练过的条数。 */
private const val RECENT_EXERCISE_LIMIT = 20

/** 「练习频率」显示的周数。 */
internal const val WEEKS_SHOWN = 12

class TrainingTrendViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    bodyRecordRepository: BodyRecordRepository,
    /** 做组 PR 的当前最低次数，取自训练偏好；极限 PR 不使用它。 */
    private val workingPrMinRepsProvider: () -> Int = {
        PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS
    },
    /** 「练习频率」按哪一天算本周，测试时可替换。 */
    private val todayProvider: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val selection = MutableStateFlow(TrendSelection())

    val uiState: StateFlow<TrainingTrendUiState> = combine(
        selection,
        workoutRepository.observeAllSetsForPr(),
        exerciseRepository.observeAll(),
        bodyRecordRepository.observeMeasurements(),
    ) { selected, setRows, exercises, measurements ->
        val lastTrainedByExercise = setRows
            .groupBy { it.exerciseId }
            .mapValues { (_, rows) -> rows.maxOf { it.dateEpochDay } }
        val trainedIds = lastTrainedByExercise.keys
        val bodyPart = selected.bodyPart
        val equipment = selected.equipment
        val filteredForCategory = exercises.filter {
            (bodyPart.id.isBlank() || it.bodyCategoryId == bodyPart.id) &&
                (equipment.id.isBlank() || it.equipmentCategoryId == equipment.id)
        }
        val candidates = trendCandidates(filteredForCategory, lastTrainedByExercise, selected.query)
        val chosen = filteredForCategory.firstOrNull { it.id == selected.exerciseId }
            ?: trendCandidates(filteredForCategory, lastTrainedByExercise, "").firstOrNull()
        val sets = setRows.filter { it.exerciseId == chosen?.id }.mapNotNull { row ->
            val reps = row.reps ?: return@mapNotNull null
            PerformedSet(
                sessionId = row.sessionId,
                dateEpochDay = row.dateEpochDay,
                weightKg = WeightLogic.effectiveKg(
                    WeightMode.fromStorage(row.weightMode),
                    row.weightKg,
                    WeightLogic.bodyWeightKg(measurements, row.dateEpochDay),
                ),
                reps = reps,
                weightMode = row.weightMode,
            )
        }
        val points = when (selected.metric) {
            TrainingTrendMetric.VOLUME -> TrendLogic.exerciseVolumeSeries(sets)
            TrainingTrendMetric.WORKING_PR -> TrendLogic.runningPrSeries(
                sets = sets,
                maxEffort = false,
                minWorkingReps = workingPrMinRepsProvider(),
            )
            TrainingTrendMetric.MAX_PR -> TrendLogic.runningPrSeries(sets, maxEffort = true)
        }
        val best = sets.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
        val trainedDays = sets.map { it.dateEpochDay }.distinct().sorted()
        val weeks = weeklyActivity(trainedDays, todayProvider().toEpochDay())
        val windowDays = trainedDays.filter { it >= weeks.first().startEpochDay }
        TrainingTrendUiState(
            metric = selected.metric,
            bodyPart = bodyPart,
            equipmentType = equipment,
            query = selected.query,
            exercises = candidates,
            trainedExerciseIds = trainedIds,
            selectedExercise = chosen,
            points = points,
            stats = TrendStats(
                sessionCount = sets.map { it.sessionId }.distinct().size,
                totalVolumeKg = sets.sumOf { (it.weightKg ?: 0.0) * it.reps },
                bestWeightKg = best?.weightKg ?: 0.0,
                bestReps = best?.reps ?: 0,
            ),
            history = sessionRows(sets),
            weeks = weeks,
            averageGapDays = averageGapDays(windowDays),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrainingTrendUiState(),
    )

    fun onMetricSelected(value: TrainingTrendMetric) {
        selection.value = selection.value.copy(metric = value)
    }

    fun onBodyPartSelected(value: ExerciseClassification.BodyCategory) {
        selection.value = selection.value.copy(bodyPart = value)
    }

    fun onEquipmentSelected(value: ExerciseClassification.EquipmentCategory) {
        selection.value = selection.value.copy(equipment = value)
    }

    fun onExerciseSelected(id: String) {
        selection.value = selection.value.copy(exerciseId = id)
    }

    fun onQueryChange(value: String) {
        selection.value = selection.value.copy(query = value)
    }

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            exerciseRepository: ExerciseRepository,
            bodyRecordRepository: BodyRecordRepository,
            workingPrMinRepsProvider: () -> Int = {
                PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS
            },
            todayProvider: () -> LocalDate = LocalDate::now,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrainingTrendViewModel(
                        workoutRepository,
                        exerciseRepository,
                        bodyRecordRepository,
                        workingPrMinRepsProvider,
                        todayProvider,
                    ) as T
                }
            }
        }
    }
}

/**
 * 选择面板的候选列表（[filtered] 已按部位 / 器械过滤）：
 * 不输入时只给练过的、按最近练过的时间倒序、取 [limit] 条；
 * 输入后连没练过的也一起搜（靠「未练过」标记区分），练过的排在前面。
 */
internal fun trendCandidates(
    filtered: List<ExerciseEntity>,
    lastTrainedByExercise: Map<String, Long>,
    query: String,
    limit: Int = RECENT_EXERCISE_LIMIT,
): List<ExerciseEntity> {
    val needle = query.trim()
    if (needle.isEmpty()) {
        return filtered
            .filter { it.id in lastTrainedByExercise }
            .sortedByDescending { lastTrainedByExercise[it.id] ?: Long.MIN_VALUE }
            .take(limit)
    }
    return filtered
        .filter { it.matches(needle) }
        .sortedWith(
            compareByDescending<ExerciseEntity> { it.id in lastTrainedByExercise }
                .thenBy { it.name },
        )
}

/**
 * 把同一个动作的记录按训练场次聚合，每场一行、按日期倒序：
 * 组数、总次数、最好的一组、当次容量。
 */
internal fun sessionRows(sets: List<PerformedSet>): List<TrendSessionRow> {
    return sets.groupBy { it.sessionId }
        .map { (sessionId, rows) ->
            val best = rows.maxWithOrNull(compareBy({ it.weightKg ?: 0.0 }, { it.reps }))
            TrendSessionRow(
                sessionId = sessionId,
                dateEpochDay = rows.maxOf { it.dateEpochDay },
                setCount = rows.size,
                totalReps = rows.sumOf { it.reps },
                bestWeightKg = best?.weightKg ?: 0.0,
                bestReps = best?.reps ?: 0,
                volumeKg = rows.sumOf { (it.weightKg ?: 0.0) * it.reps },
            )
        }
        .sortedByDescending { it.dateEpochDay }
}

/** 近 [weeks] 周每周练了几天，最早的在前、包含今天的那周在最后。 */
internal fun weeklyActivity(
    trainedDays: Collection<Long>,
    todayEpochDay: Long,
    weeks: Int = WEEKS_SHOWN,
): List<TrainingWeek> {
    val days = trainedDays.toSet()
    val thisWeek = weekStart(todayEpochDay)
    return (weeks - 1 downTo 0).map { back ->
        val start = thisWeek - back * 7L
        TrainingWeek(startEpochDay = start, days = (0L until 7L).count { start + it in days })
    }
}

/** 1970-01-01 是周四，所以 (epochDay + 3) 除以 7 的余数就是「离本周一几天」。 */
private fun weekStart(epochDay: Long): Long = epochDay - Math.floorMod(epochDay + 3L, 7L)

/** 相邻两次训练的平均间隔（天）；不到两次返回 null。 */
internal fun averageGapDays(trainedDays: List<Long>): Int? {
    val sorted = trainedDays.distinct().sorted()
    if (sorted.size < 2) return null
    val span = sorted.last() - sorted.first()
    return (span.toDouble() / (sorted.size - 1)).roundToInt()
}

/**
 * 选择面板的搜索：原名（清洗后的动作本体）、旧的中文全名、英文名、别名词条都参与匹配，
 * 这样输「卧推」「bench」都能命中，不必记住这一条到底叫什么。
 */
internal fun ExerciseEntity.matches(query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return name.lowercase().contains(needle) ||
        originalName.lowercase().contains(needle) ||
        nameEn?.lowercase()?.contains(needle) == true ||
        aliases?.lowercase()?.contains(needle) == true
}
