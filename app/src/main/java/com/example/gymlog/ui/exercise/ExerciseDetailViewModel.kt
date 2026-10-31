package com.example.gymlog.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseHistoryEntry
import com.example.gymlog.data.ExerciseHistoryLogic
import com.example.gymlog.data.ExerciseMediaRepository
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleMapLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val media: ExerciseMediaView? = null,
    val muscleLinks: List<MuscleLink> = emptyList(),
    val history: List<ExerciseHistoryEntry> = emptyList(),
    /** 首帧数据是否已到齐（用于区分「加载中」与「动作不存在」）。 */
    val loaded: Boolean = false,
)

/**
 * 动作详情页的状态源。
 *
 * 三个 tab 共用同一个 ViewModel 与同一个 [exerciseId]，切换 tab 不会重新指定动作，
 * 因此不存在「切页后串到别的动作」的可能。
 */
class ExerciseDetailViewModel(
    private val exerciseId: String,
    private val repository: ExerciseRepository,
    private val mediaRepository: ExerciseMediaRepository,
    private val bodyRecordRepository: BodyRecordRepository,
) : ViewModel() {

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        repository.observeById(exerciseId),
        mediaRepository.observeMedia(),
        repository.observeMuscleLinks(),
        repository.observeHistory(exerciseId),
        bodyRecordRepository.observeMeasurements(),
    ) { exercise, media, links, historyRows, measurements ->
        ExerciseDetailUiState(
            exercise = exercise,
            media = media[exerciseId],
            muscleLinks = if (exercise == null) {
                emptyList()
            } else {
                MuscleMapLogic.linksFor(exercise.id, exercise.isCustom, links)
            },
            history = ExerciseHistoryLogic.entries(historyRows, measurements),
            loaded = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExerciseDetailUiState(),
    )

    fun saveTrainingFields(
        aliases: String?,
        howTo: String?,
        cues: String?,
    ) {
        viewModelScope.launch {
            repository.updateTrainingFields(
                exerciseId = exerciseId,
                aliases = aliases,
                howTo = howTo,
                cues = cues,
            )
        }
    }

    fun setFavorite(favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(exerciseId, favorite) }
    }

    fun replaceCustomMuscles(links: List<MuscleLink>) {
        viewModelScope.launch { repository.replaceCustomMuscles(exerciseId, links) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseId)
            onDeleted()
        }
    }

    companion object {
        fun factory(
            exerciseId: String,
            repository: ExerciseRepository,
            mediaRepository: ExerciseMediaRepository,
            bodyRecordRepository: BodyRecordRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExerciseDetailViewModel(
                        exerciseId = exerciseId,
                        repository = repository,
                        mediaRepository = mediaRepository,
                        bodyRecordRepository = bodyRecordRepository,
                    ) as T
                }
            }
        }
    }
}
