package com.example.gymlog

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.gymlog.data.AppearanceStore
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.CalendarColorStore
import com.example.gymlog.data.BodyRecordRepository
import com.example.gymlog.data.ExerciseRepository
import com.example.gymlog.data.GymLogDatabase
import com.example.gymlog.data.MIGRATION_1_2
import com.example.gymlog.data.MIGRATION_2_3
import com.example.gymlog.data.MIGRATION_3_4
import com.example.gymlog.data.MIGRATION_4_5
import com.example.gymlog.data.DayNoteRepository
import com.example.gymlog.data.MIGRATION_5_6
import com.example.gymlog.data.MIGRATION_6_7
import com.example.gymlog.data.MIGRATION_7_8
import com.example.gymlog.data.MIGRATION_8_9
import com.example.gymlog.data.MIGRATION_9_10
import com.example.gymlog.data.MIGRATION_10_11
import com.example.gymlog.data.MIGRATION_11_12
import com.example.gymlog.data.MIGRATION_12_13
import com.example.gymlog.data.DayKindRepository
import com.example.gymlog.data.MIGRATION_13_14
import com.example.gymlog.data.MIGRATION_14_15
import com.example.gymlog.data.MIGRATION_15_16
import com.example.gymlog.data.MIGRATION_16_17
import com.example.gymlog.data.MIGRATION_17_18
import com.example.gymlog.data.MIGRATION_18_19
import com.example.gymlog.data.PhotoStore
import com.example.gymlog.data.PhotoWallRepository
import com.example.gymlog.data.ExerciseCatalogCoordinator
import com.example.gymlog.data.ExerciseCatalogImporter
import com.example.gymlog.data.ExerciseMediaRepository
import com.example.gymlog.data.ProfileAvatarStore
import com.example.gymlog.data.SessionDraftStore
import com.example.gymlog.data.NotificationSettingsStore
import com.example.gymlog.data.TimerAlarmScheduler
import com.example.gymlog.data.TimerRecordRepository
import com.example.gymlog.data.TrainingTimerStore
import com.example.gymlog.data.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymLogApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: GymLogDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            GymLogDatabase::class.java,
            "gymlog.db",
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
            )
            .build()
    }

    val exerciseCatalogCoordinator: ExerciseCatalogCoordinator by lazy {
        ExerciseCatalogCoordinator()
    }

    val exerciseCatalogImporter: ExerciseCatalogImporter by lazy {
        ExerciseCatalogImporter(this, database, exerciseCatalogCoordinator)
    }

    val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepository(database.exerciseDao(), database.exerciseMuscleDao())
    }

    val exerciseMediaRepository: ExerciseMediaRepository by lazy {
        ExerciseMediaRepository(this, database.exerciseCatalogDao())
    }

    val profileAvatarStore: ProfileAvatarStore by lazy {
        ProfileAvatarStore(this)
    }

    val bodyRecordRepository: BodyRecordRepository by lazy {
        BodyRecordRepository(database.bodyRecordDao())
    }

    val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(
            database.sessionDao(),
            database.exerciseDao(),
            database.bodyRecordDao(),
            database.exerciseDurationDao(),
        )
    }

    val dayNoteRepository: DayNoteRepository by lazy {
        DayNoteRepository(database.dayNoteDao())
    }

    val dayKindRepository: DayKindRepository by lazy {
        DayKindRepository(database.dayKindDao())
    }

    val photoStore: PhotoStore by lazy { PhotoStore(this) }

    val photoWallRepository: PhotoWallRepository by lazy {
        PhotoWallRepository(database.photoWallDao(), photoStore)
    }

    val appearanceStore: AppearanceStore by lazy {
        AppearanceStore(this)
    }

    val calendarColorStore: CalendarColorStore by lazy {
        CalendarColorStore(this)
    }

    val trainingTimerStore: TrainingTimerStore by lazy {
        TrainingTimerStore(this)
    }

    val sessionDraftStore: SessionDraftStore by lazy {
        SessionDraftStore(filesDir)
    }

    val timerRecordRepository: TimerRecordRepository by lazy {
        TimerRecordRepository(database.timerRecordDao())
    }

    val notificationSettingsStore: NotificationSettingsStore by lazy {
        NotificationSettingsStore(this)
    }

    val timerAlarmScheduler: TimerAlarmScheduler by lazy {
        TimerAlarmScheduler(this, notificationSettingsStore)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            database,
            database.backupDao(),
            exerciseRepository,
            workoutRepository,
            profileAvatarStore,
            exerciseCatalogImporter = exerciseCatalogImporter,
            coordinator = exerciseCatalogCoordinator,
        )
    }

    override fun onCreate() {
        super.onCreate()
        // 独立倒计时不跨进程恢复；进程重建后不能留下上一进程排队的旧闹钟，
        // 否则新会话和旧提醒会互相矛盾。总计时和训练草稿不受影响。
        timerAlarmScheduler.cancel()
        appScope.launch {
            runCatching {
                exerciseCatalogCoordinator.withLock {
                    val stats = exerciseCatalogImporter.importIfNeededLocked(force = false)
                    workoutRepository.rebuildAllPersonalRecords()
                    stats
                }
            }
                .onSuccess { stats ->
                    Log.i(
                        "ExerciseCatalog",
                        "import added=${stats.added} linked=${stats.linked} skipped=${stats.skipped} " +
                            "pending=${stats.pendingMatches} unmapped=${stats.unmappedTokens} " +
                            "failed=${stats.failed} already=${stats.alreadyImported}",
                    )
                }
                .onFailure { error -> Log.e("ExerciseCatalog", "import failed; retry on next launch", error) }
        }
    }
}
