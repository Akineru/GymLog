package com.example.gymlog.ui.calendar

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarColorPalette

/** Current calendar palette, provided once at the app root. */
val LocalCalendarColorPalette = staticCompositionLocalOf { CalendarColorPalette.Vivid }

internal fun CalendarColorPalette.composeColor(category: CalendarColorCategory) =
    androidx.compose.ui.graphics.Color(color(category))
