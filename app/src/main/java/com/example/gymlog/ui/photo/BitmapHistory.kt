package com.example.gymlog.ui.photo

import android.graphics.Bitmap
import java.util.ArrayDeque

/**
 * Undo / redo stack of full-size bitmaps for the photo editor.
 *
 * Once [limit] entries are held, the oldest undo point is evicted and recycled.
 * Pushing a new entry clears the redo branch, and every discarded or evicted
 * bitmap is recycled, matching the editor's previous inline behaviour.
 */
internal class BitmapHistory(private val limit: Int = DEFAULT_LIMIT) {

    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Records [previous] as an undo point and drops the redo branch. */
    fun push(previous: Bitmap) {
        undoStack.addLast(previous)
        while (undoStack.size > limit) undoStack.removeFirst().recycle()
        redoStack.forEach { it.recycle() }
        redoStack.clear()
    }

    /** Steps back from [current]; returns the bitmap to show, or null at the start. */
    fun undo(current: Bitmap): Bitmap? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    /** Steps forward from [current]; returns the bitmap to show, or null at the tip. */
    fun redo(current: Bitmap): Bitmap? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        return redoStack.removeLast()
    }

    /** Recycles everything held by both branches. */
    fun clear() {
        undoStack.forEach { it.recycle() }
        redoStack.forEach { it.recycle() }
        undoStack.clear()
        redoStack.clear()
    }

    companion object {
        const val DEFAULT_LIMIT = 5
    }
}
