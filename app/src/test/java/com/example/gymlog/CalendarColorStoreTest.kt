package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.CalendarColorCategory
import com.example.gymlog.data.CalendarColorMode
import com.example.gymlog.data.CalendarColorStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarColorStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(CalendarColorStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultIsVivid() {
        val settings = CalendarColorStore(context).state.value
        assertEquals(CalendarColorMode.VIVID, settings.mode)
        assertEquals(CalendarColorCategory.CHEST.argb, settings.customColors[CalendarColorCategory.CHEST])
    }

    @Test
    fun modeAndCustomColorsSurviveANewStore() {
        CalendarColorStore(context).apply {
            setMode(CalendarColorMode.CUSTOM)
            setCustomColor(CalendarColorCategory.CHEST, 0xFF123456.toInt())
        }

        val reloaded = CalendarColorStore(context).state.value
        assertEquals(CalendarColorMode.CUSTOM, reloaded.mode)
        assertEquals(0xFF123456.toInt(), reloaded.customColors[CalendarColorCategory.CHEST])
    }

    @Test
    fun resetRestoresVividCustomColors() {
        CalendarColorStore(context).apply {
            setCustomColor(CalendarColorCategory.BACK, 0xFFABCDEF.toInt())
            resetCustomColors()
        }

        assertEquals(
            CalendarColorCategory.BACK.argb,
            CalendarColorStore(context).state.value.customColors[CalendarColorCategory.BACK],
        )
    }
}
