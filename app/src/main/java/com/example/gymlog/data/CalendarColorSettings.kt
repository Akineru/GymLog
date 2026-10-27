package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CalendarColorMode {
    VIVID,
    SOFT,
    CUSTOM,
}

/** A color per merged calendar category, stored as ARGB. */
data class CalendarColorPalette(val colors: Map<CalendarColorCategory, Int>) {
    fun color(category: CalendarColorCategory): Int =
        colors[category] ?: category.argb

    companion object {
        /** 当前熟悉的部位色，作为柔和方案。 */
        val Current = CalendarColorPalette(
            CalendarColorCategory.entries.associateWith { it.argb },
        )

        /** 同色相里更亮、更透的版本，作为鲜明方案。 */
        val Vivid = CalendarColorPalette(
            mapOf(
                CalendarColorCategory.CHEST to 0xFFFF8A80.toInt(),
                CalendarColorCategory.BACK to 0xFF82B1FF.toInt(),
                CalendarColorCategory.SHOULDERS to 0xFFFFD740.toInt(),
                CalendarColorCategory.LEGS to 0xFFB9F6CA.toInt(),
                CalendarColorCategory.ARMS to 0xFFEA80FC.toInt(),
                CalendarColorCategory.OTHER to 0xFFCFD8DC.toInt(),
            ),
        )

        val Soft = Current
    }
}

data class CalendarColorSettings(
    val mode: CalendarColorMode = CalendarColorMode.VIVID,
    val customColors: Map<CalendarColorCategory, Int> = CalendarColorPalette.Current.colors,
) {
    fun palette(): CalendarColorPalette = when (mode) {
        CalendarColorMode.VIVID -> CalendarColorPalette.Vivid
        CalendarColorMode.SOFT -> CalendarColorPalette.Soft
        CalendarColorMode.CUSTOM -> CalendarColorPalette(customColors)
    }
}

class CalendarColorStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<CalendarColorSettings> = _state.asStateFlow()

    fun setMode(mode: CalendarColorMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _state.value = read()
    }

    fun setCustomColor(category: CalendarColorCategory, argb: Int) {
        prefs.edit().putInt(customKey(category), argb).apply()
        _state.value = read()
    }

    fun resetCustomColors() {
        prefs.edit().apply {
            CalendarColorCategory.entries.forEach { remove(customKey(it)) }
        }.apply()
        _state.value = read()
    }

    private fun read(): CalendarColorSettings {
        val stored = prefs.getString(KEY_MODE, CalendarColorMode.VIVID.name)
        val mode = CalendarColorMode.entries.find { it.name == stored } ?: CalendarColorMode.VIVID
        val custom = CalendarColorCategory.entries.associateWith { category ->
            prefs.getInt(customKey(category), category.argb)
        }
        return CalendarColorSettings(mode = mode, customColors = custom)
    }

    private fun customKey(category: CalendarColorCategory) = "custom_${category.name}"

    companion object {
        const val PREFS_NAME = "gymlog_calendar_colors"
        private const val KEY_MODE = "mode"
    }
}
