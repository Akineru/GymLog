package com.example.gymlog.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Read-only view of the media registry. The app does not package the Gym visual
 * files; [thumbnailPath]/[gifPath] are non-null only when a licensed local file
 * exists under filesDir/exercise_media (or an explicit existing local path).
 */
data class ExerciseMediaView(
    val exerciseId: String,
    val thumbnailPath: String?,
    val gifPath: String?,
    val thumbnailAssetPath: String? = null,
    val gifAssetPath: String? = null,
    val attribution: String?,
    val downloadState: String,
) {
    val hasThumbnail: Boolean get() = thumbnailPath != null || thumbnailAssetPath != null
    val hasGif: Boolean get() = gifPath != null || gifAssetPath != null
}

class ExerciseMediaRepository(
    private val context: Context,
    private val catalogDao: ExerciseCatalogDao,
) {
    private val mediaRoot: File = File(context.filesDir, MEDIA_DIRECTORY)

    private val packagedAssets: Set<String> by lazy {
        buildSet {
            listAssetDirectory("$ASSET_MEDIA_ROOT/images")
            listAssetDirectory("$ASSET_MEDIA_ROOT/videos")
        }
    }

    fun observeMedia(): Flow<Map<String, ExerciseMediaView>> =
        catalogDao.observeAllMedia().map { rows ->
            rows.associate { it.exerciseId to it.toView() }
        }.flowOn(Dispatchers.IO)

    suspend fun mediaForExercise(exerciseId: String): ExerciseMediaView? =
        catalogDao.mediaForExercise(exerciseId)?.toView()

    fun viewOf(entity: ExerciseMediaEntity): ExerciseMediaView = entity.toView()

    private fun ExerciseMediaEntity.toView(): ExerciseMediaView {
        val localThumbnail = resolveFile(localThumbnailPath)
        val localGif = resolveFile(localGifPath)
        return ExerciseMediaView(
            exerciseId = exerciseId,
            thumbnailPath = localThumbnail,
            gifPath = localGif,
            thumbnailAssetPath = localThumbnail ?: resolveAsset(sourceImagePath),
            gifAssetPath = localGif ?: resolveAsset(sourceGifPath),
            attribution = attribution,
            downloadState = downloadState,
        )
    }

    private fun resolveFile(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (file.isAbsolute) return file.takeIf { it.isFile }?.absolutePath
        val candidate = File(mediaRoot, path)
        return candidate.takeIf { it.isFile }?.absolutePath
    }

    private fun resolveAsset(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val assetPath = "$ASSET_MEDIA_ROOT/$path"
        return assetPath.takeIf { it in packagedAssets }
    }

    private fun MutableSet<String>.listAssetDirectory(directory: String) {
        val names = runCatching { context.assets.list(directory) }.getOrNull().orEmpty()
        names.forEach { name -> add("$directory/$name") }
    }

    companion object {
        const val MEDIA_DIRECTORY = "exercise_media"
        const val ASSET_MEDIA_ROOT = "exercises/media"
    }
}
