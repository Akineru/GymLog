package com.example.gymlog.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseGuideBuilder
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
import kotlinx.coroutines.flow.map
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

    /**
     * 「要点」页用：数据集自带的中文步骤，用来生成「动作怎么做」。
     *
     * 单独一条流，不进 [uiState]：uiState 的 combine 已经有 5 个来源，
     * 再加一个反而更难读，而步骤只被要点页用到。
     */
    val instructionSteps: StateFlow<List<String>> = repository.observeInstructionSteps(exerciseId)
        .map { ExerciseGuideBuilder.parseSteps(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
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
