package com.example.gymlog.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DraftSetSnapshot(
    val id: String,
    val mode: WeightMode,
    val normalKgText: String,
    val extraKgText: String,
    val repsText: String,
    val completed: Boolean,
)

data class DraftExerciseSnapshot(
    val id: String,
    val exerciseId: String,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
    val durationMillis: Long?,
    val sets: List<DraftSetSnapshot>,
)

data class SessionDraftSnapshot(
    val draftKey: String,
    val sessionId: String?,
    val dateEpochDay: Long,
    val note: String,
    val totalElapsedMillis: Long,
    val timerRunning: Boolean,
    val savedAtEpochMillis: Long,
    val finishing: Boolean,
    val finishedDurationMillis: Long,
    val photoId: String?,
    val exercises: List<DraftExerciseSnapshot>,
)

/**
 * A single, versioned JSON snapshot of the active workout draft.
 *
 * This is intentionally not part of the Room schema or the backup format. It only bridges
 * process death for the one draft that may currently be in progress.
 */
class SessionDraftStore(private val directory: File) {
    private val file = File(directory, FILE_NAME)
    private val tempFile = File(directory, "$FILE_NAME.tmp")
    private val mutex = Mutex()

    fun load(): SessionDraftSnapshot? {
        if (!file.exists()) return null
        return runCatching {
            val root = JSONObject(file.readText())
            require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION)
            SessionDraftSnapshot(
                draftKey = root.getString("draftKey"),
                sessionId = root.optStringOrNull("sessionId"),
                dateEpochDay = root.getLong("dateEpochDay"),
                note = root.optString("note"),
                totalElapsedMillis = root.optLong("totalElapsedMillis", 0L),
                timerRunning = root.optBoolean("timerRunning", false),
                savedAtEpochMillis = root.optLong("savedAtEpochMillis", 0L),
                finishing = root.optBoolean("finishing", false),
                finishedDurationMillis = root.optLong("finishedDurationMillis", 0L),
                photoId = root.optStringOrNull("photoId"),
                exercises = root.optJSONArray("exercises")?.objects()
                    ?.map { it.toExerciseSnapshot() }
                    .orEmpty(),
            )
        }.getOrNull()
    }

    suspend fun save(snapshot: SessionDraftSnapshot) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                directory.mkdirs()
                val text = snapshot.toJson().toString()
                FileOutputStream(tempFile).use { stream ->
                    stream.write(text.toByteArray(Charsets.UTF_8))
                    stream.fd.sync()
                }
                if (!tempFile.renameTo(file)) {
                    runCatching {
                        Files.move(
                            tempFile.toPath(),
                            file.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE,
                        )
                    }.onFailure {
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                    }
                }
            }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                file.delete()
                tempFile.delete()
            }
        }
    }

    companion object {
        const val FILE_NAME = "session_draft.json"
        const val SCHEMA_VERSION = 1
    }
}

private fun SessionDraftSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put("schemaVersion", SessionDraftStore.SCHEMA_VERSION)
        .put("draftKey", draftKey)
        .putNullable("sessionId", sessionId)
        .put("dateEpochDay", dateEpochDay)
        .put("note", note)
        .put("totalElapsedMillis", totalElapsedMillis)
        .put("timerRunning", timerRunning)
        .put("savedAtEpochMillis", savedAtEpochMillis)
        .put("finishing", finishing)
        .put("finishedDurationMillis", finishedDurationMillis)
        .putNullable("photoId", photoId)
        .put("exercises", JSONArray().apply {
            exercises.forEach { put(it.toJson()) }
        })
}

private fun DraftExerciseSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("exerciseId", exerciseId)
        .putNullable("startedAtMillis", startedAtMillis)
        .putNullable("completedAtMillis", completedAtMillis)
        .putNullable("durationMillis", durationMillis)
        .put("sets", JSONArray().apply {
            sets.forEach { put(it.toJson()) }
        })
}

private fun DraftSetSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("mode", mode.name)
        .put("normalKgText", normalKgText)
        .put("extraKgText", extraKgText)
        .put("repsText", repsText)
        .put("completed", completed)
}

private fun JSONObject.toExerciseSnapshot(): DraftExerciseSnapshot {
    return DraftExerciseSnapshot(
        id = getString("id"),
        exerciseId = getString("exerciseId"),
        startedAtMillis = optLongOrNull("startedAtMillis"),
        completedAtMillis = optLongOrNull("completedAtMillis"),
        durationMillis = optLongOrNull("durationMillis"),
        sets = optJSONArray("sets")?.objects()?.map { it.toSetSnapshot() }.orEmpty(),
    )
}

private fun JSONObject.toSetSnapshot(): DraftSetSnapshot {
    return DraftSetSnapshot(
        id = getString("id"),
        mode = WeightMode.valueOf(getString("mode")),
        normalKgText = optString("normalKgText"),
        extraKgText = optString("extraKgText"),
        repsText = optString("repsText"),
        completed = optBoolean("completed", false),
    )
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    return put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotEmpty() }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (isNull(key) || !has(key)) return null
    return optLong(key)
}

private fun JSONArray.objects(): List<JSONObject> {
    return (0 until length()).mapNotNull { index -> optJSONObject(index) }
}
