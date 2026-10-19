package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.AppearanceStore
import com.example.gymlog.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AppearanceStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsFollowSystemAndUseTheAppPalette() {
        val store = AppearanceStore(context)
        assertEquals(ThemeMode.SYSTEM, store.state.value.themeMode)
        assertFalse(store.state.value.useWallpaperColors)
    }

    @Test
    fun choicesSurviveANewStore() {
        AppearanceStore(context).apply {
            setThemeMode(ThemeMode.DARK)
            setUseWallpaperColors(false)
        }
        val again = AppearanceStore(context)
        assertEquals(ThemeMode.DARK, again.state.value.themeMode)
        assertFalse(again.state.value.useWallpaperColors)
    }
}
