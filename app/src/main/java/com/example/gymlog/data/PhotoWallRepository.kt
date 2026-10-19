package com.example.gymlog.data

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class PhotoWallRepository(
    private val dao: PhotoWallDao,
    private val store: PhotoStore,
) {
    private val gate = Mutex()

    fun observe() = dao.observeAll()

    suspend fun get(id: String) = dao.get(id)

    suspend fun reconcile() {
        val referenced = HashSet<String>()
        dao.allPaths().forEach { row ->
            referenced += row.originalRelativePath
            row.editedRelativePath?.let { referenced += it }
        }
        store.deleteOrphans(referenced)
    }

    /** Copies a picker URI into private storage. No database row until the user saves. */
    suspend fun stageImport(uri: Uri): String? = gate.withLock {
        val id = newId()
        store.importUri(uri, id)?.let { id }
    }

    suspend fun stageCapture(): String? = gate.withLock {
        val id = newId()
        store.importCapture(id)?.let { id }
    }

    fun discardStaged(id: String) {
        store.deleteRelative(PhotoPaths.original(id))
        store.deleteThumbnail(id)
    }

    fun decode(id: String, preferEdited: Boolean): Bitmap? {
        val edited = PhotoPaths.edited(id)
        val original = PhotoPaths.original(id)
        val path = if (preferEdited && store.fileFor(edited)?.isFile == true) edited else original
        return store.decodeDisplay(path)
    }

    suspend fun saveNew(
        id: String,
        bitmap: Bitmap,
        dateEpochDay: Long,
        note: String,
    ): Boolean = gate.withLock {
        if (dao.get(id) != null) return false
        val original = PhotoPaths.original(id)
        if (store.fileFor(original)?.isFile != true) return false
        val edited = PhotoPaths.edited(id)
        if (!store.writeJpeg(edited, bitmap)) return false
        val cleanNote = note.trim().take(PhotoImage.NOTE_LIMIT)
        dao.insert(
            PhotoWallEntity(
                id = id,
                originalRelativePath = original,
                editedRelativePath = edited,
                dateEpochDay = dateEpochDay,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                note = cleanNote,
                width = bitmap.width,
                height = bitmap.height,
            ),
        )
        store.deleteThumbnail(id)
        true
    }

    suspend fun saveEdit(
        id: String,
        bitmap: Bitmap,
        dateEpochDay: Long,
        note: String,
    ): Boolean = gate.withLock {
        if (dao.get(id) == null) return false
        val edited = PhotoPaths.edited(id)
        if (!store.writeJpeg(edited, bitmap)) return false
        dao.updateSaved(
            id = id,
            edited = edited,
            dateEpochDay = dateEpochDay,
            note = note.trim().take(PhotoImage.NOTE_LIMIT),
            width = bitmap.width,
            height = bitmap.height,
            updatedAt = System.currentTimeMillis(),
        )
        store.deleteThumbnail(id)
        true
    }

    suspend fun updateDate(id: String, dateEpochDay: Long) {
        dao.updateDate(id, dateEpochDay, System.currentTimeMillis())
    }

    suspend fun updateNote(id: String, note: String) {
        dao.updateNote(id, note.trim().take(PhotoImage.NOTE_LIMIT), System.currentTimeMillis())
    }

    suspend fun delete(id: String) = gate.withLock {
        val row = dao.get(id) ?: return@withLock
        dao.delete(id)
        if (dao.countUsing(row.originalRelativePath) == 0) store.deleteRelative(row.originalRelativePath)
        row.editedRelativePath?.let { path ->
            if (dao.countUsing(path) == 0) store.deleteRelative(path)
        }
        store.deleteThumbnail(id)
    }

    fun thumbnail(id: String, displayPath: String?) = store.thumbnail(id, displayPath)

    fun captureUri() = store.captureUri()

    fun discardCapture() = store.discardCapture()

    companion object {
        fun newId(): String = UUID.randomUUID().toString().replace("-", "")

        fun sort(rows: List<PhotoWallEntity>): List<PhotoWallEntity> {
            return rows.sortedWith(
                compareByDescending<PhotoWallEntity> { it.dateEpochDay }
                    .thenByDescending { it.createdAt }
                    .thenByDescending { it.id },
            )
        }

        fun inMonth(rows: List<PhotoWallEntity>, month: YearMonth?): List<PhotoWallEntity> {
            val sorted = sort(rows)
            if (month == null) return sorted
            return sorted.filter { row ->
                val date = LocalDate.ofEpochDay(row.dateEpochDay)
                YearMonth.from(date) == month
            }
        }
    }
}
