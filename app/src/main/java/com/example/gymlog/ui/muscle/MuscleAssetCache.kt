package com.example.gymlog.ui.muscle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.gymlog.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class MuscleSideAssets(
    val base: ImageBitmap,
    /** Null when a mask failed to load; the base figure is still usable. */
    val masks: MuscleMaskStore?,
    val masksError: String? = null,
)

internal data class MuscleAssets(
    val front: MuscleSideAssets? = null,
    val back: MuscleSideAssets? = null,
    val girthAnchors: List<GirthAnchorPoint> = emptyList(),
)

/**
 * Lazily decodes one side at a time, off the main thread. Body record only needs
 * the front side; the muscle map loads the back side when the user switches.
 * Mask resources are process-wide and shared by body record and the muscle map.
 */
internal object MuscleAssetCache {
    private val mutex = Mutex()
    private val state = MutableStateFlow(MuscleAssets())
    private val loadingState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<Map<Boolean, String>>(emptyMap())

    val assets: StateFlow<MuscleAssets> = state.asStateFlow()
    val loading: StateFlow<Boolean> = loadingState.asStateFlow()
    val errors: StateFlow<Map<Boolean, String>> = errorState.asStateFlow()

    suspend fun loadSide(
        context: Context,
        front: Boolean,
        force: Boolean = false,
    ) {
        if (!force && sideAssets(front) != null) return
        mutex.withLock {
            if (!force && sideAssets(front) != null) return
            loadingState.value = true
            errorState.update { it - front }
            try {
                val app = context.applicationContext
                val loaded = withContext(Dispatchers.Default) {
                    val base = decode(app, if (front) R.drawable.muscle_front_base else R.drawable.muscle_back_base)
                    val masksResult = runCatching {
                        MuscleMaskStore(app.assets, front).also { it.prepare() }
                    }
                    val masks = masksResult.getOrNull()
                    val anchors = if (front && masks != null) {
                        runCatching { GirthAnchors.resolve(masks) }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    LoadedSide(
                        base = base,
                        masks = masks,
                        masksError = masksResult.exceptionOrNull()?.message,
                        anchors = anchors,
                    )
                }
                state.update { current ->
                    if (front) {
                        current.copy(
                            front = MuscleSideAssets(loaded.base, loaded.masks, loaded.masksError),
                            girthAnchors = loaded.anchors,
                        )
                    } else {
                        current.copy(
                            back = MuscleSideAssets(loaded.base, loaded.masks, loaded.masksError),
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val message = error.message ?: error::class.java.simpleName
                errorState.update { it + (front to message) }
            } finally {
                loadingState.value = false
            }
        }
    }

    suspend fun retry(context: Context, front: Boolean) {
        loadSide(context, front, force = true)
    }

    internal fun resetForTesting() {
        state.value = MuscleAssets()
        loadingState.value = false
        errorState.value = emptyMap()
    }

    private fun sideAssets(front: Boolean): MuscleSideAssets? {
        return if (front) state.value.front else state.value.back
    }

    private fun decode(context: Context, resId: Int): ImageBitmap {
        val bitmap = context.resources.openRawResource(resId).use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Missing muscle figure $resId")
        return bitmap.asImageBitmap()
    }

    private class LoadedSide(
        val base: ImageBitmap,
        val masks: MuscleMaskStore?,
        val masksError: String?,
        val anchors: List<GirthAnchorPoint>,
    )
}
