package com.example.gymlog.ui.session

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.PhotoWallRepository
import com.example.gymlog.data.SessionExerciseDraft
import com.example.gymlog.data.SessionSetDraft
import com.example.gymlog.data.TimerRecordRepository
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutClockLogic
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import com.example.gymlog.data.sortedGroupingBodyPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

data class DraftSetUi(
    val id: String,
    val mode: WeightMode,
    val normalKgText: String,
    val extraKgText: String,
    val repsText: String,
    val completed: Boolean = false,
)

data class DraftExerciseUi(
    val id: String,
    val exercise: ExerciseEntity,
    val sets: List<DraftSetUi>,
    val startedAtMillis: Long? = null,
    val completedAtMillis: Long? = null,
    val durationMillis: Long? = null,
)

data class SessionEditorUiState(
    val sessionId: String?,
    val dateEpochDay: Long,
    val note: String,
    val exercises: List<DraftExerciseUi>,
    val bodyWeightKg: Double = 0.0,
    val errorMessage: String? = null,
    val loading: Boolean = false,
    val totalElapsedMillis: Long = 0L,
    val finishing: Boolean = false,
    val finishedDurationMillis: Long = 0L,
    val saving: Boolean = false,
    val photoId: String? = null,
    val photoBitmap: Bitmap? = null,
    val loadingPhoto: Boolean = false,
    val collapsedExerciseIds: Set<String> = emptySet(),
)

enum class WorkoutClockMode {
    TIMER,
    STOPWATCH,
}

data class WorkoutClockUi(
    val mode: WorkoutClockMode = WorkoutClockMode.TIMER,
    val running: Boolean = false,
    val displayMillis: Long = WorkoutClockLogic.DEFAULT_TIMER_MILLIS,
    val timerDurationMillis: Long = WorkoutClockLogic.DEFAULT_TIMER_MILLIS,
)

class SessionEditorViewModel(
    private val repository: WorkoutRepository,
    private val bodyRecordRepository: BodyRecordRepository,
    private val existingSessionId: String?,
    initialDateEpochDay: Long? = null,
    private val photoWallRepository: PhotoWallRepository? = null,
    private val timerRecordRepository: TimerRecordRepository? = null,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
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
    private val totalTick = MutableStateFlow(0)
    private val collapsedExerciseIds = MutableStateFlow<Set<String>>(emptySet())
    private val _clock = MutableStateFlow(WorkoutClockUi())
    val clock: StateFlow<WorkoutClockUi> = _clock
    private val _clockFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clockFinished: SharedFlow<Unit> = _clockFinished.asSharedFlow()

    private var totalStartRealtime: Long? =
        if (existingSessionId == null) elapsedRealtime() else null
    private var totalAccumulatedMillis = 0L
    private var clockStartedAt: Long? = null
    private var clockBaseRemainingMillis = WorkoutClockLogic.DEFAULT_TIMER_MILLIS
    private var stopwatchAccumulatedMillis = 0L

    val state: StateFlow<SessionEditorUiState> = combine(
        _state,
        measurements,
        totalTick,
        collapsedExerciseIds,
    ) { editor, rows, _, collapsed ->
        editor.copy(
            bodyWeightKg = WeightLogic.bodyWeightKg(rows, editor.dateEpochDay),
            totalElapsedMillis = if (editor.finishing) {
                editor.finishedDurationMillis
            } else {
                totalElapsed()
            },
            collapsedExerciseIds = collapsed,
        )
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
                    totalAccumulatedMillis = detail.session.durationMillis
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
                                    durationMillis = item.durationMillis,
                                )
                            },
                            loading = false,
                            finishedDurationMillis = detail.session.durationMillis,
                        )
                    }
                    totalTick.value++
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(100)
                if (totalStartRealtime != null) totalTick.value++
                tickClock()
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
            timerRecordRepository?.deleteSessionDuration(id)
            _exitAfterSave.tryEmit(Unit)
        }
    }

    fun discardTraining() {
        val current = _state.value
        current.photoId?.let { photoWallRepository?.discardStaged(it) }
        totalStartRealtime = null
        clockStartedAt = null
        if (current.sessionId == null) {
            _exitAfterSave.tryEmit(Unit)
        } else {
            delete()
        }
    }

    fun onDateChange(epochDay: Long) {
        _state.update { it.copy(dateEpochDay = epochDay, errorMessage = null) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun finish() {
        if (_state.value.finishing) return
        val elapsed = totalElapsed()
        totalAccumulatedMillis = elapsed
        totalStartRealtime = null
        totalTick.value++
        _state.update {
            it.copy(
                finishing = true,
                finishedDurationMillis = elapsed,
                errorMessage = null,
            )
        }
    }

    fun cancelFinish() {
        _state.update { it.copy(finishing = false) }
        if (existingSessionId == null && totalStartRealtime == null) {
            totalStartRealtime = elapsedRealtime()
        }
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
                        startedAtMillis = elapsedRealtime(),
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
                        item.copy(sets = item.sets.filterNot { it.id == setId })
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

    fun toggleSetCompleted(exerciseDraftId: String, setId: String) {
        updateSet(exerciseDraftId, setId) { it.copy(completed = !it.completed) }
    }

    fun toggleExerciseCollapsed(exerciseDraftId: String) {
        collapsedExerciseIds.update { current ->
            if (exerciseDraftId in current) {
                current - exerciseDraftId
            } else {
                current + exerciseDraftId
            }
        }
    }

    fun finishExercise(exerciseDraftId: String) {
        val now = elapsedRealtime()
        _state.update { current ->
            current.copy(
                exercises = current.exercises.map { item ->
                    if (item.id != exerciseDraftId) {
                        item
                    } else {
                        val started = item.startedAtMillis
                        item.copy(
                            completedAtMillis = now,
                            durationMillis = started?.let { (now - it).coerceAtLeast(0L) }
                                ?: item.durationMillis,
                        )
                    }
                },
            )
        }
        collapsedExerciseIds.update { it + exerciseDraftId }
    }

    fun changeMode(exerciseDraftId: String, setId: String, mode: WeightMode) {
        updateSet(exerciseDraftId, setId) { set -> set.copy(mode = mode) }
    }

    fun stagePhoto(uri: Uri) {
        val photos = photoWallRepository ?: return
        val previousPhotoId = _state.value.photoId
        _state.update { it.copy(loadingPhoto = true, errorMessage = null) }
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { photos.stageImport(uri) }
            val bitmap = id?.let { withContext(Dispatchers.IO) { photos.decode(it, preferEdited = false) } }
            if (id == null || bitmap == null) {
                _state.update { it.copy(loadingPhoto = false, errorMessage = "没有读到照片") }
                return@launch
            }
            if (previousPhotoId != null && previousPhotoId != id) {
                photos.discardStaged(previousPhotoId)
            }
            _state.update { it.copy(photoId = id, photoBitmap = bitmap, loadingPhoto = false) }
        }
    }

    fun removePhoto() {
        val current = _state.value
        current.photoId?.let { photoWallRepository?.discardStaged(it) }
        _state.update { it.copy(photoId = null, photoBitmap = null) }
    }

    fun saveBodyWeight(weightKg: Double) {
        if (weightKg <= 0.0) return
        viewModelScope.launch {
            bodyRecordRepository.saveWeight(weightKg)
            repository.rebuildAllPersonalRecords()
        }
    }

    fun saveFinal() {
        val current = _state.value
        if (current.saving) return
        val drafts = current.exercises.mapNotNull { item ->
            val sets = item.sets.filter { it.completed }
            if (sets.isEmpty()) {
                null
            } else {
                SessionExerciseDraft(
                    exercise = item.exercise,
                    sets = sets.map { set ->
                        SessionSetDraft(
                            id = set.id,
                            mode = set.mode,
                            normalKgText = set.normalKgText,
                            extraKgText = set.extraKgText,
                            repsText = set.repsText,
                        )
                    },
                    durationMillis = item.durationMillis,
                )
            }
        }
        if (drafts.isEmpty()) {
            _state.update { it.copy(errorMessage = "请至少勾选完成一组训练") }
            return
        }
        _state.update { it.copy(saving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.saveSession(
                sessionId = current.sessionId,
                dateEpochDay = current.dateEpochDay,
                note = current.note,
                exercises = drafts,
                durationMillis = current.finishedDurationMillis.takeIf { it > 0 }
                    ?: totalElapsed(),
            )
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = messageFor(result.exceptionOrNull()?.message),
                    )
                }
                return@launch
            }
            val savedId = result.getOrNull()
            val savedDuration = current.finishedDurationMillis.takeIf { it > 0 }
                ?: totalElapsed()
            if (savedId != null) {
                if (savedDuration > 0) {
                    timerRecordRepository?.saveSessionDuration(
                        sessionId = savedId,
                        dateEpochDay = current.dateEpochDay,
                        durationMillis = savedDuration,
                    )
                } else {
                    timerRecordRepository?.deleteSessionDuration(savedId)
                }
            }
            _state.update { it.copy(sessionId = savedId) }
            val photos = photoWallRepository
            val photoId = current.photoId
            val bitmap = current.photoBitmap
            if (photos != null && photoId != null && bitmap != null) {
                val photoSaved = withContext(Dispatchers.IO) {
                    photos.saveNew(
                        id = photoId,
                        bitmap = bitmap,
                        dateEpochDay = current.dateEpochDay,
                        note = current.note,
                    )
                }
                if (!photoSaved) {
                    _state.update {
                        it.copy(
                            saving = false,
                            errorMessage = "训练已保存，但照片保存失败，可以再试一次",
                        )
                    }
                    return@launch
                }
            }
            _state.update { it.copy(saving = false) }
            _exitAfterSave.tryEmit(Unit)
        }
    }

    fun save() = saveFinal()

    fun setClockMode(mode: WorkoutClockMode) {
        if (_clock.value.mode == mode) return
        clockStartedAt = null
        clockBaseRemainingMillis = WorkoutClockLogic.DEFAULT_TIMER_MILLIS
        stopwatchAccumulatedMillis = 0L
        _clock.value = WorkoutClockUi(
            mode = mode,
            displayMillis = if (mode == WorkoutClockMode.TIMER) {
                WorkoutClockLogic.DEFAULT_TIMER_MILLIS
            } else {
                0L
            },
        )
    }

    fun toggleClock() {
        val current = _clock.value
        if (current.running) {
            clockBaseRemainingMillis = current.displayMillis
            stopwatchAccumulatedMillis = current.displayMillis
            clockStartedAt = null
            _clock.update { it.copy(running = false) }
            return
        }
        val display = if (current.mode == WorkoutClockMode.TIMER && current.displayMillis <= 0L) {
            current.timerDurationMillis
        } else {
            current.displayMillis
        }
        clockStartedAt = elapsedRealtime()
        clockBaseRemainingMillis = display
        stopwatchAccumulatedMillis = display
        _clock.value = current.copy(running = true, displayMillis = display)
    }

    fun resetClock() {
        clockStartedAt = null
        val timerDuration = _clock.value.timerDurationMillis
        _clock.update {
            it.copy(
                running = false,
                displayMillis = if (it.mode == WorkoutClockMode.TIMER) timerDuration else 0L,
            )
        }
    }

    fun adjustTimer(deltaMillis: Long) {
        val current = _clock.value
        if (current.mode != WorkoutClockMode.TIMER) return
        val adjusted = WorkoutClockLogic.adjustTimer(
            durationMillis = current.timerDurationMillis,
            displayMillis = current.displayMillis,
            deltaMillis = deltaMillis,
        )
        if (current.running) {
            clockStartedAt = elapsedRealtime()
            clockBaseRemainingMillis = adjusted.displayMillis
        }
        _clock.value = current.copy(
            displayMillis = adjusted.displayMillis,
            timerDurationMillis = adjusted.durationMillis,
        )
    }

    private fun tickClock() {
        val current = _clock.value
        if (!current.running) return
        val started = clockStartedAt ?: return
        val elapsed = (elapsedRealtime() - started).coerceAtLeast(0L)
        if (current.mode == WorkoutClockMode.TIMER) {
            val remaining = (clockBaseRemainingMillis - elapsed).coerceAtLeast(0L)
            _clock.update { it.copy(displayMillis = remaining) }
            if (remaining == 0L) {
                clockStartedAt = null
                _clock.update { it.copy(running = false, displayMillis = 0L) }
                _clockFinished.tryEmit(Unit)
            }
        } else {
            _clock.update {
                it.copy(displayMillis = stopwatchAccumulatedMillis + elapsed)
            }
        }
    }

    fun pauseTotalTimer() {
        if (totalStartRealtime == null) return
        totalAccumulatedMillis = totalElapsed()
        totalStartRealtime = null
        totalTick.value++
    }

    fun resumeTotalTimer() {
        if (totalStartRealtime == null && !_state.value.finishing) {
            totalStartRealtime = elapsedRealtime()
            totalTick.value++
        }
    }

    fun setTotalDuration(durationMillis: Long) {
        totalAccumulatedMillis = durationMillis.coerceIn(0L, 86_399_000L)
        totalStartRealtime = elapsedRealtime()
        totalTick.value++
    }

    fun pauseClockForEdit() {
        val current = _clock.value
        if (!current.running) return
        clockBaseRemainingMillis = current.displayMillis
        stopwatchAccumulatedMillis = current.displayMillis
        clockStartedAt = null
        _clock.update { it.copy(running = false) }
    }

    fun setTimerDuration(durationMillis: Long) {
        val clamped = durationMillis.coerceIn(0L, 3_599_000L)
        clockStartedAt = null
        _clock.value = _clock.value.copy(
            mode = WorkoutClockMode.TIMER,
            running = false,
            displayMillis = clamped,
            timerDurationMillis = clamped,
        )
    }

    private fun totalElapsed(): Long {
        return totalAccumulatedMillis +
            (totalStartRealtime?.let { (elapsedRealtime() - it).coerceAtLeast(0L) } ?: 0L)
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
            completed = true,
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
            photoWallRepository: PhotoWallRepository? = null,
            timerRecordRepository: TimerRecordRepository? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionEditorViewModel(
                        repository = repository,
                        bodyRecordRepository = bodyRecordRepository,
                        existingSessionId = sessionId,
                        initialDateEpochDay = initialDateEpochDay,
                        photoWallRepository = photoWallRepository,
                        timerRecordRepository = timerRecordRepository,
                    ) as T
                }
            }
        }
    }
}
