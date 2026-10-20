package com.example.gymlog

import com.example.gymlog.ui.settings.ReleaseHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseHistoryTest {
    @Test
    fun newestNoteIsVersionOnePointFive() {
        val notes = ReleaseHistory.notes
        assertEquals("1.5", notes.first().versionName)
        assertTrue(notes.first().sections.all { it.items.isNotEmpty() })
        val versions = notes.map { note ->
            val parts = note.versionName.split('.').map { it.toInt() }
            List(3) { index -> parts.getOrElse(index) { 0 } }
        }
        for (index in 0 until versions.lastIndex) {
            val newer = versions[index]
            val older = versions[index + 1]
            val descending = newer.zip(older).firstOrNull { (left, right) -> left != right }
                ?.let { (left, right) -> left > right }
                ?: false
            assertTrue(descending)
        }
    }
}
