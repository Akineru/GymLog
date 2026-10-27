package com.example.gymlog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.NotificationSettingsStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSettingsStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(NotificationSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun countdownAlertsAreEnabledByDefault() {
        assertTrue(
            NotificationSettingsStore(context).countdownAlertsEnabled.value,
        )
    }

    @Test
    fun choiceSurvivesANewStore() {
        NotificationSettingsStore(context).setCountdownAlertsEnabled(false)

        val reloaded = NotificationSettingsStore(context)
        assertFalse(reloaded.countdownAlertsEnabled.value)
    }

    @Test
    fun reEnablingIsPersisted() {
        NotificationSettingsStore(context).apply {
            setCountdownAlertsEnabled(false)
            setCountdownAlertsEnabled(true)
        }

        assertTrue(
            NotificationSettingsStore(context).countdownAlertsEnabled.value,
        )
    }
}
