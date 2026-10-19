package com.example.gymlog

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.gymlog.data.AvatarImageBackup
import com.example.gymlog.data.BackupCodec
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindEntity
import com.example.gymlog.data.DayNoteEntity
import com.example.gymlog.data.BodyPart
import com.example.gymlog.data.BodyProfileEntity
import com.example.gymlog.data.EquipmentType
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMuscleEntity
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.Gender
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.GymLogSnapshot
import com.example.gymlog.data.MineStats
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.data.ProfileAvatarStore
import com.example.gymlog.data.TimerLapEntity
import com.example.gymlog.data.TimerRecordEntity
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.WorkoutRecord
import com.example.gymlog.data.WorkoutRepository
import com.example.gymlog.data.WorkoutSessionEntity
import com.example.gymlog.data.WorkoutSetEntity
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRoundTripTest {
    private lateinit var db: GymLogDatabase
    private lateinit var repository: BackupRepository
    private lateinit var avatarStore: ProfileAvatarStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val exercises = ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao())
        val workouts = WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao())
        avatarStore = ProfileAvatarStore(context)
        repository = BackupRepository(db, db.backupDao(), exercises, workouts, avatarStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportImportReplacesCurrentDataAndKeepsWaistAndArm() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "备注", createdAt = 3L),
            listOf(
                WorkoutSetEntity(
                    id = "set1",
                    sessionId = "s1",
                    exerciseId = "curl",
                    exerciseOrder = 0,
                    setIndex = 1,
                    weightMode = WeightMode.WEIGHTED.name,
                    weightKg = 100.0,
                    reps = 1,
                    note = null,
                ),
            ),
        )
        db.bodyRecordDao().upsertProfile(BodyProfileEntity(birthdayEpochDay = 10_000))
        db.bodyRecordDao().insertMeasurement(
            BodyMeasurementEntity(
                id = "m1",
                recordedEpochDay = 20_000,
                heightCm = 175.0,
                weightKg = 70.0,
                bodyFatPercent = 15.0,
                waistCm = 80.5,
                armCm = 32.0,
                createdAt = 4L,
            ),
        )
        db.workoutDao().insert(
            WorkoutRecord(
                id = 7,
                dateEpochDay = 19_000,
                exerciseName = "旧记录",
                weightKg = 40.0,
                reps = 8,
                sets = 3,
                note = "",
            ),
        )

        val original = repository.exportSnapshot()
        val restored = BackupCodec.decode(BackupCodec.encode(original, exportedAt = 9L))
        assertEquals(original, restored)

        repository.importSnapshot(
            GymLogSnapshot(
                exercises = emptyList(),
                sessions = emptyList(),
                sets = emptyList(),
                profile = null,
                measurements = emptyList(),
                legacyRecords = emptyList(),
            ),
        )
        assertTrue(repository.exportSnapshot().sessions.isEmpty())
        assertTrue(repository.exportSnapshot().measurements.isEmpty())

        repository.importSnapshot(restored)
        val after = repository.exportSnapshot()
        val curl = after.exercises.first { it.id == "curl" }
        assertEquals(80.5, after.measurements.single().waistCm!!, 0.001)
        assertEquals(32.0, after.measurements.single().armCm!!, 0.001)
        assertEquals(10_000L, after.profile?.birthdayEpochDay)
        assertEquals("备注", after.sessions.single().note)
        assertEquals(7L, after.legacyRecords.single().id)
        assertEquals(100.0, curl.maxPrWeightKg!!, 0.001)
        assertEquals(1, curl.maxPrReps)
        assertNull(curl.workingPrReps)
        assertEquals(1, after.exercises.size)
        assertTrue(after.exercises.none { it.id.startsWith("preset_") })
    }

    @Test
    fun invalidBackupDoesNotReplaceCurrentData() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 1L),
            listOf(
                WorkoutSetEntity(
                    id = "set1",
                    sessionId = "s1",
                    exerciseId = "curl",
                    exerciseOrder = 0,
                    setIndex = 1,
                    weightMode = WeightMode.BODYWEIGHT.name,
                    weightKg = null,
                    reps = 8,
                    note = null,
                ),
            ),
        )
        val current = repository.exportSnapshot()
        val broken = current.copy(
            sets = current.sets.map { it.copy(exerciseId = "missing") },
        )
        var rejected = false
        try {
            repository.importSnapshot(broken)
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals("s1", repository.exportSnapshot().sessions.single().id)
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("""{"format":"other","version":1}""")
        }
    }

    @Test
    fun clearLocalDataRemovesUserDataAndDoesNotSeedLegacyPresets() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "备注", createdAt = 3L),
            listOf(
                WorkoutSetEntity(
                    id = "set1",
                    sessionId = "s1",
                    exerciseId = "curl",
                    exerciseOrder = 0,
                    setIndex = 1,
                    weightMode = WeightMode.WEIGHTED.name,
                    weightKg = 20.0,
                    reps = 8,
                    note = null,
                ),
            ),
        )
        db.bodyRecordDao().upsertProfile(BodyProfileEntity(birthdayEpochDay = 10_000))
        db.bodyRecordDao().insertMeasurement(
            BodyMeasurementEntity(
                id = "m1",
                recordedEpochDay = 20_000,
                heightCm = 175.0,
                weightKg = 70.0,
                bodyFatPercent = null,
                waistCm = null,
                armCm = null,
                createdAt = 4L,
            ),
        )
        db.workoutDao().insert(
            WorkoutRecord(
                dateEpochDay = 19_000,
                exerciseName = "旧记录",
                weightKg = 40.0,
                reps = 8,
                sets = 3,
                note = "",
            ),
        )

        repository.clearLocalData()

        val after = repository.exportSnapshot()
        assertTrue(after.sessions.isEmpty())
        assertTrue(after.sets.isEmpty())
        assertTrue(after.measurements.isEmpty())
        assertTrue(after.legacyRecords.isEmpty())
        assertNull(after.profile)
        assertTrue(after.exercises.none { it.id == "curl" })
        assertTrue(after.exercises.none { it.id.startsWith("preset_") })
        assertEquals(1, repository.dataEpoch.value)
    }

    @Test
    fun dayNoteSurvivesBackupAndClearRemovesIt() = runTest {
        db.dayNoteDao().upsert(DayNoteEntity(dateEpochDay = 20_000, message = "休息"))
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        assertEquals("休息", BackupCodec.decode(encoded).dayNotes.single().message)

        val withoutNotes = JSONObject(encoded)
        withoutNotes.remove("dayNotes")
        assertTrue(BackupCodec.decode(withoutNotes.toString()).dayNotes.isEmpty())

        repository.clearLocalData()
        assertTrue(repository.exportSnapshot().dayNotes.isEmpty())
    }

    @Test
    fun dayKindSurvivesBackupAndOldFileOmitsIt() = runTest {
        db.dayKindDao().upsert(DayKindEntity(dateEpochDay = 20_000, kind = DayKind.INDULGE.name))
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        assertEquals(DayKind.INDULGE.name, BackupCodec.decode(encoded).dayKinds.single().kind)

        val withoutKinds = JSONObject(encoded)
        withoutKinds.remove("dayKinds")
        assertTrue(BackupCodec.decode(withoutKinds.toString()).dayKinds.isEmpty())

        val broken = JSONObject(encoded)
        broken.getJSONArray("dayKinds").getJSONObject(0).put("kind", "NAP")
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(broken.toString())
        }

        repository.clearLocalData()
        assertTrue(repository.exportSnapshot().dayKinds.isEmpty())
    }

    @Test
    fun genderSurvivesBackupAndOldFileStillImports() = runTest {
        db.bodyRecordDao().upsertProfile(
            BodyProfileEntity(birthdayEpochDay = 10_000, gender = Gender.FEMALE.name),
        )
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        assertEquals(Gender.FEMALE.name, BackupCodec.decode(encoded).profile?.gender)
        assertEquals(10_000L, BackupCodec.decode(encoded).profile?.birthdayEpochDay)

        val withoutGender = JSONObject(encoded)
        withoutGender.getJSONObject("profile").remove("gender")
        val decoded = BackupCodec.decode(withoutGender.toString())
        assertEquals(10_000L, decoded.profile?.birthdayEpochDay)
        assertNull(decoded.profile?.gender)

        val broken = JSONObject(encoded)
        broken.getJSONObject("profile").put("gender", "unknown")
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(broken.toString())
        }
    }

    @Test
    fun oldBackupWithoutMuscleLinksImports() = runTest {
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        val withoutMuscles = JSONObject(encoded)
        withoutMuscles.remove("exerciseMuscles")
        val decoded = BackupCodec.decode(withoutMuscles.toString())
        assertTrue(decoded.exerciseMuscles.isEmpty())
        repository.importSnapshot(decoded)
    }

    @Test
    fun newBackupKeepsCustomMuscleLink() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.exerciseMuscleDao().insertAll(
            listOf(ExerciseMuscleEntity("curl", "biceps_brachii", MuscleRole.PRIMARY.name, 0)),
        )
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        assertTrue(encoded.contains("biceps_brachii"))
        repository.importSnapshot(BackupCodec.decode(encoded))
        val restored = repository.exportSnapshot().exerciseMuscles
        assertTrue(restored.any { it.exerciseId == "curl" && it.muscleId == "biceps_brachii" && it.role == MuscleRole.PRIMARY })
    }

    @Test
    fun invalidMuscleLinkDoesNotReplaceCurrentData() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 1L),
            emptyList(),
        )
        val current = repository.exportSnapshot()
        val broken = current.copy(
            exerciseMuscles = current.exerciseMuscles + MuscleLink("curl", "not_a_muscle", MuscleRole.PRIMARY, 0),
        )
        var rejected = false
        try {
            repository.importSnapshot(broken)
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals("s1", repository.exportSnapshot().sessions.single().id)
        val duplicate = current.copy(
            exerciseMuscles = listOf(
                MuscleLink("curl", "biceps_brachii", MuscleRole.PRIMARY, 0),
                MuscleLink("curl", "biceps_brachii", MuscleRole.SECONDARY, 1),
            ),
        )
        rejected = false
        try {
            repository.importSnapshot(duplicate)
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun version2RestoresEveryTimerFieldAndSameDayDurations() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 3L),
            emptyList(),
        )
        db.timerRecordDao().insert(
            timer("early", 20_000, 1_500, "s1", 10L),
            listOf(lap("early", 1, 400, 400), lap("early", 2, 1_100, 1_500)),
        )
        db.timerRecordDao().insert(timer("later", 20_000, 2_500, null, 11L), emptyList())
        db.timerRecordDao().insert(timer("next-day", 20_001, 4_000, null, 12L), emptyList())

        val original = repository.exportSnapshot()
        val restored = BackupCodec.decode(BackupCodec.encode(original, exportedAt = 9L))
        repository.importSnapshot(restored)
        repository.importSnapshot(restored)

        val records = db.backupDao().allTimerRecords().sortedBy { it.id }
        val laps = db.backupDao().allTimerLaps().sortedBy { it.id }
        assertEquals(original.timerRecords?.sortedBy { it.id }, records)
        assertEquals(original.timerLaps?.sortedBy { it.id }, laps)
        val sameDay = records.filter { it.dateEpochDay == 20_000L }
        assertEquals(listOf(1_500L, 2_500L), sameDay.sortedBy { it.createdAt }.map { it.durationMillis })
        assertEquals(4_000L, MineStats.totalTimerMillis(sameDay))
        assertEquals(8_000L, MineStats.totalTimerMillis(records))
        assertEquals("s1", records.first { it.id == "early" }.sessionId)
        assertEquals(2, laps.count { it.recordId == "early" })
        assertEquals(3, db.backupDao().allTimerRecords().size)
    }

    @Test
    fun version1WithoutTimerFieldsKeepsLocalTimers() = runTest {
        db.timerRecordDao().insert(timer("local", 20_000, 5_000, null, 8L), listOf(lap("local", 1, 5_000, 5_000)))
        val version1 = asVersion1(BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L))
        val decoded = BackupCodec.decode(version1)
        assertNull(decoded.timerRecords)
        assertNull(decoded.timerLaps)
        assertTrue(decoded.avatarImage is AvatarImageBackup.Unspecified)
        repository.importSnapshot(decoded.copy(sessions = emptyList(), sets = emptyList()))
        assertEquals("local", db.backupDao().allTimerRecords().single().id)
        assertEquals(5_000L, db.backupDao().allTimerRecords().single().durationMillis)
        assertEquals("local", db.backupDao().allTimerLaps().single().recordId)
        assertTrue(db.backupDao().allSessions().isEmpty())
    }

    @Test
    fun version2EmptyTimerArraysClearSavedTimers() = runTest {
        db.timerRecordDao().insert(timer("local", 20_000, 5_000, null, 8L), listOf(lap("local", 1, 5_000, 5_000)))
        val cleared = repository.exportSnapshot().copy(timerRecords = emptyList(), timerLaps = emptyList())
        repository.importSnapshot(cleared)
        assertTrue(db.backupDao().allTimerRecords().isEmpty())
        assertTrue(db.backupDao().allTimerLaps().isEmpty())
    }

    @Test
    fun avatarBytesRoundTripAndOldBackupKeepsTheLocalFile() = runTest {
        val portrait = jpeg(android.graphics.Color.RED)
        avatarStore.write(portrait)
        db.bodyRecordDao().upsertProfile(
            BodyProfileEntity(displayName = "啦啦啦", avatarFileName = ProfileAvatarStore.FILE_NAME),
        )
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        val decoded = BackupCodec.decode(encoded)
        val saved = decoded.avatarImage as AvatarImageBackup.Jpeg
        assertTrue(portrait.contentEquals(saved.bytes))

        avatarStore.write(jpeg(android.graphics.Color.BLUE))
        db.bodyRecordDao().upsertProfile(
            BodyProfileEntity(displayName = "别人", avatarFileName = ProfileAvatarStore.FILE_NAME),
        )
        repository.importSnapshot(decoded)
        assertTrue(portrait.contentEquals(avatarStore.readBytes()))
        assertEquals("啦啦啦", db.backupDao().profile()?.displayName)
        assertEquals(ProfileAvatarStore.FILE_NAME, db.backupDao().profile()?.avatarFileName)

        avatarStore.write(portrait)
        val version1 = asVersion1(encoded)
        val oldProfile = JSONObject(version1).getJSONObject("profile")
        oldProfile.put("displayName", "旧备份")
        val version1Json = JSONObject(version1).put("profile", oldProfile)
        repository.importSnapshot(BackupCodec.decode(version1Json.toString()))
        assertEquals("旧备份", db.backupDao().profile()?.displayName)
        assertTrue(portrait.contentEquals(avatarStore.readBytes()))
    }

    @Test
    fun missingAvatarDoesNotCreateAFile() = runTest {
        val decoded = BackupCodec.decode(BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L))
        assertTrue(decoded.avatarImage is AvatarImageBackup.None)
        repository.importSnapshot(decoded)
        assertNull(avatarStore.readBytes())
        assertNull(db.backupDao().profile()?.avatarFileName)
    }

    @Test
    fun corruptAvatarDoesNotReplaceCurrentData() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = null, createdAt = 1L),
            emptyList(),
        )
        val portrait = jpeg(android.graphics.Color.GREEN)
        avatarStore.write(portrait)
        db.bodyRecordDao().upsertProfile(
            BodyProfileEntity(avatarFileName = ProfileAvatarStore.FILE_NAME),
        )
        val encoded = BackupCodec.encode(repository.exportSnapshot(), exportedAt = 1L)
        val broken = JSONObject(encoded)
        broken.getJSONObject("profile").put("avatarJpeg", android.util.Base64.encodeToString(byteArrayOf(1, 2, 3, 4), android.util.Base64.NO_WRAP))
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(broken.toString())
        }
        assertEquals("s1", db.backupDao().allSessions().single().id)
        assertTrue(portrait.contentEquals(avatarStore.readBytes()))

        val snapshot = repository.exportSnapshot().copy(
            avatarImage = AvatarImageBackup.Jpeg(byteArrayOf(9, 9, 9)),
        )
        var rejected = false
        try {
            repository.importSnapshot(snapshot)
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals("s1", db.backupDao().allSessions().single().id)
        assertTrue(portrait.contentEquals(avatarStore.readBytes()))
    }

    @Test
    fun invalidTimerLinksAndDuplicateIdsDoNotReplaceCurrentData() = runTest {
        db.timerRecordDao().insert(timer("local", 20_000, 5_000, null, 8L), emptyList())
        val current = repository.exportSnapshot()
        val duplicateTimer = current.copy(
            timerRecords = listOf(timer("same", 1, 1, null, 1L), timer("same", 2, 2, null, 2L)),
            timerLaps = emptyList(),
        )
        assertRejected(duplicateTimer)
        val missingParent = current.copy(
            timerRecords = listOf(timer("parent", 1, 1, null, 1L)),
            timerLaps = listOf(lap("other", 1, 1, 1)),
        )
        assertRejected(missingParent)
        val duplicateSet = current.copy(
            sets = listOf(sampleSet("set"), sampleSet("set")),
            sessions = listOf(WorkoutSessionEntity("s1", 1L, null, 1L)),
            exercises = listOf(exercise()),
        )
        assertRejected(duplicateSet)
        assertEquals(listOf("local"), db.backupDao().allTimerRecords().map { it.id })
    }

    @Test
    fun failedImportStepRollsBackTheDatabase() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "留下", createdAt = 1L),
            emptyList(),
        )
        db.timerRecordDao().insert(timer("local", 20_000, 5_000, null, 8L), emptyList())
        val failing = BackupRepository(
            db,
            db.backupDao(),
            ExerciseRepository(db.exerciseDao(), db.exerciseMuscleDao()),
            WorkoutRepository(db.sessionDao(), db.exerciseDao(), db.bodyRecordDao()),
            avatarStore,
            finishImport = { error("REBUILD") },
        )
        var rejected = false
        try {
            failing.importSnapshot(
                failing.exportSnapshot().copy(sessions = emptyList(), sets = emptyList()),
            )
        } catch (error: IllegalStateException) {
            rejected = error.message == "REBUILD"
        }
        assertTrue(rejected)
        assertEquals("留下", db.backupDao().allSessions().single().note)
        assertEquals("local", db.backupDao().allTimerRecords().single().id)
        assertEquals(0, failing.dataEpoch.value)
    }

    @Test
    fun foreignKeyFailureInsideReplaceDoesNotKeepAPartialImport() = runTest {
        db.exerciseDao().insertIgnore(listOf(exercise()))
        db.sessionDao().replaceSession(
            WorkoutSessionEntity(id = "s1", dateEpochDay = 20_000, note = "留下", createdAt = 1L),
            emptyList(),
        )
        try {
            db.backupDao().replaceAll(
                GymLogSnapshot(
                    exercises = emptyList(),
                    sessions = listOf(WorkoutSessionEntity("s2", 21_000, null, 2L)),
                    sets = listOf(sampleSet("set-new").copy(sessionId = "s2", exerciseId = "missing")),
                    profile = null,
                    measurements = emptyList(),
                    legacyRecords = emptyList(),
                    timerRecords = emptyList(),
                    timerLaps = emptyList(),
                ),
            )
            fail("expected the set insert to fail")
        } catch (error: Exception) {
            assertTrue(error !is AssertionError)
        }
        assertEquals("s1", db.backupDao().allSessions().single().id)
    }

    private suspend fun assertRejected(snapshot: GymLogSnapshot) {
        var rejected = false
        try {
            repository.importSnapshot(snapshot)
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private fun asVersion1(encoded: String): String {
        val json = JSONObject(encoded)
        json.put("version", 1)
        json.remove("timerRecords")
        json.remove("timerLaps")
        json.remove("avatarJpeg")
        if (!json.isNull("profile")) json.getJSONObject("profile").remove("avatarJpeg")
        return json.toString()
    }

    private fun timer(
        id: String,
        day: Long,
        duration: Long,
        sessionId: String?,
        createdAt: Long,
    ): TimerRecordEntity {
        return TimerRecordEntity(
            id = id,
            durationMillis = duration,
            dateEpochDay = day,
            sessionId = sessionId,
            createdAt = createdAt,
        )
    }

    private fun lap(
        recordId: String,
        index: Int,
        interval: Long,
        elapsed: Long,
    ): TimerLapEntity {
        return TimerLapEntity(
            id = "$recordId-$index",
            recordId = recordId,
            lapIndex = index,
            intervalMillis = interval,
            elapsedMillis = elapsed,
        )
    }

    private fun sampleSet(id: String): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = id,
            sessionId = "s1",
            exerciseId = "curl",
            exerciseOrder = 0,
            setIndex = 1,
            weightMode = WeightMode.WEIGHTED.name,
            weightKg = 20.0,
            reps = 8,
            note = null,
        )
    }

    private fun jpeg(color: Int): ByteArray {
        val bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output))
            output.toByteArray()
        }
    }

    private fun exercise(): ExerciseEntity {
        return ExerciseEntity(
            id = "curl",
            name = "弯举",
            bodyPart = BodyPart.ARMS.name,
            equipmentType = EquipmentType.FREE_WEIGHT.name,
            weightMode = WeightMode.WEIGHTED.name,
            isCustom = true,
            aliases = "curl",
            createdAt = 2L,
            archived = false,
        )
    }
}
