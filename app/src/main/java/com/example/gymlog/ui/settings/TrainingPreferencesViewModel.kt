package com.example.gymlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.TrainingPreferencesStore
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 「刷新成绩」的结果，用于提示文案。 */
sealed interface RefreshResult {
    /** 成功，[updatedExercises] 是实际发生变化（PR 被改写）的动作数。 */
    data class Done(val updatedExercises: Int) : RefreshResult

    data object Failed : RefreshResult
}

data class TrainingPreferencesUiState(
    val refreshing: Boolean = false,
    val result: RefreshResult? = null,
)

class TrainingPreferencesViewModel(
    private val store: TrainingPreferencesStore,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    val minWorkingReps: StateFlow<Int> = store.workingPrMinReps

    private val mutableState = MutableStateFlow(TrainingPreferencesUiState())
    val state: StateFlow<TrainingPreferencesUiState> = mutableState.asStateFlow()

    /** 越界值会在存储层被收敛到合法区间，这里不需要再判断。 */
    fun setMinWorkingReps(value: Int) {
        store.setWorkingPrMinReps(value)
    }

    /**
     * 按当前设置重算全部历史记录的 PR。
     *
     * 真正做的是「重新扫一遍 workout_sets，按新规则算出极限 PR / 做组 PR，再写回 exercises 表」，
     * 不是刷新界面。重复点击通过 [TrainingPreferencesUiState.refreshing] 拦截。
     */
    fun refreshRecords() {
        if (mutableState.value.refreshing) return
        mutableState.value = mutableState.value.copy(refreshing = true, result = null)
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { workoutRepository.rebuildAllPersonalRecords() }
            }
            mutableState.value = TrainingPreferencesUiState(
                refreshing = false,
                result = outcome.fold(
                    onSuccess = { updated -> RefreshResult.Done(updated) },
                    onFailure = { RefreshResult.Failed },
                ),
            )
        }
    }

    fun consumeResult() {
        if (mutableState.value.result != null) {
            mutableState.value = mutableState.value.copy(result = null)
        }
    }

    companion object {
        fun factory(
            store: TrainingPreferencesStore,
            workoutRepository: WorkoutRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TrainingPreferencesViewModel(store, workoutRepository) as T
                }
            }
        }

        val minRepsRange: IntRange =
            PersonalRecordLogic.MIN_WORKING_REPS..PersonalRecordLogic.MAX_WORKING_REPS
    }
}
