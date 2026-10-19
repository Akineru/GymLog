package com.example.gymlog.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class HomeUiState(
    val mode: HomeRangeMode = HomeRangeMode.WEEK,
    val selectedDate: LocalDate = LocalDate.now(),
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
    }

    fun shift(forward: Boolean) {
        _state.update { current ->
            current.copy(selectedDate = HomePeriod.shift(current.mode, current.selectedDate, forward))
        }
    }

    fun setMode(mode: HomeRangeMode) {
        _state.update { current ->
            if (current.mode == mode) {
                current
            } else {
                current.copy(mode = mode, selectedDate = LocalDate.now())
            }
        }
    }

    fun jumpToToday() {
        _state.update { it.copy(selectedDate = LocalDate.now()) }
    }
}
