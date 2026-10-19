package com.example.gymlog.ui.muscle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.Muscle
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleMapLogic
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate

enum class MuscleMapRange {
    DAY,
    WEEK,
    MONTH,
}

data class MuscleMapUiState(
    val date: LocalDate = LocalDate.now(),
    val range: MuscleMapRange = MuscleMapRange.DAY,
    val highlights: List<MuscleHighlight> = emptyList(),
    val hasSessions: Boolean = false,
    val unsupported: List<Muscle> = emptyList(),
)

class MuscleMapViewModel(
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    initialDateEpochDay: Long,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.ofEpochDay(initialDateEpochDay))
    private val range = MutableStateFlow(MuscleMapRange.DAY)

    val state: StateFlow<MuscleMapUiState> = combine(
        workoutRepository.observeHistory(),
        exerciseRepository.observeMuscleLinks(),
        date,
        range,
    ) { sessions, links, day, span ->
        val picked = sessions.filter { dayInRange(it.session.dateEpochDay, day, span) }
        val highlights = MuscleMapLogic.highlightsFor(picked, links)
        val unsupported = highlights.mapNotNull { highlight ->
            MuscleCatalog.find(highlight.muscleId)?.takeIf { !it.visualSupported }
        }
        MuscleMapUiState(
            date = day,
            range = span,
            highlights = highlights,
            hasSessions = picked.isNotEmpty(),
            unsupported = unsupported,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MuscleMapUiState(date = LocalDate.ofEpochDay(initialDateEpochDay)),
    )

    fun shiftDate(days: Long) {
        date.value = date.value.plusDays(days)
    }

    fun setDate(value: LocalDate) {
        date.value = value
    }

    fun setRange(value: MuscleMapRange) {
        range.value = value
    }

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            exerciseRepository: ExerciseRepository,
            initialDateEpochDay: Long,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MuscleMapViewModel(
                        workoutRepository,
                        exerciseRepository,
                        initialDateEpochDay,
                    ) as T
                }
            }
        }
    }
}

internal fun dayInRange(epochDay: Long, anchor: LocalDate, range: MuscleMapRange): Boolean {
    val start = when (range) {
        MuscleMapRange.DAY -> anchor
        MuscleMapRange.WEEK -> anchor.with(DayOfWeek.MONDAY)
        MuscleMapRange.MONTH -> anchor.withDayOfMonth(1)
    }
    val end = when (range) {
        MuscleMapRange.DAY -> anchor
        MuscleMapRange.WEEK -> start.plusDays(6)
        MuscleMapRange.MONTH -> anchor.withDayOfMonth(anchor.lengthOfMonth())
    }
    return epochDay in start.toEpochDay()..end.toEpochDay()
}
