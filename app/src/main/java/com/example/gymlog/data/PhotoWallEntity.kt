package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [dateEpochDay] is a [java.time.LocalDate] epoch day: the calendar date the
 * user confirmed, not a UTC instant. Display it with LocalDate.ofEpochDay.
 */
@Entity(
    tableName = "photo_wall",
    indices = [Index(value = ["dateEpochDay"])],
)
data class PhotoWallEntity(
    @PrimaryKey val id: String,
    val originalRelativePath: String,
    val editedRelativePath: String?,
    val dateEpochDay: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val note: String,
    val width: Int,
    val height: Int,
) {
    fun displayPath(): String? {
        val edited = editedRelativePath?.takeIf { PhotoPaths.isSafe(it) }
        if (edited != null) return edited
        return originalRelativePath.takeIf { PhotoPaths.isSafe(it) }
    }
}
