package com.example.gymlog.data

sealed interface AvatarImageBackup {
    data object Unspecified : AvatarImageBackup

    data object None : AvatarImageBackup

    class Jpeg(val bytes: ByteArray) : AvatarImageBackup {
        override fun equals(other: Any?): Boolean {
            return other is Jpeg && bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }
}

data class GymLogSnapshot(
    val exercises: List<ExerciseEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<WorkoutSetEntity>,
    val profile: BodyProfileEntity?,
    val measurements: List<BodyMeasurementEntity>,
    val legacyRecords: List<WorkoutRecord>,
    val dayNotes: List<DayNoteEntity> = emptyList(),
    val dayKinds: List<DayKindEntity> = emptyList(),
    val exerciseMuscles: List<MuscleLink> = emptyList(),
    val timerRecords: List<TimerRecordEntity>? = null,
    val timerLaps: List<TimerLapEntity>? = null,
    val avatarImage: AvatarImageBackup = AvatarImageBackup.Unspecified,
)

fun GymLogSnapshot.validate() {
    val exerciseIds = exercises.map { it.id }
    require(exerciseIds.size == exerciseIds.toSet().size) { "DUPLICATE_EXERCISE" }
    val sessionIds = sessions.map { it.id }
    require(sessionIds.size == sessionIds.toSet().size) { "DUPLICATE_SESSION" }
    val exerciseIdSet = exerciseIds.toSet()
    val sessionIdSet = sessionIds.toSet()
    val setIds = sets.map { it.id }
    require(setIds.size == setIds.toSet().size) { "DUPLICATE_SET" }
    require(sets.all { it.id.isNotBlank() }) { "BAD_SET_ID" }
    sets.forEach { set ->
        require(set.sessionId in sessionIdSet) { "MISSING_SESSION" }
        require(set.exerciseId in exerciseIdSet) { "MISSING_EXERCISE" }
    }
    val measurementIds = measurements.map { it.id }
    require(measurementIds.size == measurementIds.toSet().size) { "DUPLICATE_MEASUREMENT" }
    val legacyIds = legacyRecords.map { it.id }
    require(legacyIds.size == legacyIds.toSet().size) { "DUPLICATE_LEGACY" }
    val noteDays = dayNotes.map { it.dateEpochDay }
    require(noteDays.size == noteDays.toSet().size) { "DUPLICATE_DAY_NOTE" }
    require(dayNotes.all { it.message.isNotBlank() }) { "BLANK_DAY_NOTE" }
    val kindDays = dayKinds.map { it.dateEpochDay }
    require(kindDays.size == kindDays.toSet().size) { "DUPLICATE_DAY_KIND" }
    require(dayKinds.all { DayKind.fromStored(it.kind) != null }) { "BAD_DAY_KIND" }
    val gender = profile?.gender
    require(gender == null || Gender.fromStored(gender) != null) { "BAD_GENDER" }
    MuscleMapLogic.validateLinks(exerciseIds.toSet(), exerciseMuscles)
    validateTimers()
    val avatar = avatarImage
    if (avatar is AvatarImageBackup.Jpeg) validateAvatarBytes(avatar.bytes)
}

private fun GymLogSnapshot.validateTimers() {
    val records = timerRecords
    val laps = timerLaps
    require((records == null) == (laps == null)) { "INCOMPLETE_TIMERS" }
    if (records == null || laps == null) return
    require(records.all { it.id.isNotBlank() }) { "BAD_TIMER_ID" }
    val recordIds = records.map { it.id }
    require(recordIds.size == recordIds.toSet().size) { "DUPLICATE_TIMER" }
    require(laps.all { it.id.isNotBlank() }) { "BAD_LAP_ID" }
    val lapIds = laps.map { it.id }
    require(lapIds.size == lapIds.toSet().size) { "DUPLICATE_LAP" }
    val recordIdSet = recordIds.toSet()
    laps.forEach { lap ->
        require(lap.recordId in recordIdSet) { "MISSING_TIMER" }
    }
    val lapKeys = laps.map { it.recordId to it.lapIndex }
    require(lapKeys.size == lapKeys.toSet().size) { "DUPLICATE_LAP_INDEX" }
}
