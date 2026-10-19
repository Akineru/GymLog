package com.example.gymlog.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.sortedGroupingBodyPart
import com.example.gymlog.data.SessionExerciseDraft
import com.example.gymlog.data.SessionSetDraft
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class DraftSetUi(
    val id: String,
    val mode: WeightMode,
    val normalKgText: String,
    val extraKgText: String,
    val repsText: String,
)

data class DraftExerciseUi(
    val id: String,
    val exercise: ExerciseEntity,
    val sets: List<DraftSetUi>,
)

data class SessionEditorUiState(
    val sessionId: String?,
    val dateEpochDay: Long,
    val note: String,
    val exercises: List<DraftExerciseUi>,
    val bodyWeightKg: Double = 0.0,
    val errorMessage: String? = null,
    val loading: Boolean = false,
)

class SessionEditorViewModel(
    private val repository: WorkoutRepository,
    bodyRecordRepository: BodyRecordRepository,
    private val existingSessionId: String?,
    initialDateEpochDay: Long? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SessionEditorUiState(
            sessionId = existingSessionId,
            dateEpochDay = initialDateEpochDay ?: LocalDate.now().toEpochDay(),
            note = "",
            exercises = emptyList(),
            loading = existingSessionId != null,
        ),
    )
    private val measurements = MutableStateFlow<List<BodyMeasurementEntity>>(emptyList())
    val state: StateFlow<SessionEditorUiState> = combine(_state, measurements) { editor, rows ->
        editor.copy(bodyWeightKg = WeightLogic.bodyWeightKg(rows, editor.dateEpochDay))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _state.value,
    )
    private val _exitAfterSave = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exitAfterSave: SharedFlow<Unit> = _exitAfterSave

    init {
        viewModelScope.launch {
            bodyRecordRepository.observeMeasurements().collect { measurements.value = it }
        }
        if (existingSessionId != null) {
            viewModelScope.launch {
                val detail = repository.loadSession(existingSessionId)
                if (detail == null) {
                    _state.update {
                        it.copy(loading = false, errorMessage = "找不到这条训练记录")
                    }
                } else {
                    _state.update {
                        it.copy(
                            sessionId = detail.session.id,
                            dateEpochDay = detail.session.dateEpochDay,
                            note = detail.session.note.orEmpty(),
                            exercises = detail.exercises.map { item ->
                                DraftExerciseUi(
                                    id = item.exercise.id + "_" + UUID.randomUUID(),
                                    exercise = item.exercise,
                                    sets = item.sets.map { set -> set.toDraft() },
                                )
                            },
                            loading = false,
                        )
                    }
                }
            }
        }
    }

    fun delete() {
        val id = existingSessionId ?: return
        viewModelScope.launch {
            repository.deleteSession(
                WorkoutSessionEntity(
                    id = id,
                    dateEpochDay = _state.value.dateEpochDay,
                    note = null,
                    createdAt = 0L,
                ),
            )
            _exitAfterSave.tryEmit(Unit)
        }
    }

    fun onDateChange(epochDay: Long) {
        _state.update { it.copy(dateEpochDay = epochDay, errorMessage = null) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun addExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val fromHistory = repository.latestSetForExercise(exercise.id)?.toDraft()
            _state.update { current ->
                val fromDraft = current.exercises
                    .lastOrNull { it.exercise.id == exercise.id }
                    ?.sets
                    ?.lastOrNull()
                val previous = fromDraft ?: fromHistory
                val mode = previous?.mode ?: WeightLogic.defaultModeForExercise(exercise)
                current.copy(
                    exercises = (current.exercises + DraftExerciseUi(
                        id = UUID.randomUUID().toString(),
                        exercise = exercise,
                        sets = listOf(newSet(mode, previous)),
                    )).sortedGroupingBodyPart {
                        ExerciseClassification.exerciseGroupingKey(it.exercise)
                    },
                    errorMessage = null,
                )
            }
        }
    }

    fun removeExercise(draftId: String) {
        _state.update { current ->
            current.copy(exercises = current.exercises.filterNot { it.id == draftId })
        }
    }

    fun addSet(exerciseDraftId: String) {
        _state.update { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.id != exerciseDraftId) {
                        item
                    } else {
                        val previous = item.sets.lastOrNull()
                        item.copy(sets = item.sets + newSet(previous?.mode ?: WeightMode.WEIGHTED, previous))
                    }
                },
            )
        }
    }

    fun removeSet(exerciseDraftId: String, setId: String) {
        _state.update { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.id != exerciseDraftId) {
                        item
                    } else {
                        item.copy(sets = item.sets.filterNot { it.id == setId }.ifEmpty { item.sets })
                    }
                },
            )
        }
    }

    fun updateSet(exerciseDraftId: String, setId: String, transform: (DraftSetUi) -> DraftSetUi) {
        _state.update { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.id != exerciseDraftId) {
                        item
                    } else {
                        item.copy(sets = item.sets.map { set -> if (set.id == setId) transform(set) else set })
                    }
                },
                errorMessage = null,
            )
        }
    }

    fun changeMode(exerciseDraftId: String, setId: String, mode: WeightMode) {
        updateSet(exerciseDraftId, setId) { set -> set.copy(mode = mode) }
    }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            val drafts = current.exercises.map { item ->
                SessionExerciseDraft(
                    exercise = item.exercise,
                    sets = item.sets.map { set ->
                        SessionSetDraft(
                            id = set.id,
                            mode = set.mode,
                            normalKgText = set.normalKgText,
                            extraKgText = set.extraKgText,
                            repsText = set.repsText,
                        )
                    },
                )
            }
            val result = repository.saveSession(
                sessionId = current.sessionId,
                dateEpochDay = current.dateEpochDay,
                note = current.note,
                exercises = drafts,
            )
            if (result.isSuccess) {
                _state.update {
                    it.copy(sessionId = result.getOrNull(), errorMessage = null)
                }
                _exitAfterSave.tryEmit(Unit)
            } else {
                _state.update {
                    it.copy(errorMessage = messageFor(result.exceptionOrNull()?.message))
                }
            }
        }
    }

    private fun newSet(mode: WeightMode, previous: DraftSetUi?): DraftSetUi {
        return DraftSetUi(
            id = UUID.randomUUID().toString(),
            mode = mode,
            normalKgText = previous?.normalKgText.orEmpty(),
            extraKgText = previous?.extraKgText.orEmpty(),
            repsText = previous?.repsText.orEmpty(),
        )
    }

    private fun WorkoutSetEntity.toDraft(): DraftSetUi {
        val mode = WeightMode.fromStorage(weightMode)
        val kgText = weightKg?.let(WeightLogic::formatKg).orEmpty()
        return DraftSetUi(
            id = id,
            mode = mode,
            normalKgText = if (mode == WeightMode.WEIGHTED) kgText else "",
            extraKgText = if (mode == WeightMode.BODYWEIGHT_PLUS) kgText else "",
            repsText = reps?.toString().orEmpty(),
        )
    }

    private fun messageFor(code: String?): String {
        return when (code) {
            "NO_EXERCISES" -> "请至少添加一个训练动作"
            "NO_SETS" -> "每个动作至少需要一组数据"
            "WEIGHT_REQUIRED" -> "请填写普通重量，可输入小数"
            "EXTRA_WEIGHT_REQUIRED" -> "请填写负重，可为负数"
            "REPS_REQUIRED" -> "请填写次数"
            "BODYWEIGHT_HAS_KG" -> "自重组不能保存重量数字"
            else -> "保存失败，请检查各组数据"
        }
    }

    companion object {
        fun factory(
            repository: WorkoutRepository,
            bodyRecordRepository: BodyRecordRepository,
            sessionId: String?,
            initialDateEpochDay: Long? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionEditorViewModel(
                        repository,
                        bodyRecordRepository,
                        sessionId,
                        initialDateEpochDay,
                    ) as T
                }
            }
        }
    }
}
