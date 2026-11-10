package com.example.gymlog.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.MuscleHeatLogic
import com.example.gymlog.data.MuscleHeatRange
import com.example.gymlog.data.MuscleHeatStat
import com.example.gymlog.data.MuscleRecoveryInfo
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
    /** 仅恢复模式下有值：肌群 id → 恢复状态。 */
    val recovery: Map<String, MuscleRecoveryInfo> = emptyMap(),
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
        val touches = MuscleHeatLogic.touches(sessions, links)
        val recoveryMode = selected == MuscleHeatRange.RECOVERY
        MuscleHeatUiState(
            range = selected,
            start = period.start,
            end = period.endInclusive,
            // 恢复模式下 stats 只取每块肌肉「最近一次训练那天」，让底部详情的动作列表
            // 和「上次训练」日期对得上；着色走 recovery 而不是 stats。
            stats = if (recoveryMode) {
                MuscleHeatLogic.latestSessionStats(touches)
            } else {
                MuscleHeatLogic.aggregate(
                    touches = touches,
                    startEpochDay = period.start.toEpochDay(),
                    endEpochDay = period.endInclusive.toEpochDay(),
                )
            },
            recovery = if (recoveryMode) MuscleHeatLogic.recovery(touches, today) else emptyMap(),
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
