package com.example.gymlog

import com.example.gymlog.data.DraftExerciseSnapshot
import com.example.gymlog.data.DraftSetSnapshot
import com.example.gymlog.data.SessionDraftSnapshot
import com.example.gymlog.data.SessionDraftStore
import com.example.gymlog.data.WeightMode
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionDraftStoreTest {
    @Test
    fun roundTripPreservesDraftContent() = runTest {
        val dir = Files.createTempDirectory("gymlog-draft").toFile()
        val store = SessionDraftStore(dir)
        val snapshot = SessionDraftSnapshot(
            draftKey = "new-1",
            sessionId = null,
            dateEpochDay = 20_000L,
            note = "练腿",
            totalElapsedMillis = 123_000L,
            timerRunning = true,
            savedAtEpochMillis = 456_000L,
            finishing = false,
            finishedDurationMillis = 0L,
            photoId = "photo-1",
            exercises = listOf(
                DraftExerciseSnapshot(
                    id = "draft-exercise",
                    exerciseId = "squat",
                    startedAtMillis = 1_000L,
                    completedAtMillis = 2_000L,
                    durationMillis = 1_000L,
                    sets = listOf(
                        DraftSetSnapshot(
                            id = "set-1",
                            mode = WeightMode.BODYWEIGHT_PLUS,
                            normalKgText = "",
                            extraKgText = "20",
                            repsText = "8",
                            completed = true,
                        ),
                    ),
                ),
            ),
        )

        store.save(snapshot)
        val restored = store.load()

        assertEquals(snapshot, restored)
        assertTrue(store.load()?.exercises?.single()?.sets?.single()?.completed == true)
    }

    @Test
    fun clearRemovesSnapshot() = runTest {
        val dir = Files.createTempDirectory("gymlog-draft").toFile()
        val store = SessionDraftStore(dir)
        store.save(
            SessionDraftSnapshot(
                draftKey = "new-1",
                sessionId = null,
                dateEpochDay = 1L,
                note = "",
                totalElapsedMillis = 0L,
                timerRunning = false,
                savedAtEpochMillis = 0L,
                finishing = false,
                finishedDurationMillis = 0L,
                photoId = null,
                exercises = emptyList(),
            ),
        )
        assertNotNull(store.load())
        store.clear()
        assertNull(store.load())
        assertNull(SessionDraftStore(dir).load())
    }
}
