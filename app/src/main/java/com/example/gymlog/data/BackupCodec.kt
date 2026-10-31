package com.example.gymlog.data

import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    const val FORMAT = "gymlog"
    const val VERSION = 2

    fun encode(snapshot: GymLogSnapshot, exportedAt: Long): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", exportedAt)
        root.put("exercises", JSONArray().apply {
            snapshot.exercises.forEach { put(it.toJson()) }
        })
        root.put("sessions", JSONArray().apply {
            snapshot.sessions.forEach { put(it.toJson()) }
        })
        root.put("sets", JSONArray().apply {
            snapshot.sets.forEach { put(it.toJson()) }
        })
        root.put("exerciseDurations", JSONArray().apply {
            snapshot.exerciseDurations.forEach { put(it.toJson()) }
        })
        val profileJson = snapshot.profile?.toJson(snapshot.avatarImage)
        if (profileJson != null) {
            root.put("profile", profileJson)
        } else {
            root.put("profile", JSONObject.NULL)
            writeAvatar(root, snapshot.avatarImage)
        }
        root.put("measurements", JSONArray().apply {
            snapshot.measurements.forEach { put(it.toJson()) }
        })
        root.put("legacyRecords", JSONArray().apply {
            snapshot.legacyRecords.forEach { put(it.toJson()) }
        })
        root.put("dayNotes", JSONArray().apply {
            snapshot.dayNotes.forEach { put(it.toJson()) }
        })
        root.put("dayKinds", JSONArray().apply {
            snapshot.dayKinds.forEach { put(it.toJson()) }
        })
        root.put("exerciseMuscles", JSONArray().apply {
            snapshot.exerciseMuscles.forEach { put(it.toJson()) }
        })
        val timerRecords = requireNotNull(snapshot.timerRecords) { "MISSING_TIMERS" }
        val timerLaps = requireNotNull(snapshot.timerLaps) { "MISSING_TIMERS" }
        require(snapshot.avatarImage != AvatarImageBackup.Unspecified) { "MISSING_AVATAR" }
        root.put("timerRecords", JSONArray().apply {
            timerRecords.forEach { put(it.toJson()) }
        })
        root.put("timerLaps", JSONArray().apply {
            timerLaps.forEach { put(it.toJson()) }
        })
        return root.toString()
    }

    fun decode(text: String): GymLogSnapshot {
        val root = JSONObject(text.trim().removePrefix("\uFEFF"))
        val format = root.optString("format")
        val version = root.optInt("version", -1)
        require(format == FORMAT && version in 1..VERSION) { "UNSUPPORTED" }
        val profileJson = if (root.isNull("profile")) null else root.getJSONObject("profile")
        val avatar = readAvatar(profileJson, root, version)
        val timers = readTimers(root, version)
        var profile = profileJson?.toProfile()
        profile = when (avatar) {
            AvatarImageBackup.Unspecified -> profile
            AvatarImageBackup.None -> profile?.copy(avatarFileName = null)
            is AvatarImageBackup.Jpeg -> (profile ?: BodyProfileEntity()).copy(
                avatarFileName = ProfileAvatarStore.FILE_NAME,
            )
        }
        val snapshot = GymLogSnapshot(
            exercises = root.getJSONArray("exercises").objects().map { it.toExercise() },
            sessions = root.getJSONArray("sessions").objects().map { it.toSession() },
            sets = root.getJSONArray("sets").objects().map { it.toSet() },
            exerciseDurations = root.optJSONArray("exerciseDurations")
                ?.objects()
                ?.map { it.toExerciseDuration() }
                .orEmpty(),
            profile = profile,
            measurements = root.getJSONArray("measurements").objects().map { it.toMeasurement() },
            legacyRecords = root.getJSONArray("legacyRecords").objects().map { it.toLegacy() },
            dayNotes = root.optJSONArray("dayNotes")?.objects()?.map { it.toDayNote() }.orEmpty(),
            dayKinds = root.optJSONArray("dayKinds")?.objects()?.map { it.toDayKind() }.orEmpty(),
            exerciseMuscles = root.optJSONArray("exerciseMuscles")?.objects()?.map { it.toMuscleLink() }.orEmpty(),
            timerRecords = timers.first,
            timerLaps = timers.second,
            avatarImage = avatar,
        )
        snapshot.validate()
        return snapshot
    }
}

private fun ExerciseEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("name", name)
        .put("bodyPart", bodyPart)
        .put("equipmentType", equipmentType)
        .put("weightMode", weightMode)
        .put("isCustom", isCustom)
        .putNullable("aliases", aliases)
        .put("createdAt", createdAt)
        .putNullable("workingPrWeightKg", workingPrWeightKg)
        .putNullable("workingPrReps", workingPrReps)
        .putNullable("workingPrWeightMode", workingPrWeightMode)
        .putNullable("workingPrDateEpochDay", workingPrDateEpochDay)
        .putNullable("maxPrWeightKg", maxPrWeightKg)
        .putNullable("maxPrReps", maxPrReps)
        .putNullable("maxPrWeightMode", maxPrWeightMode)
        .putNullable("maxPrDateEpochDay", maxPrDateEpochDay)
        .put("archived", archived)
        .putNullable("nameEn", nameEn)
        .put("source", source)
        .putNullable("sourceId", sourceId)
        .putNullable("equipmentDetail", equipmentDetail)
        .putNullable("bodyPartDetail", bodyPartDetail)
        .putNullable("bodyCategoryId", bodyCategoryId)
        .putNullable("equipmentCategoryId", equipmentCategoryId)
        .put("classificationStatus", classificationStatus)
        .put("classificationVersion", classificationVersion)
        // v20 新增字段。旧备份里没有这几项，读取时都会回落到默认值。
        .put("priority", priority)
        .put("isFavorite", isFavorite)
        .putNullable("howTo", howTo)
        .putNullable("cues", cues)
}

private fun JSONObject.toExercise(): ExerciseEntity {
    val isCustom = getBoolean("isCustom")
    return ExerciseEntity(
        id = getString("id"),
        name = getString("name"),
        bodyPart = getString("bodyPart"),
        equipmentType = getString("equipmentType"),
        weightMode = getString("weightMode"),
        isCustom = isCustom,
        aliases = optStringOrNull("aliases"),
        createdAt = getLong("createdAt"),
        workingPrWeightKg = optDoubleOrNull("workingPrWeightKg"),
        workingPrReps = optIntOrNull("workingPrReps"),
        workingPrWeightMode = optStringOrNull("workingPrWeightMode"),
        workingPrDateEpochDay = optLongOrNull("workingPrDateEpochDay"),
        maxPrWeightKg = optDoubleOrNull("maxPrWeightKg"),
        maxPrReps = optIntOrNull("maxPrReps"),
        maxPrWeightMode = optStringOrNull("maxPrWeightMode"),
        maxPrDateEpochDay = optLongOrNull("maxPrDateEpochDay"),
        archived = optBoolean("archived", false),
        nameEn = optStringOrNull("nameEn"),
        source = optString("source").takeIf { it.isNotBlank() }
            ?: if (isCustom) ExerciseSource.CUSTOM else ExerciseSource.LEGACY,
        sourceId = optStringOrNull("sourceId"),
        equipmentDetail = optStringOrNull("equipmentDetail"),
        bodyPartDetail = optStringOrNull("bodyPartDetail"),
        bodyCategoryId = optStringOrNull("bodyCategoryId"),
        equipmentCategoryId = optStringOrNull("equipmentCategoryId"),
        classificationStatus = optString("classificationStatus")
            .takeIf { it.isNotBlank() }
            ?: ExerciseClassification.STATUS_UNMIGRATED,
        classificationVersion = optInt("classificationVersion", 0),
        // v21 起优先级合法范围 0–10；旧备份里可能还有 0–100 的值，恢复时钳回合法区间
        priority = optInt("priority", ExercisePriority.DEFAULT).coerceIn(ExercisePriority.MIN, ExercisePriority.MAX),
        isFavorite = optBoolean("isFavorite", false),
        howTo = optStringOrNull("howTo"),
        cues = optStringOrNull("cues"),
    )
}

private fun MuscleLink.toJson(): JSONObject {
    return JSONObject()
        .put("exerciseId", exerciseId)
        .put("muscleId", muscleId)
        .put("role", role.name)
        .put("sortOrder", sortOrder)
}

private fun JSONObject.toMuscleLink(): MuscleLink {
    val roleName = getString("role")
    require(roleName == MuscleRole.PRIMARY.name || roleName == MuscleRole.SECONDARY.name) { "BAD_MUSCLE_ROLE" }
    return MuscleLink(
        exerciseId = getString("exerciseId"),
        muscleId = getString("muscleId"),
        role = MuscleRole.valueOf(roleName),
        sortOrder = optInt("sortOrder", 0),
    )
}

private fun WorkoutSessionEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("dateEpochDay", dateEpochDay)
        .putNullable("note", note)
        .put("createdAt", createdAt)
        .put("durationMillis", durationMillis)
}

private fun ExerciseDurationEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("sessionId", sessionId)
        .put("exerciseId", exerciseId)
        .put("exerciseOrder", exerciseOrder)
        .put("durationMillis", durationMillis)
        .put("createdAt", createdAt)
}

private fun JSONObject.toExerciseDuration(): ExerciseDurationEntity {
    return ExerciseDurationEntity(
        id = getString("id"),
        sessionId = getString("sessionId"),
        exerciseId = getString("exerciseId"),
        exerciseOrder = getInt("exerciseOrder"),
        durationMillis = getLong("durationMillis"),
        createdAt = getLong("createdAt"),
    )
}

private fun JSONObject.toSession(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = getString("id"),
        dateEpochDay = getLong("dateEpochDay"),
        note = optStringOrNull("note"),
        createdAt = getLong("createdAt"),
        durationMillis = optLong("durationMillis", 0L),
    )
}

private fun WorkoutSetEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("sessionId", sessionId)
        .put("exerciseId", exerciseId)
        .put("exerciseOrder", exerciseOrder)
        .put("setIndex", setIndex)
        .put("weightMode", weightMode)
        .putNullable("weightKg", weightKg)
        .putNullable("reps", reps)
        .putNullable("note", note)
}

private fun JSONObject.toSet(): WorkoutSetEntity {
    return WorkoutSetEntity(
        id = getString("id"),
        sessionId = getString("sessionId"),
        exerciseId = getString("exerciseId"),
        exerciseOrder = getInt("exerciseOrder"),
        setIndex = getInt("setIndex"),
        weightMode = getString("weightMode"),
        weightKg = optDoubleOrNull("weightKg"),
        reps = optIntOrNull("reps"),
        note = optStringOrNull("note"),
    )
}

private fun BodyProfileEntity.toJson(avatar: AvatarImageBackup): JSONObject {
    val json = JSONObject()
        .putNullable("birthdayEpochDay", birthdayEpochDay)
        .putNullable("gender", gender)
        .putNullable("displayName", displayName)
        .putNullable("avatarFileName", avatarFileName)
    writeAvatar(json, avatar)
    return json
}

private fun JSONObject.toProfile(): BodyProfileEntity {
    val gender = optStringOrNull("gender")
    require(gender == null || Gender.fromStored(gender) != null) { "BAD_GENDER" }
    return BodyProfileEntity(
        birthdayEpochDay = optLongOrNull("birthdayEpochDay"),
        gender = gender,
        displayName = optStringOrNull("displayName"),
        avatarFileName = optStringOrNull("avatarFileName"),
    )
}

private fun BodyMeasurementEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("recordedEpochDay", recordedEpochDay)
        .putNullable("heightCm", heightCm)
        .putNullable("weightKg", weightKg)
        .putNullable("bodyFatPercent", bodyFatPercent)
        .putNullable("waistCm", waistCm)
        .putNullable("armLeftCm", armLeftCm)
        .putNullable("armRightCm", armRightCm)
        .putNullable("forearmLeftCm", forearmLeftCm)
        .putNullable("forearmRightCm", forearmRightCm)
        .putNullable("chestCm", chestCm)
        .putNullable("hipCm", hipCm)
        .putNullable("thighLeftCm", thighLeftCm)
        .putNullable("thighRightCm", thighRightCm)
        .putNullable("calfLeftCm", calfLeftCm)
        .putNullable("calfRightCm", calfRightCm)
        .put("createdAt", createdAt)
}

private fun JSONObject.toMeasurement(): BodyMeasurementEntity {
    return BodyMeasurementEntity(
        id = getString("id"),
        recordedEpochDay = getLong("recordedEpochDay"),
        heightCm = optDoubleOrNull("heightCm"),
        weightKg = optDoubleOrNull("weightKg"),
        bodyFatPercent = optDoubleOrNull("bodyFatPercent"),
        waistCm = optDoubleOrNull("waistCm"),
        armLeftCm = optDoubleOrNull("armLeftCm"),
        armRightCm = optDoubleOrNull("armRightCm"),
        forearmLeftCm = optDoubleOrNull("forearmLeftCm"),
        forearmRightCm = optDoubleOrNull("forearmRightCm"),
        chestCm = optDoubleOrNull("chestCm"),
        hipCm = optDoubleOrNull("hipCm"),
        thighLeftCm = optDoubleOrNull("thighLeftCm"),
        thighRightCm = optDoubleOrNull("thighRightCm"),
        calfLeftCm = optDoubleOrNull("calfLeftCm"),
        calfRightCm = optDoubleOrNull("calfRightCm"),
        createdAt = getLong("createdAt"),
    )
}

private fun WorkoutRecord.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("dateEpochDay", dateEpochDay)
        .put("exerciseName", exerciseName)
        .put("weightKg", weightKg)
        .put("reps", reps)
        .put("sets", sets)
        .put("note", note)
}

private fun JSONObject.toLegacy(): WorkoutRecord {
    return WorkoutRecord(
        id = getLong("id"),
        dateEpochDay = getLong("dateEpochDay"),
        exerciseName = getString("exerciseName"),
        weightKg = getDouble("weightKg"),
        reps = getInt("reps"),
        sets = getInt("sets"),
        note = getString("note"),
    )
}

private fun DayNoteEntity.toJson(): JSONObject {
    return JSONObject()
        .put("dateEpochDay", dateEpochDay)
        .put("message", message)
}

private fun JSONObject.toDayNote(): DayNoteEntity {
    return DayNoteEntity(
        dateEpochDay = getLong("dateEpochDay"),
        message = getString("message"),
    )
}

private fun DayKindEntity.toJson(): JSONObject {
    return JSONObject()
        .put("dateEpochDay", dateEpochDay)
        .put("kind", kind)
}

private fun JSONObject.toDayKind(): DayKindEntity {
    val kind = getString("kind")
    require(DayKind.fromStored(kind) != null) { "BAD_DAY_KIND" }
    return DayKindEntity(
        dateEpochDay = getLong("dateEpochDay"),
        kind = kind,
    )
}

private fun TimerRecordEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("durationMillis", durationMillis)
        .put("dateEpochDay", dateEpochDay)
        .putNullable("sessionId", sessionId)
        .put("createdAt", createdAt)
}

private fun JSONObject.toTimerRecord(): TimerRecordEntity {
    return TimerRecordEntity(
        id = getString("id"),
        durationMillis = getLong("durationMillis"),
        dateEpochDay = getLong("dateEpochDay"),
        sessionId = optStringOrNull("sessionId")?.takeIf { it.isNotBlank() },
        createdAt = getLong("createdAt"),
    )
}

private fun TimerLapEntity.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("recordId", recordId)
        .put("lapIndex", lapIndex)
        .put("intervalMillis", intervalMillis)
        .put("elapsedMillis", elapsedMillis)
}

private fun JSONObject.toTimerLap(): TimerLapEntity {
    return TimerLapEntity(
        id = getString("id"),
        recordId = getString("recordId"),
        lapIndex = getInt("lapIndex"),
        intervalMillis = getLong("intervalMillis"),
        elapsedMillis = getLong("elapsedMillis"),
    )
}

private fun writeAvatar(json: JSONObject, avatar: AvatarImageBackup) {
    when (avatar) {
        AvatarImageBackup.Unspecified -> Unit
        AvatarImageBackup.None -> json.put("avatarJpeg", JSONObject.NULL)
        is AvatarImageBackup.Jpeg -> json.put(
            "avatarJpeg",
            Base64.encodeToString(avatar.bytes, Base64.NO_WRAP),
        )
    }
}

private fun readAvatar(
    profileJson: JSONObject?,
    root: JSONObject,
    version: Int,
): AvatarImageBackup {
    val source = when {
        profileJson != null && profileJson.has("avatarJpeg") -> profileJson
        root.has("avatarJpeg") -> root
        else -> null
    }
    if (version >= 2) require(source != null) { "MISSING_AVATAR" }
    if (source == null) return AvatarImageBackup.Unspecified
    if (source.isNull("avatarJpeg")) return AvatarImageBackup.None
    return AvatarImageBackup.Jpeg(decodeAvatarBytes(source.getString("avatarJpeg")))
}

private fun readTimers(
    root: JSONObject,
    version: Int,
): Pair<List<TimerRecordEntity>?, List<TimerLapEntity>?> {
    val hasRecords = root.has("timerRecords")
    val hasLaps = root.has("timerLaps")
    if (version >= 2) {
        require(hasRecords && hasLaps && !root.isNull("timerRecords") && !root.isNull("timerLaps")) {
            "MISSING_TIMERS"
        }
    }
    if (!hasRecords && !hasLaps) return null to null
    require(hasRecords && hasLaps && !root.isNull("timerRecords") && !root.isNull("timerLaps")) {
        "INCOMPLETE_TIMERS"
    }
    return root.getJSONArray("timerRecords").objects().map { it.toTimerRecord() } to
        root.getJSONArray("timerLaps").objects().map { it.toTimerLap() }
}

private fun decodeAvatarBytes(value: String): ByteArray {
    val bytes = try {
        Base64.decode(value, Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("BAD_AVATAR")
    }
    validateAvatarBytes(bytes)
    return bytes
}

internal const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
internal const val MAX_AVATAR_EDGE = 4096

internal fun validateAvatarBytes(bytes: ByteArray) {
    require(bytes.size in 4..MAX_AVATAR_BYTES && isJpeg(bytes)) { "BAD_AVATAR" }
    val bounds = try {
        BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { options ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }
    } catch (_: Throwable) {
        throw IllegalArgumentException("BAD_AVATAR")
    }
    require(bounds.outWidth in 1..MAX_AVATAR_EDGE && bounds.outHeight in 1..MAX_AVATAR_EDGE) {
        "BAD_AVATAR"
    }
}

private fun isJpeg(bytes: ByteArray): Boolean {
    return bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
}

private fun JSONObject.putNullable(key: String, value: Any?): JSONObject {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
    return this
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return getString(key)
}

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return getDouble(key)
}

private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return getInt(key)
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return getLong(key)
}

private fun JSONArray.objects(): List<JSONObject> {
    return List(length()) { index -> getJSONObject(index) }
}
