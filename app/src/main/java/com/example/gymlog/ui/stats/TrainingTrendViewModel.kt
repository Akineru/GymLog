package com.example.gymlog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.PerformedSet
import com.example.gymlog.data.TrendLogic
import com.example.gymlog.data.TrendPoint
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.displayName

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class TrainingTrendMetric {
    VOLUME,
    WORKING_PR,
    MAX_PR,
}

data class TrainingTrendUiState(
    val metric: TrainingTrendMetric = TrainingTrendMetric.VOLUME,
    val bodyPart: ExerciseClassification.BodyCategory = ExerciseClassification.CHEST,
    val equipmentType: ExerciseClassification.EquipmentCategory =
        ExerciseClassification.ALL_EQUIPMENT,
    val exercises: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: String? = null,
    val points: List<TrendPoint> = emptyList(),
)

private data class TrendSelection(
    val metric: TrainingTrendMetric = TrainingTrendMetric.VOLUME,
    val exerciseId: String? = null,
    val bodyPart: ExerciseClassification.BodyCategory? = null,
    val equipment: ExerciseClassification.EquipmentCategory? = null,
)

class TrainingTrendViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    bodyRecordRepository: BodyRecordRepository,
) : ViewModel() {
    private val selection = MutableStateFlow(TrendSelection())

    val uiState: StateFlow<TrainingTrendUiState> = combine(
        selection,
        workoutRepository.observeAllSetsForPr(),
        exerciseRepository.observeAll(),
        bodyRecordRepository.observeMeasurements(),
    ) { selected, setRows, exercises, measurements ->
        val trainedIds = setRows.map { it.exerciseId }.toSet()
        val trained = exercises.filter { it.id in trainedIds }
        val anchor = trained.firstOrNull()
        val bodyPart = selected.bodyPart
            ?: anchor?.let(ExerciseClassification::bodyCategoryForExercise)
                ?.takeIf { it.standard }
            ?: ExerciseClassification.ALL_BODY
        val equipment = selected.equipment
            ?: anchor?.let(ExerciseClassification::equipmentCategoryForExercise)
                ?.takeIf { it.standard }
            ?: ExerciseClassification.ALL_EQUIPMENT
        val visible = trained
            .filter {
                (bodyPart.id.isBlank() || it.bodyCategoryId == bodyPart.id) &&
                    (equipment.id.isBlank() || it.equipmentCategoryId == equipment.id)
            }
            .sortedBy { it.displayName() }
        val chosenId = selected.exerciseId?.takeIf { id -> visible.any { it.id == id } }
            ?: visible.firstOrNull()?.id
        val sets = setRows.filter { it.exerciseId == chosenId }.mapNotNull { row ->
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
            TrainingTrendMetric.WORKING_PR -> TrendLogic.runningPrSeries(sets, maxEffort = false)
            TrainingTrendMetric.MAX_PR -> TrendLogic.runningPrSeries(sets, maxEffort = true)
        }
        TrainingTrendUiState(
            metric = selected.metric,
            bodyPart = bodyPart,
            equipmentType = equipment,
            exercises = visible,
            selectedExerciseId = chosenId,
            points = points,
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

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            exerciseRepository: ExerciseRepository,
            bodyRecordRepository: BodyRecordRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrainingTrendViewModel(
                        workoutRepository,
                        exerciseRepository,
                        bodyRecordRepository,
                    ) as T
                }
            }
        }
    }
}

fun ExerciseEntity.trendLabel(): String = displayName()
