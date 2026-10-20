package com.example.gymlog.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupRepository(
    private val database: GymLogDatabase,
    private val backupDao: BackupDao,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val avatarStore: ProfileAvatarStore,
    private val exerciseCatalogImporter: ExerciseCatalogImporter? = null,
    private val coordinator: ExerciseCatalogCoordinator =
        exerciseCatalogImporter?.coordinator ?: ExerciseCatalogCoordinator(),
    private val finishImport: suspend () -> Unit = {
        workoutRepository.rebuildAllPersonalRecords()
    },
) {
    private val epoch = MutableStateFlow(0)
    val dataEpoch: StateFlow<Int> = epoch.asStateFlow()

    suspend fun exportSnapshot(): GymLogSnapshot {
        val profile = backupDao.profile()
        val avatarBytes = if (profile?.avatarFileName == null) null else avatarStore.readBytes()
        val avatar = if (avatarBytes == null) {
            AvatarImageBackup.None
        } else {
            AvatarImageBackup.Jpeg(avatarBytes)
        }
        return GymLogSnapshot(
            exercises = backupDao.allExercises(),
            sessions = backupDao.allSessions(),
            sets = backupDao.allSets(),
            exerciseDurations = backupDao.allExerciseDurations(),
            profile = if (avatarBytes == null) profile?.copy(avatarFileName = null) else profile,
            measurements = backupDao.allMeasurements(),
            legacyRecords = backupDao.allLegacyRecords(),
            dayNotes = backupDao.allDayNotes(),
            dayKinds = backupDao.allDayKinds(),
            exerciseMuscles = exportedMuscleLinks(),
            timerRecords = backupDao.allTimerRecords(),
            timerLaps = backupDao.allTimerLaps(),
            avatarImage = avatar,
        )
    }

    private suspend fun exportedMuscleLinks(): List<MuscleLink> {
        val present = backupDao.allExercises().map { it.id }.toSet()
        val stored = backupDao.allExerciseMuscles().map { it.toLink() }
        val presets = PresetMuscleMappings.exerciseIds()
            .filter { it in present }
            .flatMap { PresetMuscleMappings.links(it) }
        return stored + presets
    }

    private data class DatasetState(
        val contents: List<ExerciseContentEntity>,
        val media: List<ExerciseMediaEntity>,
        val sourceMaps: List<ExerciseSourceMapEntity>,
        val muscleTokens: List<ExerciseMuscleTokenEntity>,
        val presetCandidates: List<ExercisePresetMatchCandidateEntity>,
        val storedMuscleLinks: List<ExerciseMuscleEntity>,
        val importState: ExerciseImportStateEntity?,
    )

    private suspend fun captureDatasetState(): DatasetState {
        val catalog = database.exerciseCatalogDao()
        return DatasetState(
            contents = catalog.allContents(),
            media = catalog.allMedia(),
            sourceMaps = catalog.allSourceMaps(),
            muscleTokens = catalog.allMuscleTokens(),
            presetCandidates = catalog.presetCandidates(),
            storedMuscleLinks = backupDao.allExerciseMuscles(),
            importState = catalog.importState(ExerciseSource.DATASET),
        )
    }

    private suspend fun restoreDatasetState(state: DatasetState) {
        val catalog = database.exerciseCatalogDao()
        if (state.contents.isNotEmpty()) catalog.insertContents(state.contents)
        if (state.media.isNotEmpty()) catalog.insertMedia(state.media)
        if (state.sourceMaps.isNotEmpty()) catalog.insertSourceMaps(state.sourceMaps)
        if (state.muscleTokens.isNotEmpty()) catalog.insertMuscleTokens(state.muscleTokens)
        catalog.insertPresetCandidates(state.presetCandidates)
        if (state.storedMuscleLinks.isNotEmpty()) {
            backupDao.insertExerciseMuscles(state.storedMuscleLinks)
        }
        if (state.importState == null) {
            catalog.clearImportState()
        } else {
            catalog.putImportState(state.importState)
        }
    }

    suspend fun importSnapshot(snapshot: GymLogSnapshot) = coordinator.withLock {
        snapshot.validate()
        val previous = exportSnapshot()
        val previousDataset = captureDatasetState()
        val previousAvatar = avatarStore.readBytes()
        try {
            database.withTransaction {
                backupDao.replaceAll(snapshot)
                finishImport()
            }
            // Caller already owns the coordinator lock; do not acquire it again.
            exerciseCatalogImporter?.importIfNeededLocked(force = false)
            applyAvatar(snapshot.avatarImage)
        } catch (error: Throwable) {
            runCatching {
                database.withTransaction {
                    backupDao.replaceAll(previous)
                    restoreDatasetState(previousDataset)
                }
                restoreAvatar(previousAvatar)
            }.onFailure { rollbackError -> error.addSuppressed(rollbackError) }
            throw error
        }
        epoch.value = epoch.value + 1
    }

    private fun applyAvatar(image: AvatarImageBackup) {
        when (image) {
            AvatarImageBackup.Unspecified -> Unit
            AvatarImageBackup.None -> avatarStore.delete()
            is AvatarImageBackup.Jpeg -> avatarStore.write(image.bytes)
        }
    }

    private fun restoreAvatar(bytes: ByteArray?) {
        if (bytes == null) avatarStore.delete() else avatarStore.write(bytes)
    }

    suspend fun clearLocalData() = coordinator.withLock {
        val previous = exportSnapshot()
        val previousDataset = captureDatasetState()
        try {
            backupDao.clearAll()
            // Caller already owns the coordinator lock.
            exerciseCatalogImporter?.importIfNeededLocked(force = false)
            workoutRepository.rebuildAllPersonalRecords()
        } catch (error: Throwable) {
            runCatching {
                database.withTransaction {
                    backupDao.replaceAll(previous)
                    restoreDatasetState(previousDataset)
                }
            }.onFailure { rollbackError -> error.addSuppressed(rollbackError) }
            throw error
        }
        epoch.value = epoch.value + 1
    }
}
