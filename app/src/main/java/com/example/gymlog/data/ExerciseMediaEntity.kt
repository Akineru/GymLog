package com.example.gymlog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_media",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exerciseId"])],
)
data class ExerciseMediaEntity(
    @PrimaryKey val exerciseId: String,
    val mediaId: String?,
    val sourceImagePath: String?,
    val sourceGifPath: String?,
    val localThumbnailPath: String? = null,
    val localGifPath: String? = null,
    val remoteGifUrl: String? = null,
    val width: Int = 180,
    val height: Int = 180,
    val attribution: String? = null,
    val thumbnailSha256: String? = null,
    val gifSha256: String? = null,
    val thumbnailBytes: Long? = null,
    val gifBytes: Long? = null,
    val downloadState: String = ExerciseMediaStatus.NOT_BUNDLED,
)
