package com.example.gymlog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class GymLogViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GymLogApplication
    private val repository = app.workoutRepository
    private val dayNoteRepository = app.dayNoteRepository
    private val dayKindRepository = app.dayKindRepository

    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val measurements = app.bodyRecordRepository.observeMeasurements().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val dayKinds = dayKindRepository.observe().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val dayNotes = dayNoteRepository.observeMessages().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val sessions = repository.observeHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val exerciseMedia = app.exerciseMediaRepository.observeMedia().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val partsByDate: StateFlow<Map<LocalDate, List<String>>> = _calendarMonth
        .flatMapLatest { month ->
            repository.observeTrainedPartsByDate(
                startDate = month.atDay(1),
                endDate = month.atEndOfMonth(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    fun onCalendarMonthChange(month: YearMonth) {
        _calendarMonth.value = month
    }

    fun onCalendarDayClick(date: LocalDate) {
        _selectedDate.value = date
        val dateMonth = YearMonth.from(date)
        if (_calendarMonth.value != dateMonth) {
            _calendarMonth.value = dateMonth
        }
    }

    fun selectDayKind(date: LocalDate, kind: DayKind, hasTraining: Boolean) {
        val stored = dayKinds.value[date.toEpochDay()]
        val next = DayKindLogic.nextStored(hasTraining, stored, kind)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dayKindRepository.save(date.toEpochDay(), next)
            }
        }
    }

    fun saveDayNote(date: LocalDate, message: String, defaultMessage: String) {
        val trimmed = message.trim()
        val stored = trimmed.takeIf { it.isNotEmpty() && it != defaultMessage.trim() }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dayNoteRepository.save(date.toEpochDay(), stored)
            }
        }
    }
}
