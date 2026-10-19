package com.example.gymlog.ui.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.TimerLapEntity
import com.example.gymlog.data.TimerPhase
import com.example.gymlog.data.TimerRecordEntity
import com.example.gymlog.data.TimerRecordRepository
import com.example.gymlog.data.TimerRecordWithLaps
import com.example.gymlog.data.TimerState
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.data.TrainingTimerStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class TimerMessage {
    SAVED,
    SAVE_FAILED,
    NOTHING_TO_SAVE,
    RECOVERED,
}

data class TrainingTimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val elapsedMillis: Long = 0,
    val formatted: String = TrainingTimerLogic.formatDuration(0),
    val laps: List<com.example.gymlog.data.TimerLap> = emptyList(),
    val recoveredAfterReboot: Boolean = false,
    val saving: Boolean = false,
    val saved: List<TimerRecordWithLaps> = emptyList(),
    val recordDateEpochDay: Long? = null,
) {
    val showDigits: Boolean get() = phase != TimerPhase.IDLE
}

class TrainingTimerViewModel(
    private val store: TrainingTimerStore,
    private val repository: TimerRecordRepository,
    private val elapsedRealtime: () -> Long,
) : ViewModel() {
    private val snapshot = MutableStateFlow(TimerState())
    private val tick = MutableStateFlow(0)
    private val saving = MutableStateFlow(false)
    private val messagesFlow = MutableSharedFlow<TimerMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<TimerMessage> = messagesFlow.asSharedFlow()

    val uiState: StateFlow<TrainingTimerUiState> = combine(
        snapshot,
        tick,
        repository.observeAll(),
        saving,
    ) { state, _, saved, isSaving ->
        val elapsed = TrainingTimerLogic.displayedMillis(state, elapsedRealtime())
        TrainingTimerUiState(
            phase = state.phase,
            elapsedMillis = elapsed,
            formatted = TrainingTimerLogic.formatDuration(elapsed),
            laps = state.laps,
            recoveredAfterReboot = state.recoveredAfterReboot,
            saving = isSaving,
            saved = saved,
            recordDateEpochDay = state.recordDateEpochDay,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrainingTimerUiState(),
    )

    init {
        val restored = TrainingTimerLogic.restore(
            state = store.read(),
            nowElapsedRealtime = elapsedRealtime(),
            savedBootCount = store.savedBootCount(),
            currentBootCount = store.currentBootCount(),
        )
        snapshot.value = restored
        store.write(restored)
        if (restored.recoveredAfterReboot) {
            messagesFlow.tryEmit(TimerMessage.RECOVERED)
        }
        viewModelScope.launch {
            snapshot.collect { state ->
                if (state.phase == TimerPhase.RUNNING) {
                    while (isActive && snapshot.value.phase == TimerPhase.RUNNING) {
                        tick.value = tick.value + 1
                        kotlinx.coroutines.delay(200)
                    }
                }
            }
        }
    }

    fun toggle(dateEpochDay: Long) = update {
        TrainingTimerLogic.toggle(it, elapsedRealtime(), dateEpochDay)
    }

    fun edit(millis: Long) = update { TrainingTimerLogic.edit(it, elapsedRealtime(), millis) }

    fun lap() = update { TrainingTimerLogic.lap(it, elapsedRealtime()) }

    fun reset(clearLaps: Boolean) = update { TrainingTimerLogic.resetTime(it, clearLaps) }

    fun deleteSaved(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun updateSavedDuration(id: String, millis: Long) {
        viewModelScope.launch {
            repository.updateDuration(id, millis.coerceAtLeast(0))
        }
    }

    fun finish(dateEpochDay: Long) {
        if (saving.value) return
        val settled = TrainingTimerLogic.pause(snapshot.value, elapsedRealtime())
        val elapsed = TrainingTimerLogic.displayedMillis(settled, elapsedRealtime())
        if (elapsed <= 0 && settled.laps.isEmpty()) {
            update { TrainingTimerLogic.resetTime(it, clearLaps = true) }
            messagesFlow.tryEmit(TimerMessage.NOTHING_TO_SAVE)
            return
        }
        update { settled }
        val laps = settled.laps
        saving.value = true
        viewModelScope.launch {
            try {
                val recordId = UUID.randomUUID().toString()
                repository.save(
                    record = TimerRecordEntity(
                        id = recordId,
                        durationMillis = elapsed,
                        dateEpochDay = settled.recordDateEpochDay ?: dateEpochDay,
                        sessionId = null,
                        createdAt = System.currentTimeMillis(),
                    ),
                    laps = laps.map { lap ->
                        TimerLapEntity(
                            id = "$recordId-${lap.index}",
                            recordId = recordId,
                            lapIndex = lap.index,
                            intervalMillis = lap.intervalMillis,
                            elapsedMillis = lap.elapsedMillis,
                        )
                    },
                )
                update { TrainingTimerLogic.resetTime(it, clearLaps = true) }
                messagesFlow.tryEmit(TimerMessage.SAVED)
            } catch (_: Exception) {
                messagesFlow.tryEmit(TimerMessage.SAVE_FAILED)
            } finally {
                saving.value = false
            }
        }
    }

    private fun update(transform: (TimerState) -> TimerState) {
        val next = transform(snapshot.value)
        snapshot.value = next
        store.write(next)
    }

    companion object {
        fun factory(app: GymLogApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrainingTimerViewModel(
                        store = app.trainingTimerStore,
                        repository = app.timerRecordRepository,
                        elapsedRealtime = { SystemClock.elapsedRealtime() },
                    ) as T
                }
            }
        }
    }
}
