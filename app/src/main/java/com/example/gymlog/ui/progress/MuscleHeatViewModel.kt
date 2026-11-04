package com.example.gymlog.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.MuscleHeatLogic
import com.example.gymlog.data.MuscleHeatRange
import com.example.gymlog.data.MuscleHeatStat
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class MuscleHeatUiState(
    val range: MuscleHeatRange = MuscleHeatRange.WEEK,
    val start: LocalDate = LocalDate.now().minusDays(6),
    val end: LocalDate = LocalDate.now(),
    val stats: Map<String, MuscleHeatStat> = emptyMap(),
)

class MuscleHeatViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val range = MutableStateFlow(MuscleHeatRange.WEEK)

    val state: StateFlow<MuscleHeatUiState> = combine(
        workoutRepository.observeHistory(),
        exerciseRepository.observeMuscleLinks(),
        range,
    ) { sessions, links, selected ->
        val today = LocalDate.now()
        val period = MuscleHeatLogic.period(today, selected)
        val stats = MuscleHeatLogic.aggregate(
            touches = MuscleHeatLogic.touches(sessions, links),
            startEpochDay = period.start.toEpochDay(),
            endEpochDay = period.endInclusive.toEpochDay(),
        )
        MuscleHeatUiState(
            range = selected,
            start = period.start,
            end = period.endInclusive,
            stats = stats,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MuscleHeatUiState(),
    )

    fun setRange(value: MuscleHeatRange) {
        range.value = value
    }

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            exerciseRepository: ExerciseRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MuscleHeatViewModel(workoutRepository, exerciseRepository) as T
                }
            }
        }
    }
}
