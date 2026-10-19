package com.example.gymlog.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.TrendLogic
import com.example.gymlog.data.TrendPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class BodyTrendMetric {
    WEIGHT,
    FAT,
    BMI,
}

data class BodyTrendUiState(
    val metric: BodyTrendMetric = BodyTrendMetric.WEIGHT,
    val points: List<TrendPoint> = emptyList(),
)

class BodyTrendViewModel(
    bodyRecordRepository: BodyRecordRepository,
) : ViewModel() {
    private val metric = MutableStateFlow(BodyTrendMetric.WEIGHT)

    val uiState: StateFlow<BodyTrendUiState> = combine(
        metric,
        bodyRecordRepository.observeMeasurements(),
    ) { selected, measurements ->
        val points = when (selected) {
            BodyTrendMetric.WEIGHT -> TrendLogic.weightSeries(measurements)
            BodyTrendMetric.FAT -> TrendLogic.bodyFatSeries(measurements)
            BodyTrendMetric.BMI -> TrendLogic.bmiSeries(measurements)
        }
        BodyTrendUiState(metric = selected, points = points)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BodyTrendUiState(),
    )

    fun onMetricSelected(value: BodyTrendMetric) {
        metric.value = value
    }

    companion object {
        fun factory(bodyRecordRepository: BodyRecordRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BodyTrendViewModel(bodyRecordRepository) as T
                }
            }
        }
    }
}
