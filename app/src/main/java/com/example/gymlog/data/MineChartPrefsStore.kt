package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 我的页统计图表偏好：记住用户上次选的时间段（最近1个月/3个月/1年/全期）。
 * 沿用项目现有做法：SharedPreferences + StateFlow，不进 Room、不进备份 JSON。
 * 存枚举名，UI 层负责解析，解析失败回落默认值。
 */
class MineChartPrefsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readRangeName())

    /** 上次选择的时间段（枚举名），供界面订阅。 */
    val chartRangeName: StateFlow<String> = state.asStateFlow()

    fun setChartRangeName(name: String) {
        prefs.edit().putString(KEY_CHART_RANGE, name).apply()
        state.value = name
    }

    private fun readRangeName(): String {
        return runCatching { prefs.getString(KEY_CHART_RANGE, null) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT
    }

    companion object {
        const val PREFS_NAME = "gymlog_mine_chart_prefs"
        private const val KEY_CHART_RANGE = "chart_range"
        private const val DEFAULT = "M3"
    }
}
