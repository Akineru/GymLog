package com.example.gymlog.ui.photo

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.PhotoImage
import com.example.gymlog.data.PhotoWallEntity
import com.example.gymlog.data.PhotoWallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

enum class PhotoTool {
    DRAW,
    TEXT,
    CROP,
    MOSAIC,
    BLUR,
}

data class PhotoWallUiState(
    val all: List<PhotoWallEntity> = emptyList(),
    val photos: List<PhotoWallEntity> = emptyList(),
    val month: YearMonth? = null,
    val months: List<YearMonth> = emptyList(),
    val message: String? = null,
)

data class PhotoEditorUi(
    val id: String,
    val isNew: Boolean,
    val bitmap: Bitmap,
    val tool: PhotoTool = PhotoTool.MOSAIC,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val continueFromSaved: Boolean = false,
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val note: String = "",
    val index: Int = 1,
    val total: Int = 1,
    val brushColor: Int = 0xFF172033.toInt(),
    val brushWidth: Float = 18f,
    val blurRadius: Int = 8,
)

class PhotoWallViewModel(
    private val repository: PhotoWallRepository,
) : ViewModel() {
    private val month = MutableStateFlow<YearMonth?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val editor = MutableStateFlow<PhotoEditorUi?>(null)
    private val edits = Mutex()
    private val history = BitmapHistory()
    private val queue = ArrayList<String>()
    private var queueIndex = 0
    private var saving = false

    val wall: StateFlow<PhotoWallUiState> = combine(
        repository.observe(),
        month,
        message,
    ) { rows, selected, notice ->
        val sorted = PhotoWallRepository.sort(rows)
        val months = sorted.map { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }.distinct()
        PhotoWallUiState(
            all = sorted,
            photos = PhotoWallRepository.inMonth(sorted, selected),
            month = selected,
            months = months,
            message = notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoWallUiState())

    val editorState: StateFlow<PhotoEditorUi?> = editor

    init {
        viewModelScope.launch(Dispatchers.IO) { runCatching { repository.reconcile() } }
    }

    fun setMonth(value: YearMonth?) {
        month.value = value
    }

    fun dismissMessage() {
        message.value = null
    }

    fun captureUri() = repository.captureUri()

    fun stageUris(uris: List<Uri>, onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ids = ArrayList<String>()
            for (uri in uris) {
                val id = withContext(Dispatchers.IO) { repository.stageImport(uri) }
                if (id != null) ids += id
            }
            if (ids.isEmpty()) {
                message.value = "没有读到照片"
                onReady(false)
                return@launch
            }
            startQueue(ids, onReady)
        }
    }

    fun stageCapture(onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) { repository.stageCapture() }
            if (id == null) {
                message.value = "没有拿到拍摄的照片"
                onReady(false)
                return@launch
            }
            startQueue(listOf(id), onReady)
        }
    }

    fun discardCapture() {
        repository.discardCapture()
    }

    fun editExisting(id: String, onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val row = withContext(Dispatchers.IO) { repository.get(id) }
            val bitmap = withContext(Dispatchers.IO) { repository.decode(id, preferEdited = true) }
            if (row == null || bitmap == null) {
                message.value = "这张照片打不开"
                onReady(false)
                return@launch
            }
            history.clear()
            queue.clear()
            editor.value = PhotoEditorUi(
                id = id,
                isNew = false,
                bitmap = bitmap,
                continueFromSaved = row.editedRelativePath != null,
                dirty = false,
                dateEpochDay = row.dateEpochDay,
                note = row.note,
            )
            onReady(true)
        }
    }

    fun setTool(tool: PhotoTool) {
        editor.value = editor.value?.copy(tool = tool)
    }

    fun setBrushColor(color: Int) {
        editor.value = editor.value?.copy(brushColor = color)
    }

    fun setBrushWidth(width: Float) {
        editor.value = editor.value?.copy(brushWidth = width)
    }

    fun setBlurRadius(radius: Int) {
        editor.value = editor.value?.copy(blurRadius = radius)
    }

    fun setDate(epochDay: Long) {
        editor.value = editor.value?.copy(dateEpochDay = epochDay, dirty = true)
    }

    fun setNote(note: String) {
        editor.value = editor.value?.copy(note = note.take(PhotoImage.NOTE_LIMIT), dirty = true)
    }

    fun rotate() = mutate { PhotoImage.rotate(it, 90) }

    fun crop(rect: Rect) = mutate { PhotoImage.crop(it, rect) }

    fun draw(points: List<Pair<Float, Float>>) {
        val state = editor.value ?: return
        if (points.size < 2) return
        mutate { PhotoImage.stroke(it, points, state.brushColor, state.brushWidth) }
    }

    fun placeText(text: String, x: Float, y: Float) {
        val state = editor.value ?: return
        mutate { PhotoImage.label(it, text, x, y, state.brushColor, state.brushWidth * 2.2f) }
    }

    fun mosaic(rect: Rect) = mutate { PhotoImage.mosaic(it, rect, 24) }

    fun blur(rect: Rect) {
        val radius = editor.value?.blurRadius ?: 8
        mutate { PhotoImage.blur(it, rect, radius) }
    }

    fun undo() {
        val state = editor.value ?: return
        val previous = history.undo(state.bitmap) ?: return
        editor.value = state.copy(bitmap = previous, canUndo = history.canUndo, canRedo = history.canRedo, dirty = true)
    }

    fun redo() {
        val state = editor.value ?: return
        val next = history.redo(state.bitmap) ?: return
        editor.value = state.copy(bitmap = next, canUndo = history.canUndo, canRedo = history.canRedo, dirty = true)
    }

    fun resetToOpened() {
        val state = editor.value ?: return
        viewModelScope.launch {
            val fresh = withContext(Dispatchers.IO) {
                repository.decode(state.id, preferEdited = !state.isNew)
            } ?: return@launch
            history.clear()
            editor.value = state.copy(bitmap = fresh, canUndo = false, canRedo = false, dirty = false)
        }
    }

    fun save(onDone: (Boolean) -> Unit) {
        val state = editor.value ?: return
        if (saving) return
        saving = true
        editor.value = state.copy(saving = true)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                if (state.isNew) {
                    repository.saveNew(state.id, state.bitmap, state.dateEpochDay, state.note)
                } else {
                    repository.saveEdit(state.id, state.bitmap, state.dateEpochDay, state.note)
                }
            }
            saving = false
            if (!ok) {
                editor.value = editor.value?.copy(saving = false)
                message.value = "保存失败，编辑还在"
                onDone(false)
                return@launch
            }
            if (advanceQueue()) {
                onDone(true)
            } else {
            releaseEditor()
            onDone(true)
            }
        }
    }

    fun skipOrCancel(onFinishedQueue: () -> Unit) {
        val state = editor.value ?: return
        if (state.isNew) repository.discardStaged(state.id)
        if (!advanceQueue()) {
            closeEditor(discardUnsaved = false)
            onFinishedQueue()
        }
    }

    fun closeEditor(discardUnsaved: Boolean) {
        val state = editor.value
        if (discardUnsaved && state?.isNew == true) repository.discardStaged(state.id)
        if (discardUnsaved && state?.isNew == true) {
            queue.drop(queueIndex + 1).forEach { repository.discardStaged(it) }
        }
        releaseEditor()
    }

    fun changeDate(id: String, epochDay: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateDate(id, epochDay) }
    }

    fun changeNote(id: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateNote(id, note) }
    }

    fun delete(id: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { repository.delete(id) } }.isSuccess
            if (!ok) message.value = "删除失败"
            onDone(ok)
        }
    }

    fun loadThumbnail(row: PhotoWallEntity): Bitmap? = repository.thumbnail(row.id, row.displayPath())

    fun loadFull(row: PhotoWallEntity): Bitmap? = repository.decode(row.id, preferEdited = true)

    private fun startQueue(ids: List<String>, onReady: (Boolean) -> Unit) {
        queue.clear()
        queue += ids
        queueIndex = 0
        history.clear()
        openQueueItem(isNew = true, onReady = onReady)
    }

    private fun openQueueItem(isNew: Boolean, onReady: ((Boolean) -> Unit)? = null) {
        val id = queue.getOrNull(queueIndex) ?: return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) { repository.decode(id, preferEdited = false) }
            if (bitmap == null) {
                repository.discardStaged(id)
                onReady?.invoke(false)
                if (!advanceQueue()) releaseEditor()
                return@launch
            }
            history.clear()
            editor.value = PhotoEditorUi(
                id = id,
                isNew = isNew,
                bitmap = bitmap,
                dateEpochDay = LocalDate.now().toEpochDay(),
                index = queueIndex + 1,
                total = queue.size,
            )
            onReady?.invoke(true)
        }
    }

    private fun advanceQueue(): Boolean {
        queueIndex += 1
        if (queueIndex >= queue.size) return false
        openQueueItem(isNew = true)
        return true
    }

    private fun releaseEditor() {
        history.clear()
        editor.value?.bitmap?.recycle()
        editor.value = null
        queue.clear()
    }

    private fun mutate(block: (Bitmap) -> Bitmap) {
        val state = editor.value ?: return
        if (state.saving) return
        viewModelScope.launch {
            edits.withLock {
                val current = editor.value ?: return@withLock
                if (current.saving) return@withLock
                val next = withContext(Dispatchers.Default) { runCatching { block(current.bitmap) }.getOrNull() }
                    ?: return@withLock
                history.push(current.bitmap)
                editor.value = current.copy(
                    bitmap = next,
                    canUndo = true,
                    canRedo = false,
                    dirty = true,
                )
            }
        }
    }

    override fun onCleared() {
        history.clear()
        editor.value?.bitmap?.recycle()
        super.onCleared()
    }

    companion object {
        fun factory(app: GymLogApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PhotoWallViewModel(app.photoWallRepository) as T
                }
            }
        }
    }
}
