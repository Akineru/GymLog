package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.TimerRecordRepository
import com.example.gymlog.data.TimerState
import com.example.gymlog.data.TrainingTimerStore
import com.example.gymlog.ui.timer.TrainingTimerViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrainingTimerSaveTest {
    private lateinit var db: GymLogDatabase
    private lateinit var store: TrainingTimerStore
    private val day = LocalDate.of(2026, 10, 18).toEpochDay()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TrainingTimerStore(context)
        store.write(TimerState())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun finishingASessionSavesOnlyTheDate() = runTest {
        var now = 1_000L
        val viewModel = TrainingTimerViewModel(
            store = store,
            repository = TimerRecordRepository(db.timerRecordDao()),
            elapsedRealtime = { now },
        )

        viewModel.toggle(day)
        now += 5_000L
        viewModel.finish(day)

        val saved = db.timerRecordDao().observeAll().first { it.isNotEmpty() }
        assertEquals(1, saved.size)
        assertEquals(day, saved.single().record.dateEpochDay)
        assertEquals(5_000L, saved.single().record.durationMillis)
        assertNull(saved.single().record.sessionId)
    }
}
