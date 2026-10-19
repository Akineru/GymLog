package com.example.gymlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyMetrics
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.Gender
import com.example.gymlog.data.MineDurationUnit
import com.example.gymlog.data.MineStats
import com.example.gymlog.data.ProfileAvatarStore
import com.example.gymlog.data.TimerRecordRepository
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MineUiState(
    val displayName: String? = null,
    val avatarPath: String? = null,
    val gender: Gender? = null,
    val ageYears: Int? = null,
    val trainingDays: Int = 0,
    val trainingYears: String = "0.0",
    val durationValue: String = "0",
    val durationUnit: MineDurationUnit = MineDurationUnit.MINUTE,
    val durationMinutesValue: String = "0",
    val bodyFatText: String? = null,
    val bodyWeightText: String? = null,
)

class MineViewModel(
    workoutRepository: WorkoutRepository,
    timerRecordRepository: TimerRecordRepository,
    private val bodyRecordRepository: BodyRecordRepository,
    private val avatarStore: ProfileAvatarStore,
) : ViewModel() {
    val state: StateFlow<MineUiState> = combine(
        workoutRepository.observeHistory(),
        timerRecordRepository.observeAll(),
        bodyRecordRepository.observeMeasurements(),
        bodyRecordRepository.observeProfile(),
    ) { sessions, timers, measurements, profile ->
        val durationMillis = MineStats.totalTimerMillis(timers.map { it.record })
        val duration = MineStats.formatDuration(durationMillis)
        val durationMinutes = MineStats.formatAsMinutes(durationMillis)
        val today = LocalDate.now()
        val birthday = profile?.birthdayEpochDay?.let(LocalDate::ofEpochDay)
        val trainingDays = MineStats.trainingDayCount(sessions, today.toEpochDay())
        MineUiState(
            displayName = profile?.displayName?.takeIf { it.isNotBlank() },
            avatarPath = avatarStore.file(profile?.avatarFileName)?.absolutePath,
            gender = Gender.fromStored(profile?.gender),
            ageYears = birthday?.let { BodyMetrics.ageYears(it, today) },
            trainingDays = trainingDays,
            trainingYears = MineStats.formatTrainingYears(trainingDays),
            durationValue = duration.value,
            durationUnit = duration.unit,
            durationMinutesValue = durationMinutes.value,
            bodyFatText = MineStats.latestBodyFatPercent(measurements)?.let(WeightLogic::formatKg),
            bodyWeightText = MineStats.latestBodyWeightKg(measurements)?.let(WeightLogic::formatKg),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MineUiState(),
    )

    fun saveProfile(
        displayName: String,
        gender: Gender?,
        avatarBytes: ByteArray?,
        clearAvatar: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = when {
                avatarBytes != null -> avatarStore.write(avatarBytes)
                clearAvatar -> {
                    avatarStore.delete()
                    null
                }
                else -> null
            }
            bodyRecordRepository.saveIdentity(
                displayName = displayName,
                gender = gender?.name,
                avatarFileName = fileName,
                clearAvatar = clearAvatar,
            )
        }
    }

    companion object {
        fun factory(
            workoutRepository: WorkoutRepository,
            timerRecordRepository: TimerRecordRepository,
            bodyRecordRepository: BodyRecordRepository,
            avatarStore: ProfileAvatarStore,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MineViewModel(
                        workoutRepository,
                        timerRecordRepository,
                        bodyRecordRepository,
                        avatarStore,
                    ) as T
                }
            }
        }
    }
}
