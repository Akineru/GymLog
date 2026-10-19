package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useWallpaperColors: Boolean = false,
)

class AppearanceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stateFlow = MutableStateFlow(read())
    val state: StateFlow<AppearanceSettings> = stateFlow.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        stateFlow.value = read()
    }

    fun setUseWallpaperColors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WALLPAPER_COLORS, enabled).apply()
        stateFlow.value = read()
    }

    private fun read(): AppearanceSettings {
        val stored = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        val mode = ThemeMode.entries.find { it.name == stored } ?: ThemeMode.SYSTEM
        return AppearanceSettings(
            themeMode = mode,
            useWallpaperColors = prefs.getBoolean(KEY_WALLPAPER_COLORS, false),
        )
    }

    companion object {
        const val PREFS_NAME = "gymlog_appearance"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_WALLPAPER_COLORS = "use_wallpaper_colors"
    }
}
