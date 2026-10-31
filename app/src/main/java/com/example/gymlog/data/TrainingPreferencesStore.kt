package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 训练偏好（PR 相关）。
 *
 * 沿用项目现有做法：SharedPreferences + StateFlow，不进 Room、不进备份 JSON。
 * 目前只有一项「做组 PR 最低次数」，默认 6，范围 1..30（见 [PersonalRecordLogic]）。
 */
class TrainingPreferencesStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readMinWorkingReps())

    /** 做组 PR 的最低次数，供界面订阅。 */
    val workingPrMinReps: StateFlow<Int> = state.asStateFlow()

    /**
     * 同步读取，供 PR 计算路径使用。
     *
     * 每次都直接读 SharedPreferences，而不是返回缓存：用户可能刚改完设置就点「刷新成绩」，
     * 不能因为 StateFlow 还没下发就用旧值算。
     */
    fun workingPrMinRepsValue(): Int = readMinWorkingReps()

    fun setWorkingPrMinReps(value: Int) {
        val normalized = PersonalRecordLogic.normalizeMinWorkingReps(value)
        prefs.edit().putInt(KEY_WORKING_PR_MIN_REPS, normalized).apply()
        state.value = normalized
    }

    /** 存储值缺失 / 非法 / 越界时回落到默认值，绝不抛异常。 */
    private fun readMinWorkingReps(): Int {
        val raw = runCatching { prefs.getInt(KEY_WORKING_PR_MIN_REPS, DEFAULT) }
            .getOrDefault(DEFAULT)
        return PersonalRecordLogic.normalizeMinWorkingReps(raw)
    }

    companion object {
        const val PREFS_NAME = "gymlog_training_prefs"
        private const val KEY_WORKING_PR_MIN_REPS = "working_pr_min_reps"
        private const val DEFAULT = PersonalRecordLogic.DEFAULT_MIN_WORKING_REPS
    }
}
