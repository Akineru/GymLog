package com.example.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoWallDao {
    @Query("SELECT * FROM photo_wall ORDER BY dateEpochDay DESC, createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PhotoWallEntity>>

    @Query("SELECT * FROM photo_wall WHERE id = :id")
    suspend fun get(id: String): PhotoWallEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: PhotoWallEntity)

    @Query(
        """
        UPDATE photo_wall
        SET editedRelativePath = :edited,
            dateEpochDay = :dateEpochDay,
            note = :note,
            width = :width,
            height = :height,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateSaved(
        id: String,
        edited: String?,
        dateEpochDay: Long,
        note: String,
        width: Int,
        height: Int,
        updatedAt: Long,
    )

    @Query("UPDATE photo_wall SET dateEpochDay = :dateEpochDay, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDate(id: String, dateEpochDay: Long, updatedAt: Long)

    @Query("UPDATE photo_wall SET note = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(id: String, note: String, updatedAt: Long)

    @Query("DELETE FROM photo_wall WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        """
        SELECT COUNT(*) FROM photo_wall
        WHERE originalRelativePath = :path OR editedRelativePath = :path
        """,
    )
    suspend fun countUsing(path: String): Int

    @Query("SELECT originalRelativePath, editedRelativePath FROM photo_wall")
    suspend fun allPaths(): List<PhotoPathRow>
}

data class PhotoPathRow(
    val originalRelativePath: String,
    val editedRelativePath: String?,
)
