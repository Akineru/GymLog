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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 图表统计范围。 */
enum class MineChartRange(val label: String) {
    M1("最近1个月"),
    M3("最近3个月"),
    Y1("最近1年"),
    ALL("全期"),
}

/** 一周的柱状图数据。 */
data class MineWeeklyBar(
    val weekStart: LocalDate,
    val durationMillis: Long,
    val volumeKg: Double,
    val trainingDays: Int,
)

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
    val chartRange: MineChartRange = MineChartRange.M3,
    val weeklyBars: List<MineWeeklyBar> = emptyList(),
)

class MineViewModel(
    workoutRepository: WorkoutRepository,
    timerRecordRepository: TimerRecordRepository,
    private val bodyRecordRepository: BodyRecordRepository,
    private val avatarStore: ProfileAvatarStore,
    private val chartPrefsStore: com.example.gymlog.data.MineChartPrefsStore,
) : ViewModel() {
    // 时间段持久化：初始值来自上次保存的选择
    private val chartRange = MutableStateFlow(readRange(chartPrefsStore.chartRangeName.value))

    init {
        viewModelScope.launch {
            chartPrefsStore.chartRangeName.collect { chartRange.value = readRange(it) }
        }
    }

    fun setChartRange(range: MineChartRange) {
        chartPrefsStore.setChartRangeName(range.name)
    }

    private fun readRange(name: String): MineChartRange {
        return runCatching { MineChartRange.valueOf(name) }.getOrDefault(MineChartRange.M3)
    }

    val state: StateFlow<MineUiState> = combine(
        workoutRepository.observeHistory(),
        timerRecordRepository.observeAll(),
        bodyRecordRepository.observeMeasurements(),
        bodyRecordRepository.observeProfile(),
        chartRange,
    ) { sessions, timers, measurements, profile, range ->
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
            chartRange = range,
            weeklyBars = weeklyBars(sessions, timers.map { it.record }, measurements, range, today),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MineUiState(),
    )

    /** 按周聚合：rangeStart 所在周到本周，每周一条；柱数超过 52 时自动合并相邻周。 */
    private fun weeklyBars(
        sessions: List<com.example.gymlog.data.SessionDetail>,
        timerRecords: List<com.example.gymlog.data.TimerRecordEntity>,
        measurements: List<com.example.gymlog.data.BodyMeasurementEntity>,
        range: MineChartRange,
        today: LocalDate,
    ): List<MineWeeklyBar> {
        val rangeStart = when (range) {
            MineChartRange.M1 -> today.minusMonths(1)
            MineChartRange.M3 -> today.minusMonths(3)
            MineChartRange.Y1 -> today.minusYears(1)
            MineChartRange.ALL -> sessions.minOfOrNull {
                LocalDate.ofEpochDay(it.session.dateEpochDay)
            } ?: today
        }
        val firstWeek = com.example.gymlog.ui.home.HomePeriod.weekStart(rangeStart)
        val lastWeek = com.example.gymlog.ui.home.HomePeriod.weekStart(today)
        val totalWeeks = (((lastWeek.toEpochDay() - firstWeek.toEpochDay()) / 7) + 1).toInt()
        val stride = if (totalWeeks > 52) (totalWeeks + 51) / 52 else 1
        val bars = ArrayList<MineWeeklyBar>()
        var week = firstWeek
        while (!week.isAfter(lastWeek)) {
            // stride>1 时一根柱代表连续 stride 周（末段截止到今天）
            val spanEnd = minOf(week.plusDays(7L * stride - 1), today)
            val summary = com.example.gymlog.ui.home.HomePeriod.summarize(
                sessions, timerRecords, measurements, week, spanEnd,
            )
            bars += MineWeeklyBar(
                weekStart = week,
                durationMillis = summary.durationMillis,
                volumeKg = summary.volumeKg,
                trainingDays = summary.trainingDays,
            )
            week = week.plusWeeks(stride.toLong())
        }
        return bars
    }

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
            chartPrefsStore: com.example.gymlog.data.MineChartPrefsStore,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MineViewModel(
                        workoutRepository,
                        timerRecordRepository,
                        bodyRecordRepository,
                        avatarStore,
                        chartPrefsStore,
                    ) as T
                }
            }
        }
    }
}
