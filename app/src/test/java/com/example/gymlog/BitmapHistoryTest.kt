package com.example.gymlog

import android.graphics.Bitmap
import com.example.gymlog.ui.photo.BitmapHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BitmapHistoryTest {

    private fun bitmap(): Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun undoRedoRoundTripReturnsTheSameInstances() {
        val history = BitmapHistory()
        val original = bitmap()
        val edited = bitmap()
        history.push(original)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        val back = history.undo(edited)
        assertEquals(original, back)
        assertTrue(history.canUndo.not())
        assertTrue(history.canRedo)
        val forward = history.redo(edited)
        assertEquals(edited, forward)
    }

    @Test
    fun undoReturnsNullAtTheStart() {
        val history = BitmapHistory()
        assertNull(history.undo(bitmap()))
    }

    @Test
    fun redoReturnsNullAtTheTip() {
        val history = BitmapHistory()
        assertNull(history.redo(bitmap()))
    }

    @Test
    fun pushEvictsTheOldestBeyondTheLimit() {
        val history = BitmapHistory(limit = 2)
        val first = bitmap()
        history.push(first)
        history.push(bitmap())
        history.push(bitmap())
        val third = history.undo(bitmap())
        val second = history.undo(third!!)
        assertNull(history.undo(second!!))
    }

    @Test
    fun pushDropsTheRedoBranch() {
        val history = BitmapHistory()
        val original = bitmap()
        history.push(original)
        history.undo(bitmap())
        assertTrue(history.canRedo)
        history.push(bitmap())
        assertFalse(history.canRedo)
    }

    @Test
    fun clearEmptiesBothBranches() {
        val history = BitmapHistory()
        history.push(bitmap())
        history.undo(bitmap())
        history.clear()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
