package com.example.gymlog.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyGirths
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.BodyMetrics
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.Gender
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WeightLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BodyRecordUiState(
    val birthdayEpochDay: Long? = null,
    val gender: Gender? = null,
    val heightText: String = "",
    val weightText: String = "",
    val bodyFatText: String = "",
    /** Current waist circumference; edited on the girth page. */
    val waistText: String = "",
    val armLeftText: String = "",
    val armRightText: String = "",
    val forearmLeftText: String = "",
    val forearmRightText: String = "",
    val chestText: String = "",
    val hipText: String = "",
    val thighLeftText: String = "",
    val thighRightText: String = "",
    val calfLeftText: String = "",
    val calfRightText: String = "",
    val ageYears: Int? = null,
    val bmiText: String? = null,
    val history: List<BodyMeasurementEntity> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class BodyRecordViewModel(
    private val repository: BodyRecordRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val form = MutableStateFlow(BodyRecordUiState())

    val state: StateFlow<BodyRecordUiState> = combine(
        form,
        repository.observeMeasurements(),
    ) { current, measurements ->
        val birthday = current.birthdayEpochDay?.let(LocalDate::ofEpochDay)
        val bmi = BodyMetrics.bmi(
            WeightLogic.parseKg(current.heightText),
            WeightLogic.parseKg(current.weightText),
        )
        current.copy(
            ageYears = birthday?.let { BodyMetrics.ageYears(it, LocalDate.now()) },
            bmiText = bmi?.let(BodyMetrics::displayBmi),
            history = measurements,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BodyRecordUiState(),
    )

    init {
        viewModelScope.launch {
            val profile = repository.observeProfile().first()
            val latest = repository.observeMeasurements().first().firstOrNull()
            form.update {
                it.copy(
                    birthdayEpochDay = profile?.birthdayEpochDay,
                    gender = Gender.fromStored(profile?.gender),
                    heightText = latest?.heightCm?.let(WeightLogic::formatKg).orEmpty(),
                    weightText = latest?.weightKg?.let(WeightLogic::formatKg).orEmpty(),
                    bodyFatText = latest?.bodyFatPercent?.let(WeightLogic::formatKg).orEmpty(),
                    waistText = latest?.waistCm?.let(WeightLogic::formatKg).orEmpty(),
                    armLeftText = latest?.armLeftCm?.let(WeightLogic::formatKg).orEmpty(),
                    armRightText = latest?.armRightCm?.let(WeightLogic::formatKg).orEmpty(),
                    forearmLeftText = latest?.forearmLeftCm?.let(WeightLogic::formatKg).orEmpty(),
                    forearmRightText = latest?.forearmRightCm?.let(WeightLogic::formatKg).orEmpty(),
                    chestText = latest?.chestCm?.let(WeightLogic::formatKg).orEmpty(),
                    hipText = latest?.hipCm?.let(WeightLogic::formatKg).orEmpty(),
                    thighLeftText = latest?.thighLeftCm?.let(WeightLogic::formatKg).orEmpty(),
                    thighRightText = latest?.thighRightCm?.let(WeightLogic::formatKg).orEmpty(),
                    calfLeftText = latest?.calfLeftCm?.let(WeightLogic::formatKg).orEmpty(),
                    calfRightText = latest?.calfRightCm?.let(WeightLogic::formatKg).orEmpty(),
                )
            }
            profileLoaded = true
        }
    }

    fun onBirthdayChange(epochDay: Long) {
        form.update { it.copy(birthdayEpochDay = epochDay, errorMessage = null, saved = false) }
    }

    fun onGenderChange(value: Gender?) {
        form.update { it.copy(gender = value, errorMessage = null, saved = false) }
    }

    fun onHeightChange(value: String) {
        form.update { it.copy(heightText = value, errorMessage = null, saved = false) }
    }

    fun onWeightChange(value: String) {
        form.update { it.copy(weightText = value, errorMessage = null, saved = false) }
    }

    fun onBodyFatChange(value: String) {
        form.update { it.copy(bodyFatText = value, errorMessage = null, saved = false) }
    }

    fun onGirthChange(field: GirthField, value: String) {
        form.update { current ->
            val next = when (field) {
                GirthField.ARM_LEFT -> current.copy(armLeftText = value)
                GirthField.ARM_RIGHT -> current.copy(armRightText = value)
                GirthField.FOREARM_LEFT -> current.copy(forearmLeftText = value)
                GirthField.FOREARM_RIGHT -> current.copy(forearmRightText = value)
                GirthField.CHEST -> current.copy(chestText = value)
                GirthField.WAIST -> current.copy(waistText = value)
                GirthField.HIP -> current.copy(hipText = value)
                GirthField.THIGH_LEFT -> current.copy(thighLeftText = value)
                GirthField.THIGH_RIGHT -> current.copy(thighRightText = value)
                GirthField.CALF_LEFT -> current.copy(calfLeftText = value)
                GirthField.CALF_RIGHT -> current.copy(calfRightText = value)
            }
            next.copy(errorMessage = null, saved = false)
        }
    }

    private var profileLoaded = false

    fun clearSaved() {
        form.update { it.copy(saved = false, errorMessage = null) }
    }

    fun save() {
        if (!profileLoaded) return
        val current = form.value
        val birthday = current.birthdayEpochDay
        if (birthday != null && LocalDate.ofEpochDay(birthday).isAfter(LocalDate.now())) {
            form.update { it.copy(errorMessage = "BIRTHDAY_INVALID") }
            return
        }
        val height = parseOptional(current.heightText)
        val weight = parseOptional(current.weightText)
        val bodyFat = parseOptional(current.bodyFatText)
        if (height == null && current.heightText.isNotBlank()) {
            form.update { it.copy(errorMessage = "HEIGHT_INVALID") }
            return
        }
        if (weight == null && current.weightText.isNotBlank()) {
            form.update { it.copy(errorMessage = "WEIGHT_INVALID") }
            return
        }
        if (bodyFat == null && current.bodyFatText.isNotBlank()) {
            form.update { it.copy(errorMessage = "BODY_FAT_INVALID") }
            return
        }
        viewModelScope.launch {
            val latest = repository.observeMeasurements().first().firstOrNull()
            // The profile form no longer edits waist. Preserve the latest saved
            // value without validating unsubmitted girth text.
            val waist = latest?.waistCm
            val recordMeasurement = BodyMetrics.metricsChanged(
                previous = latest,
                heightCm = height,
                weightKg = weight,
                bodyFatPercent = bodyFat,
                waistCm = waist,
            )
            val result = repository.saveSnapshot(
                birthdayEpochDay = birthday,
                gender = current.gender?.name,
                heightCm = height,
                weightKg = weight,
                bodyFatPercent = bodyFat,
                waistCm = waist,
                recordMeasurement = recordMeasurement,
            )
            if (result.isSuccess && recordMeasurement) {
                workoutRepository.rebuildAllPersonalRecords()
            }
            form.update {
                if (result.isSuccess) {
                    it.copy(errorMessage = null, saved = true)
                } else {
                    it.copy(errorMessage = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun saveGirths() {
        if (!profileLoaded) return
        val current = form.value
        val parsed = linkedMapOf(
            GirthField.ARM_LEFT to parseOptional(current.armLeftText),
            GirthField.ARM_RIGHT to parseOptional(current.armRightText),
            GirthField.FOREARM_LEFT to parseOptional(current.forearmLeftText),
            GirthField.FOREARM_RIGHT to parseOptional(current.forearmRightText),
            GirthField.CHEST to parseOptional(current.chestText),
            GirthField.WAIST to parseOptional(current.waistText),
            GirthField.HIP to parseOptional(current.hipText),
            GirthField.THIGH_LEFT to parseOptional(current.thighLeftText),
            GirthField.THIGH_RIGHT to parseOptional(current.thighRightText),
            GirthField.CALF_LEFT to parseOptional(current.calfLeftText),
            GirthField.CALF_RIGHT to parseOptional(current.calfRightText),
        )
        val texts = listOf(
            current.armLeftText, current.armRightText,
            current.forearmLeftText, current.forearmRightText,
            current.chestText, current.waistText, current.hipText,
            current.thighLeftText, current.thighRightText,
            current.calfLeftText, current.calfRightText,
        )
        if (parsed.values.zip(texts).any { (value, text) -> value == null && text.isNotBlank() }) {
            form.update { it.copy(errorMessage = "GIRTH_INVALID") }
            return
        }
        val girths = BodyGirths(
            armLeftCm = parsed[GirthField.ARM_LEFT],
            armRightCm = parsed[GirthField.ARM_RIGHT],
            forearmLeftCm = parsed[GirthField.FOREARM_LEFT],
            forearmRightCm = parsed[GirthField.FOREARM_RIGHT],
            chestCm = parsed[GirthField.CHEST],
            waistCm = parsed[GirthField.WAIST],
            hipCm = parsed[GirthField.HIP],
            thighLeftCm = parsed[GirthField.THIGH_LEFT],
            thighRightCm = parsed[GirthField.THIGH_RIGHT],
            calfLeftCm = parsed[GirthField.CALF_LEFT],
            calfRightCm = parsed[GirthField.CALF_RIGHT],
        )
        viewModelScope.launch {
            repository.saveGirths(girths)
            form.update { it.copy(errorMessage = null, saved = true) }
        }
    }

    private fun parseOptional(text: String): Double? {
        if (text.isBlank()) return null
        return WeightLogic.parseKg(text)
    }

    enum class GirthField {
        ARM_LEFT,
        ARM_RIGHT,
        FOREARM_LEFT,
        FOREARM_RIGHT,
        CHEST,
        WAIST,
        HIP,
        THIGH_LEFT,
        THIGH_RIGHT,
        CALF_LEFT,
        CALF_RIGHT,
    }

    companion object {
        fun factory(
            repository: BodyRecordRepository,
            workoutRepository: WorkoutRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BodyRecordViewModel(repository, workoutRepository) as T
                }
            }
        }
    }
}
