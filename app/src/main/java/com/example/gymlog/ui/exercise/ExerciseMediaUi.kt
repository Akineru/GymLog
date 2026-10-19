package com.example.gymlog.ui.exercise

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Movie
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMediaView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun ExerciseThumbnail(
    exercise: ExerciseEntity,
    media: ExerciseMediaView?,
    modifier: Modifier = Modifier.size(72.dp),
) {
    val context = LocalContext.current
    val path = media?.thumbnailPath
    val assetPath = media?.thumbnailAssetPath
    val bitmap by produceState<Bitmap?>(initialValue = null, path, assetPath) {
        value = if (path == null && assetPath == null) {
            null
        } else {
            ExerciseMediaCache.thumbnail(context, path, assetPath)
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = exercise.name,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        // The existing diagram fallback is already a local drawable and needs no
        // media import. It is also the no-media placeholder.
        ExerciseDiagram(exercise = exercise, modifier = modifier)
    }
}

@Composable
fun ExerciseGifPlayer(
    media: ExerciseMediaView?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val path = media?.gifPath
    val assetPath = media?.gifAssetPath
    if (path == null && assetPath == null) {
        ExerciseDemoEmpty(modifier)
        return
    }
    val movie by produceState<Movie?>(initialValue = null, path, assetPath) {
        value = ExerciseMediaCache.movie(context, path, assetPath)
    }
    val loaded = movie
    if (loaded == null) {
        // A short empty state is preferable to an endless spinner while a missing
        // or corrupt GIF is being handled.
        ExerciseDemoEmpty(modifier)
        return
    }
    var frameTick by remember(loaded) { mutableIntStateOf(0) }
    LaunchedEffect(loaded) {
        var lastFrameNanos = 0L
        val duration = max(loaded.duration(), 1)
        while (isActive) {
            withFrameNanos { now ->
                if (lastFrameNanos == 0L) lastFrameNanos = now
                val elapsedMillis = ((now - lastFrameNanos) / 1_000_000L).toInt()
                loaded.setTime(elapsedMillis % duration)
                frameTick++
            }
        }
    }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        frameTick
        val width = loaded.width().coerceAtLeast(1)
        val height = loaded.height().coerceAtLeast(1)
        val scale = min(size.width / width, size.height / height)
        val left = (size.width - width * scale) / 2f
        val top = (size.height - height * scale) / 2f
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.translate(left, top)
            native.scale(scale, scale)
            loaded.draw(native, 0f, 0f)
            native.restore()
        }
    }
}

@Composable
private fun ExerciseDemoEmpty(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.exercise_no_demo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

internal object ExerciseMediaCache {
    private val thumbnails = object : LruCache<String, Bitmap>(32) {}
    private val movies = object : LruCache<String, Movie>(4) {}

    suspend fun thumbnail(
        context: android.content.Context,
        path: String?,
        assetPath: String?,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = assetPath?.let { "asset:$it" } ?: path ?: return@withContext null
        thumbnails.get(key)?.let { return@withContext it }
        val decoded = runCatching {
            if (assetPath != null) {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(path)
            }
        }.onFailure { Log.w("ExerciseMedia", "thumbnail decode failed: $key", it) }
            .getOrNull()
        if (decoded != null) thumbnails.put(key, decoded)
        decoded
    }

    suspend fun movie(
        context: android.content.Context,
        path: String?,
        assetPath: String?,
    ): Movie? = withContext(Dispatchers.IO) {
        val key = assetPath?.let { "asset:$it" } ?: path ?: return@withContext null
        movies.get(key)?.let { return@withContext it }
        val decoded = runCatching {
            if (assetPath != null) {
                context.assets.open(assetPath).use { Movie.decodeStream(it) }
            } else {
                Movie.decodeFile(path)
            }
        }.onFailure { Log.w("ExerciseMedia", "gif decode failed: $key", it) }
            .getOrNull()
        if (decoded != null) movies.put(key, decoded)
        decoded
    }
}
