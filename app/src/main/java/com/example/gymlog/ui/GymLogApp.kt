package com.example.gymlog.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymlog.GymLogApplication
import com.example.gymlog.R
import com.example.gymlog.data.CalendarDayColorLogic
import com.example.gymlog.data.ActiveWorkoutNotifier
import com.example.gymlog.data.WorkoutClockKind
import com.example.gymlog.data.WorkoutNotificationClock
import com.example.gymlog.data.WorkoutNotificationPhase
import com.example.gymlog.data.CalendarDayPaint
import com.example.gymlog.data.CalendarDayPartsLogic
import com.example.gymlog.data.DayKind
import com.example.gymlog.data.DayKindLogic
import com.example.gymlog.data.SessionDetail
import com.example.gymlog.data.SessionExercise
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.BodyMeasurementEntity
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.TimerPhase
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.data.WorkoutClockLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.displayName
import com.example.gymlog.data.orderedForDay
import com.example.gymlog.data.partVolumes
import com.example.gymlog.ui.home.HomeScreen
import com.example.gymlog.ui.body.BodyRecordScreen
import com.example.gymlog.ui.muscle.MuscleMapScreen
import com.example.gymlog.ui.muscle.MuscleMapViewModel
import com.example.gymlog.ui.body.BodyRecordViewModel
import com.example.gymlog.ui.calendar.DayKindSelector
import com.example.gymlog.ui.calendar.CalendarMonthOverview
import com.example.gymlog.ui.calendar.TrainingCalendarDetailScreen
import com.example.gymlog.ui.exercise.ExerciseDetailScreen
import com.example.gymlog.ui.exercise.ExerciseDetailViewModel
import com.example.gymlog.ui.exercise.ExerciseLibraryScreen
import com.example.gymlog.ui.exercise.ExerciseLibraryViewModel
import com.example.gymlog.ui.exercise.ExerciseThumbnail
import com.example.gymlog.ui.photo.PhotoDetailScreen
import com.example.gymlog.ui.photo.PhotoEditorScreen
import com.example.gymlog.ui.photo.PhotoWallScreen
import com.example.gymlog.ui.photo.PhotoWallViewModel
import com.example.gymlog.ui.progress.ProgressScreen
import com.example.gymlog.ui.train.ActiveWorkoutMiniBar
import com.example.gymlog.ui.train.ActiveWorkoutUi
import com.example.gymlog.ui.train.TrainingHomeScreen
import com.example.gymlog.ui.train.currentExerciseLabel
import com.example.gymlog.ui.session.SessionEditorScreen
import com.example.gymlog.ui.session.SessionEditorViewModel
import com.example.gymlog.ui.session.WorkoutClockMode
import com.example.gymlog.ui.settings.AboutScreen
import com.example.gymlog.ui.settings.MineViewModel
import com.example.gymlog.ui.settings.ReleaseNotesScreen
import com.example.gymlog.ui.settings.AppSettingsScreen
import com.example.gymlog.ui.settings.BackupDataScreen
import com.example.gymlog.ui.settings.DataTransferViewModel
import com.example.gymlog.ui.settings.AppearanceSettingsScreen
import com.example.gymlog.ui.settings.CalendarColorSettingsScreen
import com.example.gymlog.ui.settings.CalendarCustomColorsScreen
import com.example.gymlog.ui.settings.MineScreen
import com.example.gymlog.ui.settings.NotificationSettingsScreen
import com.example.gymlog.ui.settings.TrainingPreferencesScreen
import com.example.gymlog.ui.settings.TrainingPreferencesViewModel
import com.example.gymlog.ui.settings.SettingsTopBarAction
import com.example.gymlog.ui.settings.SettingsViewModel
import com.example.gymlog.ui.stats.BodyTrendScreen
import com.example.gymlog.ui.stats.BodyTrendViewModel
import com.example.gymlog.ui.stats.TrainingTrendScreen
import com.example.gymlog.ui.stats.TrainingTrendViewModel
import com.example.gymlog.ui.timer.SavedTimerScreen
import com.example.gymlog.ui.timer.TimerHistoryScreen
import com.example.gymlog.ui.timer.TrainingTimerScreen
import com.example.gymlog.ui.timer.TrainingTimerUiState
import com.example.gymlog.ui.timer.TrainingTimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

private enum class RootTab {
    Home,
    Calendar,
    Train,
    Progress,
    Mine,
}

private sealed interface Screen {
    data object Home : Screen
    data object Calendar : Screen
    data object CalendarExpanded : Screen
    data object Train : Screen
    data object ExerciseLibrary : Screen
    data class ExerciseDetail(val exerciseId: String) : Screen
    data object Progress : Screen
    data object Mine : Screen
    data class BodyRecord(val returnTab: RootTab = RootTab.Mine) : Screen
    data class MuscleMap(
        val initialDateEpochDay: Long,
        val returnTab: RootTab = RootTab.Mine,
    ) : Screen
    data object BodyTrend : Screen
    data object TrainingTrend : Screen
    data class SessionEditor(
        val sessionId: String?,
        val dateEpochDay: Long? = null,
        val draftKey: String = sessionId ?: "new-${UUID.randomUUID()}",
        val returnTab: RootTab = RootTab.Calendar,
    ) : Screen
    data class PickExercise(val returnTo: SessionEditor) : Screen
    data class Settings(val from: RootTab) : Screen
    data class Appearance(val from: RootTab) : Screen
    data class CalendarColors(val from: RootTab) : Screen
    data class CalendarCustomColors(val from: RootTab) : Screen
    data class NotificationSettings(val from: RootTab) : Screen
    data class TrainingPreferences(val from: RootTab) : Screen
    data class BackupData(val from: RootTab) : Screen
    data class TimerHistory(val from: RootTab) : Screen
    data object PhotoWall : Screen
    data class PhotoDetail(val id: String) : Screen
    data object PhotoEditor : Screen
    data class About(val from: RootTab, val openedFromSettings: Boolean = true) : Screen
    data class ReleaseNotes(val from: RootTab, val openedFromSettings: Boolean = true) : Screen
    data object TrainingTimer : Screen
    data class SavedTimer(
        val recordId: String,
        val returnTab: RootTab = RootTab.Calendar,
        val returnToTimerHistory: Boolean = false,
    ) : Screen
}

private val ScreenSaver: Saver<Screen, Any> = Saver(
    save = { screen ->
        when (screen) {
            Screen.Home -> mapOf("type" to "home")
            Screen.Calendar -> mapOf("type" to "calendar")
            Screen.CalendarExpanded -> mapOf("type" to "calendar-expanded")
            Screen.Train -> mapOf("type" to "train")
            Screen.ExerciseLibrary -> mapOf("type" to "exercise-library")
            Screen.Progress -> mapOf("type" to "progress")
            Screen.Mine -> mapOf("type" to "mine")
            Screen.BodyTrend -> mapOf("type" to "body-trend")
            Screen.TrainingTrend -> mapOf("type" to "training-trend")
            Screen.PhotoWall -> mapOf("type" to "photo-wall")
            Screen.PhotoEditor -> mapOf("type" to "photo-editor")
            Screen.TrainingTimer -> mapOf("type" to "training-timer")
            is Screen.BodyRecord -> mapOf(
                "type" to "body-record",
                "returnTab" to screen.returnTab.name,
            )
            is Screen.MuscleMap -> mapOf(
                "type" to "muscle-map",
                "date" to screen.initialDateEpochDay,
                "returnTab" to screen.returnTab.name,
            )
            is Screen.SessionEditor -> mapOf(
                "type" to "session",
                "sessionId" to (screen.sessionId ?: ""),
                "date" to (screen.dateEpochDay ?: Long.MIN_VALUE),
                "draftKey" to screen.draftKey,
                "returnTab" to screen.returnTab.name,
            )
            is Screen.PickExercise -> mapOf(
                "type" to "pick-exercise",
                "sessionId" to (screen.returnTo.sessionId ?: ""),
                "date" to (screen.returnTo.dateEpochDay ?: Long.MIN_VALUE),
                "draftKey" to screen.returnTo.draftKey,
                "returnTab" to screen.returnTo.returnTab.name,
            )
            is Screen.Settings -> mapOf("type" to "settings", "from" to screen.from.name)
            is Screen.Appearance -> mapOf("type" to "appearance", "from" to screen.from.name)
            is Screen.CalendarColors -> mapOf("type" to "calendar-colors", "from" to screen.from.name)
            is Screen.CalendarCustomColors -> mapOf("type" to "calendar-custom-colors", "from" to screen.from.name)
            is Screen.NotificationSettings -> mapOf("type" to "notification-settings", "from" to screen.from.name)
            is Screen.TrainingPreferences -> mapOf("type" to "training-preferences", "from" to screen.from.name)
            is Screen.BackupData -> mapOf("type" to "backup", "from" to screen.from.name)
            is Screen.TimerHistory -> mapOf("type" to "timer-history", "from" to screen.from.name)
            is Screen.PhotoDetail -> mapOf("type" to "photo-detail", "id" to screen.id)
            is Screen.ExerciseDetail -> mapOf(
                "type" to "exercise-detail",
                "id" to screen.exerciseId,
            )
            is Screen.About -> mapOf(
                "type" to "about",
                "from" to screen.from.name,
                "openedFromSettings" to screen.openedFromSettings,
            )
            is Screen.ReleaseNotes -> mapOf(
                "type" to "release-notes",
                "from" to screen.from.name,
                "openedFromSettings" to screen.openedFromSettings,
            )
            is Screen.SavedTimer -> mapOf(
                "type" to "saved-timer",
                "recordId" to screen.recordId,
                "returnTab" to screen.returnTab.name,
                "returnToTimerHistory" to screen.returnToTimerHistory,
            )
        }
    },
    restore = { saved ->
        val values = saved as? Map<*, *> ?: return@Saver Screen.Home
        fun tab(key: String): RootTab =
            (values[key] as? String)
                ?.let { runCatching { RootTab.valueOf(it) }.getOrNull() }
                ?: RootTab.Calendar
        fun session(values: Map<*, *>): Screen.SessionEditor {
            val id = (values["sessionId"] as? String).orEmpty()
            val date = values["date"] as? Long
            return Screen.SessionEditor(
                sessionId = id.ifBlank { null },
                dateEpochDay = date?.takeIf { it != Long.MIN_VALUE },
                draftKey = values["draftKey"] as? String ?: "new-${UUID.randomUUID()}",
                returnTab = tab("returnTab"),
            )
        }
        when (values["type"] as? String) {
            "home" -> Screen.Home
            "calendar" -> Screen.Calendar
            "calendar-expanded" -> Screen.CalendarExpanded
            "train" -> Screen.Train
            "exercise-library" -> Screen.ExerciseLibrary
            "progress" -> Screen.Progress
            "mine" -> Screen.Mine
            "body-trend" -> Screen.BodyTrend
            "training-trend" -> Screen.TrainingTrend
            "photo-wall" -> Screen.PhotoWall
            "photo-editor" -> Screen.PhotoEditor
            "training-timer" -> Screen.TrainingTimer
            "body-record" -> Screen.BodyRecord(tab("returnTab"))
            "muscle-map" -> Screen.MuscleMap(
                initialDateEpochDay = values["date"] as? Long ?: 0L,
                returnTab = tab("returnTab"),
            )
            "session" -> session(values)
            "pick-exercise" -> Screen.PickExercise(session(values))
            "settings" -> Screen.Settings(tab("from"))
            "appearance" -> Screen.Appearance(tab("from"))
            "calendar-colors" -> Screen.CalendarColors(tab("from"))
            "calendar-custom-colors" -> Screen.CalendarCustomColors(tab("from"))
            "notification-settings" -> Screen.NotificationSettings(tab("from"))
            "training-preferences" -> Screen.TrainingPreferences(tab("from"))
            "backup" -> Screen.BackupData(tab("from"))
            "timer-history" -> Screen.TimerHistory(tab("from"))
            "photo-detail" -> Screen.PhotoDetail(values["id"] as? String ?: "")
            "exercise-detail" -> Screen.ExerciseDetail(values["id"] as? String ?: "")
            "about" -> Screen.About(
                from = tab("from"),
                openedFromSettings = values["openedFromSettings"] as? Boolean ?: true,
            )
            "release-notes" -> Screen.ReleaseNotes(
                from = tab("from"),
                openedFromSettings = values["openedFromSettings"] as? Boolean ?: true,
            )
            "saved-timer" -> Screen.SavedTimer(
                recordId = values["recordId"] as? String ?: "",
                returnTab = tab("returnTab"),
                returnToTimerHistory = values["returnToTimerHistory"] as? Boolean ?: false,
            )
            else -> Screen.Home
        }
    },
)

private val SessionDraftSaver: Saver<Screen.SessionEditor?, Any> = Saver(
    save = { draft ->
        if (draft == null) {
            null
        } else {
            mapOf(
                "sessionId" to (draft.sessionId ?: ""),
                "dateEpochDay" to (draft.dateEpochDay ?: Long.MIN_VALUE),
                "draftKey" to draft.draftKey,
                "returnTab" to draft.returnTab.name,
            )
        }
    },
    restore = { saved ->
        val values = saved as? Map<*, *> ?: return@Saver null
        val sessionId = (values["sessionId"] as? String).orEmpty()
        val dateEpochDay = values["dateEpochDay"] as? Long
        Screen.SessionEditor(
            sessionId = sessionId.ifBlank { null },
            dateEpochDay = dateEpochDay?.takeIf { it != Long.MIN_VALUE },
            draftKey = values["draftKey"] as? String ?: "new-${UUID.randomUUID()}",
            returnTab = (values["returnTab"] as? String)
                ?.let { runCatching { RootTab.valueOf(it) }.getOrNull() }
                ?: RootTab.Calendar,
        )
    },
)

private val Screen.isRoot: Boolean
    get() = this is Screen.Home || this is Screen.Calendar || this is Screen.Train ||
        this is Screen.Progress || this is Screen.Mine

private fun Screen.toRootTab(): RootTab = when (this) {
    Screen.Home -> RootTab.Home
    Screen.Calendar, Screen.CalendarExpanded, is Screen.SavedTimer, is Screen.SessionEditor, is Screen.PickExercise -> RootTab.Calendar
    Screen.Train, Screen.ExerciseLibrary, Screen.TrainingTimer -> RootTab.Train
    is Screen.ExerciseDetail -> RootTab.Train
    Screen.Progress, Screen.TrainingTrend, Screen.BodyTrend -> RootTab.Progress
    Screen.Mine, is Screen.BodyRecord, is Screen.MuscleMap, Screen.PhotoWall, is Screen.PhotoDetail, Screen.PhotoEditor -> RootTab.Mine
    is Screen.Settings -> from
    is Screen.Appearance -> from
    is Screen.CalendarColors -> from
    is Screen.CalendarCustomColors -> from
    is Screen.NotificationSettings -> from
    is Screen.TrainingPreferences -> from
    is Screen.BackupData -> from
    is Screen.TimerHistory -> from
    is Screen.About -> from
    is Screen.ReleaseNotes -> from
}

private fun RootTab.toScreen(): Screen = when (this) {
    RootTab.Home -> Screen.Home
    RootTab.Calendar -> Screen.Calendar
    RootTab.Train -> Screen.Train
    RootTab.Progress -> Screen.Progress
    RootTab.Mine -> Screen.Mine
}

@Composable
fun GymLogApp(
    viewModel: GymLogViewModel = viewModel(),
    /** 点「正在训练」通知进入时 +1；0 表示普通启动。 */
    openActiveWorkoutSignal: Int = 0,
) {
    val app = LocalContext.current.applicationContext as GymLogApplication
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val exerciseMedia by viewModel.exerciseMedia.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val dayNotes by viewModel.dayNotes.collectAsStateWithLifecycle()
    val dayKinds by viewModel.dayKinds.collectAsStateWithLifecycle()
    val dayPaints = remember(sessions, measurements, dayKinds) {
        CalendarDayColorLogic.paintsFor(sessions, measurements, dayKinds)
    }
    val dayParts = remember(sessions, measurements) {
        CalendarDayPartsLogic.labelsFor(sessions, measurements)
    }
    val dataEpoch by app.backupRepository.dataEpoch.collectAsStateWithLifecycle()
    val photoViewModel: PhotoWallViewModel = viewModel(factory = PhotoWallViewModel.factory(app))
    val timerViewModel: TrainingTimerViewModel = viewModel(
        factory = TrainingTimerViewModel.factory(app),
    )
    val timerState by timerViewModel.uiState.collectAsStateWithLifecycle()
    var screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Home)
    }
    var activeDraft by rememberSaveable(stateSaver = SessionDraftSaver) {
        mutableStateOf<Screen.SessionEditor?>(null)
    }
    var confirmDiscardActive by remember { mutableStateOf(false) }
    // 冷启动是否已经查过草稿快照。用于两件事：判断「确实没有活动训练」，
    // 以及点通知进来时等草稿恢复完再跳转，避免抢跑。
    var snapshotChecked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (activeDraft == null) {
            val snapshot = withContext(Dispatchers.IO) { app.sessionDraftStore.load() }
            if (snapshot != null && snapshot.draftKey.isNotBlank()) {
                activeDraft = Screen.SessionEditor(
                    sessionId = snapshot.sessionId,
                    dateEpochDay = snapshot.dateEpochDay,
                    draftKey = snapshot.draftKey,
                    returnTab = RootTab.Train,
                )
                if (screen == Screen.Home) screen = Screen.Train
            }
        }
        snapshotChecked = true
    }
    val activeEditorViewModel: SessionEditorViewModel? = activeDraft?.let { draft ->
        viewModel(
            key = "${draft.draftKey}-$dataEpoch",
            factory = SessionEditorViewModel.factory(
                app.workoutRepository,
                app.bodyRecordRepository,
                draft.sessionId,
                draft.dateEpochDay,
                app.photoWallRepository,
                app.timerRecordRepository,
                draftStore = app.sessionDraftStore,
                draftKey = draft.draftKey,
                timerAlarmScheduler = app.timerAlarmScheduler,
            ),
        )
    }
    val activeEditorState = if (activeEditorViewModel != null) {
        activeEditorViewModel.state.collectAsStateWithLifecycle().value
    } else {
        null
    }
    val activeEditorClock = activeEditorViewModel?.clock?.collectAsStateWithLifecycle()?.value
    val activeWorkoutTimerText = if (activeEditorClock?.running == true) {
        WorkoutClockLogic.formatClock(activeEditorClock.displayMillis)
    } else {
        null
    }
    val activeWorkout = activeEditorState?.let { state ->
        ActiveWorkoutUi(
            durationMillis = state.totalElapsedMillis,
            currentExerciseName = currentExerciseLabel(state.exercises),
            timerText = activeWorkoutTimerText,
        )
    }

    // ---- 「正在训练」系统通知：只反映现有状态，不参与计时 ----
    // 只有新建草稿（sessionId == null）才挂通知；编辑历史记录不会出现。
    val notificationDraft = activeDraft?.takeIf { it.sessionId == null }
    val notificationPhase = activeEditorState?.let { state ->
        when {
            state.finishing -> WorkoutNotificationPhase.FINISHING
            !state.totalTimerRunning -> WorkoutNotificationPhase.PAUSED
            else -> WorkoutNotificationPhase.RUNNING
        }
    }
    val notificationExercise = activeEditorState?.let { currentExerciseLabel(it.exercises) }
    // 运行中不把这个值放进 key：秒数交给系统 chronometer，避免每秒重投通知
    val notificationFrozenElapsed = activeEditorState
        ?.takeIf { notificationPhase != WorkoutNotificationPhase.RUNNING }
        ?.totalElapsedMillis
    // 独立计时器（组歇 / 秒表）只是被展示，不参与任何计时计算。
    // 运行中的秒数不进 key：那部分由服务按锚点每秒重绘，避免 UI 与服务重复投递。
    val notificationClockRunning = activeEditorClock?.running == true
    val notificationClockStarted = activeEditorClock?.started == true
    val notificationClockMode = activeEditorClock?.mode
    val notificationClockDuration = activeEditorClock?.timerDurationMillis
    val notificationClockPausedMillis = activeEditorClock
        ?.takeIf { !notificationClockRunning }
        ?.displayMillis
    LaunchedEffect(
        notificationDraft?.draftKey,
        notificationPhase,
        notificationExercise,
        notificationFrozenElapsed,
        notificationClockMode,
        notificationClockRunning,
        notificationClockStarted,
        notificationClockDuration,
        notificationClockPausedMillis,
        snapshotChecked,
    ) {
        val draft = notificationDraft
        if (draft == null) {
            if (snapshotChecked) ActiveWorkoutNotifier.clear(app, null)
            return@LaunchedEffect
        }
        val phase = notificationPhase ?: return@LaunchedEffect
        val clock = activeEditorClock?.let { value ->
            WorkoutNotificationClock(
                kind = if (value.mode == WorkoutClockMode.TIMER) {
                    WorkoutClockKind.COUNTDOWN
                } else {
                    WorkoutClockKind.STOPWATCH
                },
                running = value.running,
                started = value.started,
                displayMillis = value.displayMillis,
                durationMillis = value.timerDurationMillis,
                anchoredAtElapsedRealtime = SystemClock.elapsedRealtime(),
            )
        }
        ActiveWorkoutNotifier.publish(
            context = app,
            draftKey = draft.draftKey,
            sessionId = draft.sessionId,
            phase = phase,
            elapsedMillis = activeEditorState?.totalElapsedMillis ?: 0L,
            exerciseName = notificationExercise,
            clock = clock,
        )
    }

    // ---- 点通知回到同一场训练（等草稿就绪再跳，绝不新建草稿）----
    var pendingOpenWorkout by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openActiveWorkoutSignal) {
        if (openActiveWorkoutSignal > 0) pendingOpenWorkout = true
    }
    LaunchedEffect(pendingOpenWorkout, activeDraft, snapshotChecked) {
        if (!pendingOpenWorkout) return@LaunchedEffect
        val draft = activeDraft
        if (draft != null) {
            screen = draft
            pendingOpenWorkout = false
        } else if (snapshotChecked) {
            // 没有活动训练（通知已过期），安静落地到当前页面
            pendingOpenWorkout = false
        }
    }
    val density = LocalDensity.current
    val navigationBarBottom = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    var measuredBarHeight by remember { mutableStateOf(80.dp + navigationBarBottom) }
    val isRoot = screen.isRoot

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = measuredBarHeight)
                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .then(if (isRoot) Modifier else Modifier.clearAndSetSemantics {}),
        ) {
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Home) {
                HomeScreen(
                    sessions = sessions,
                    measurements = measurements,
                    savedTimers = timerState.saved,
                    dayNotes = dayNotes,
                    onStartTraining = {
                        val draft = activeDraft ?: Screen.SessionEditor(
                            sessionId = null,
                            returnTab = RootTab.Home,
                        ).also { activeDraft = it }
                        screen = draft
                    },
                    onOpenSession = { sessionId ->
                        screen = Screen.SessionEditor(
                            sessionId = sessionId,
                            returnTab = RootTab.Home,
                        )
                    },
                    onOpenSavedTimer = { recordId ->
                        screen = Screen.SavedTimer(recordId, returnTab = RootTab.Home)
                    },
                    onOpenBody = { screen = Screen.BodyRecord(RootTab.Home) },
                    onOpenTrends = { screen = Screen.Progress },
                    onOpenCalendar = { date ->
                        viewModel.onCalendarDayClick(date)
                        screen = Screen.Calendar
                    },
                    onOpenSettings = { screen = Screen.Settings(RootTab.Home) },
                )
            }
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Calendar) {
                HistoryScreen(
                    sessions = sessions,
                    exerciseMedia = exerciseMedia,
                    measurements = measurements,
                    calendarMonth = calendarMonth,
                    selectedDate = selectedDate,
                    onCalendarMonthChange = viewModel::onCalendarMonthChange,
                    onDayClick = viewModel::onCalendarDayClick,
                    onEdit = { screen = Screen.SessionEditor(it.session.id) },
                    onOpenSettings = { screen = Screen.Settings(RootTab.Calendar) },
                    dayNotes = dayNotes,
                    dayKinds = dayKinds,
                    dayPaints = dayPaints,
                    onSaveDayNote = viewModel::saveDayNote,
                    onSelectDayKind = viewModel::selectDayKind,
                    onOpenCalendar = { screen = Screen.CalendarExpanded },
                    timer = timerState,
                    onOpenSavedTimer = { screen = Screen.SavedTimer(it) },
                )
            }
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Train) {
                TrainingHomeScreen(
                    hasActiveWorkout = activeWorkout != null,
                    onStartTraining = {
                        val draft = activeDraft ?: Screen.SessionEditor(
                            sessionId = null,
                            returnTab = RootTab.Train,
                        ).also { activeDraft = it }
                        screen = draft
                    },
                    onOpenLibrary = { screen = Screen.ExerciseLibrary },
                    onOpenTimer = { screen = Screen.TrainingTimer },
                    onOpenSettings = { screen = Screen.Settings(RootTab.Train) },
                )
            }
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Progress) {
                ProgressScreen(
                    onOpenTrainingTrend = { screen = Screen.TrainingTrend },
                    onOpenBodyTrend = { screen = Screen.BodyTrend },
                    onOpenSettings = { screen = Screen.Settings(RootTab.Progress) },
                )
            }
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Mine) {
                val mineViewModel: MineViewModel = viewModel(
                    key = "mine-$dataEpoch",
                    factory = MineViewModel.factory(
                        app.workoutRepository,
                        app.timerRecordRepository,
                        app.bodyRecordRepository,
                        app.profileAvatarStore,
                    ),
                )
                MineScreen(
                    viewModel = mineViewModel,
                    onOpenBodyRecord = { screen = Screen.BodyRecord() },
                    onOpenPhotoWall = { screen = Screen.PhotoWall },
                    onOpenSettings = { screen = Screen.Settings(RootTab.Mine) },
                )
            }
        }

        if (isRoot) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        measuredBarHeight = with(density) { size.height.toDp() }
                    },
            ) {
                activeWorkout?.let { workout ->
                    ActiveWorkoutMiniBar(
                        workout = workout,
                        onResume = { activeDraft?.let { screen = it } },
                        onDiscard = { confirmDiscardActive = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                RootTabBar(
                    selected = screen.toRootTab(),
                    onSelect = { screen = it.toScreen() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!isRoot) {
            val childInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = childInteraction,
                            indication = null,
                            onClick = {},
                        )
                        .clearAndSetSemantics {},
                )
                when (val current = screen) {
                    Screen.Home, Screen.Calendar, Screen.Train, Screen.Progress, Screen.Mine -> Unit
                    Screen.PhotoWall -> {
                        PhotoWallScreen(
                            viewModel = photoViewModel,
                            onBack = { screen = Screen.Mine },
                            onOpenEditor = { screen = Screen.PhotoEditor },
                            onOpen = { screen = Screen.PhotoDetail(it) },
                        )
                    }
                    is Screen.PhotoDetail -> {
                        val wall by photoViewModel.wall.collectAsStateWithLifecycle()
                        val photo = wall.all.firstOrNull { it.id == current.id }
                        PhotoDetailScreen(
                            photo = photo,
                            loadFull = photoViewModel::loadFull,
                            onBack = { screen = Screen.PhotoWall },
                            onEdit = {
                                photoViewModel.editExisting(current.id) { ok ->
                                    if (ok) screen = Screen.PhotoEditor
                                }
                            },
                            onChangeDate = { epoch -> photoViewModel.changeDate(current.id, epoch) },
                            onChangeNote = { note -> photoViewModel.changeNote(current.id, note) },
                            onDelete = {
                                photoViewModel.delete(current.id) { screen = Screen.PhotoWall }
                            },
                        )
                    }
                    Screen.PhotoEditor -> {
                        PhotoEditorScreen(
                            viewModel = photoViewModel,
                            onClose = { screen = Screen.PhotoWall },
                        )
                    }
                    Screen.CalendarExpanded -> {
                        TrainingCalendarDetailScreen(
                            month = calendarMonth,
                            dayPaints = dayPaints,
                            selectedDate = selectedDate,
                            onMonthChange = viewModel::onCalendarMonthChange,
                            onDayClick = { date ->
                                viewModel.onCalendarDayClick(date)
                                screen = Screen.Calendar
                            },
                            onBack = { screen = Screen.Calendar },
                            dayParts = dayParts,
                        )
                    }
                    is Screen.BodyRecord -> {
                        val bodyViewModel: BodyRecordViewModel = viewModel(
                            key = "body-$dataEpoch",
                            factory = BodyRecordViewModel.factory(
                                app.bodyRecordRepository,
                                app.workoutRepository,
                            ),
                        )
                        val bodyMuscleViewModel: MuscleMapViewModel = viewModel(
                            key = "body-figure-${selectedDate.toEpochDay()}-$dataEpoch",
                            factory = MuscleMapViewModel.factory(
                                app.workoutRepository,
                                app.exerciseRepository,
                                selectedDate.toEpochDay(),
                            ),
                        )
                        val bodyMuscleState by bodyMuscleViewModel.state.collectAsStateWithLifecycle()
                        BodyRecordScreen(
                            viewModel = bodyViewModel,
                            muscleHighlights = bodyMuscleState.highlights,
                            onBack = { screen = current.returnTab.toScreen() },
                            onOpenMuscleMap = {
                                screen = Screen.MuscleMap(
                                    selectedDate.toEpochDay(),
                                    current.returnTab,
                                )
                            },
                        )
                    }

                    is Screen.MuscleMap -> {
                        val muscleViewModel: MuscleMapViewModel = viewModel(
                            key = "muscle-${current.initialDateEpochDay}-$dataEpoch",
                            factory = MuscleMapViewModel.factory(
                                app.workoutRepository,
                                app.exerciseRepository,
                                current.initialDateEpochDay,
                            ),
                        )
                        MuscleMapScreen(
                            viewModel = muscleViewModel,
                            onBack = { screen = Screen.BodyRecord(current.returnTab) },
                        )
                    }

                    Screen.BodyTrend -> {
                        val trendViewModel: BodyTrendViewModel = viewModel(
                            key = "body-trend-$dataEpoch",
                            factory = BodyTrendViewModel.factory(app.bodyRecordRepository),
                        )
                        BodyTrendScreen(
                            viewModel = trendViewModel,
                            onBack = { screen = Screen.Progress },
                        )
                    }

                    Screen.TrainingTrend -> {
                        val trendViewModel: TrainingTrendViewModel = viewModel(
                            key = "training-trend-$dataEpoch",
                            factory = TrainingTrendViewModel.factory(
                                app.workoutRepository,
                                app.exerciseRepository,
                                app.bodyRecordRepository,
                                workingPrMinRepsProvider = {
                                    app.trainingPreferencesStore.workingPrMinRepsValue()
                                },
                            ),
                        )
                        TrainingTrendScreen(
                            viewModel = trendViewModel,
                            onBack = { screen = Screen.Progress },
                        )
                    }

                    is Screen.SessionEditor -> {
                        val editorViewModel: SessionEditorViewModel = viewModel(
                            key = "${current.draftKey}-$dataEpoch",
                            factory = SessionEditorViewModel.factory(
                                app.workoutRepository,
                                app.bodyRecordRepository,
                                current.sessionId,
                                current.dateEpochDay,
                                app.photoWallRepository,
                                app.timerRecordRepository,
                                draftStore = app.sessionDraftStore,
                                draftKey = current.draftKey,
                                timerAlarmScheduler = app.timerAlarmScheduler,
                            ),
                        )
                        SessionEditorScreen(
                            viewModel = editorViewModel,
                            exerciseMedia = exerciseMedia,
                            onBack = { screen = current.returnTab.toScreen() },
                            onExitAfterSave = {
                                if (activeDraft?.draftKey == current.draftKey) {
                                    activeDraft = null
                                    ActiveWorkoutNotifier.clear(app, current.draftKey)
                                }
                                screen = current.returnTab.toScreen()
                            },
                            onAddExercise = { screen = Screen.PickExercise(current) },
                        )
                    }

                    is Screen.Settings -> {
                        AppSettingsScreen(
                            onBack = { screen = current.from.toScreen() },
                            onOpenAbout = { screen = Screen.About(current.from) },
                            onOpenBackup = { screen = Screen.BackupData(current.from) },
                            onOpenAppearance = {
                                screen = Screen.Appearance(current.from)
                            },
                            onOpenNotifications = {
                                screen = Screen.NotificationSettings(current.from)
                            },
                            onOpenTrainingPreferences = {
                                screen = Screen.TrainingPreferences(current.from)
                            },
                        )
                    }

                    is Screen.Appearance -> {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            key = "app-settings",
                            factory = SettingsViewModel.factory(app),
                        )
                        AppearanceSettingsScreen(
                            viewModel = settingsViewModel,
                            calendarColorStore = app.calendarColorStore,
                            onBack = { screen = Screen.Settings(current.from) },
                            onOpenCalendarColors = {
                                screen = Screen.CalendarColors(current.from)
                            },
                        )
                    }

                    is Screen.CalendarColors -> {
                        CalendarColorSettingsScreen(
                            store = app.calendarColorStore,
                            onBack = { screen = Screen.Appearance(current.from) },
                            onOpenCustom = {
                                screen = Screen.CalendarCustomColors(current.from)
                            },
                        )
                    }

                    is Screen.CalendarCustomColors -> {
                        CalendarCustomColorsScreen(
                            store = app.calendarColorStore,
                            onBack = { screen = Screen.CalendarColors(current.from) },
                        )
                    }

                    is Screen.NotificationSettings -> {
                        NotificationSettingsScreen(
                            store = app.notificationSettingsStore,
                            onBack = { screen = Screen.Settings(current.from) },
                            onAlertsEnabledChange = { enabled ->
                                activeEditorViewModel?.setCountdownAlertsEnabled(enabled)
                                if (!enabled) {
                                    app.timerAlarmScheduler.cancel()
                                    app.timerAlarmScheduler.cancelAlerts()
                                }
                            },
                        )
                    }

                    is Screen.TrainingPreferences -> {
                        val trainingPreferencesViewModel: TrainingPreferencesViewModel = viewModel(
                            key = "training-preferences",
                            factory = TrainingPreferencesViewModel.factory(
                                app.trainingPreferencesStore,
                                app.workoutRepository,
                            ),
                        )
                        TrainingPreferencesScreen(
                            viewModel = trainingPreferencesViewModel,
                            onBack = { screen = Screen.Settings(current.from) },
                        )
                    }

                    is Screen.BackupData -> {
                        val transferViewModel: DataTransferViewModel = viewModel(
                            key = "data-transfer",
                            factory = DataTransferViewModel.factory(app),
                        )
                        val clearSettingsViewModel: SettingsViewModel = viewModel(
                            key = "app-settings",
                            factory = SettingsViewModel.factory(app),
                        )
                        BackupDataScreen(
                            viewModel = transferViewModel,
                            clearViewModel = clearSettingsViewModel,
                            onBack = { screen = Screen.Settings(current.from) },
                        )
                    }

                    is Screen.TimerHistory -> {
                        TimerHistoryScreen(
                            viewModel = timerViewModel,
                            onBack = { screen = current.from.toScreen() },
                            onOpenSaved = { recordId ->
                                screen = Screen.SavedTimer(
                                    recordId = recordId,
                                    returnTab = current.from,
                                    returnToTimerHistory = true,
                                )
                            },
                        )
                    }

                    is Screen.About -> {
                        AboutScreen(
                            onBack = {
                                screen = if (current.openedFromSettings) {
                                    Screen.Settings(current.from)
                                } else {
                                    current.from.toScreen()
                                }
                            },
                            onOpenReleaseNotes = {
                                screen = Screen.ReleaseNotes(current.from, current.openedFromSettings)
                            },
                        )
                    }

                    is Screen.ReleaseNotes -> {
                        ReleaseNotesScreen(
                            onBack = { screen = Screen.About(current.from, current.openedFromSettings) },
                        )
                    }

                    Screen.TrainingTimer -> {
                        TrainingTimerScreen(
                            viewModel = timerViewModel,
                            selectedDateEpochDay = selectedDate.toEpochDay(),
                            onBack = { screen = Screen.Calendar },
                        )
                    }

                    is Screen.SavedTimer -> {
                        SavedTimerScreen(
                            viewModel = timerViewModel,
                            recordId = current.recordId,
                            onBack = {
                                screen = if (current.returnToTimerHistory) {
                                    Screen.TimerHistory(current.returnTab)
                                } else {
                                    current.returnTab.toScreen()
                                }
                            },
                        )
                    }

                    Screen.ExerciseLibrary -> {
                        val libraryViewModel: ExerciseLibraryViewModel = viewModel(
                            key = "library-$dataEpoch",
                            factory = ExerciseLibraryViewModel.factory(
                            app.exerciseRepository,
                            app.exerciseMediaRepository,
                        ),
                        )
                        ExerciseLibraryScreen(
                            viewModel = libraryViewModel,
                            pickerMode = false,
                            onBack = { screen = Screen.Train },
                            onPick = {},
                            showBack = true,
                            onOpenDetail = { exerciseId ->
                                screen = Screen.ExerciseDetail(exerciseId)
                            },
                        )
                    }

                    is Screen.ExerciseDetail -> {
                        val detailViewModel: ExerciseDetailViewModel = viewModel(
                            key = "exercise-detail-${current.exerciseId}",
                            factory = ExerciseDetailViewModel.factory(
                                exerciseId = current.exerciseId,
                                repository = app.exerciseRepository,
                                mediaRepository = app.exerciseMediaRepository,
                                bodyRecordRepository = app.bodyRecordRepository,
                            ),
                        )
                        ExerciseDetailScreen(
                            viewModel = detailViewModel,
                            onBack = { screen = Screen.ExerciseLibrary },
                        )
                    }

                    is Screen.PickExercise -> {
                        val libraryViewModel: ExerciseLibraryViewModel = viewModel(
                            // Scope the picker to the training draft so a previous
                            // library or picker search does not leak into a new workout.
                            key = "picker-${current.returnTo.draftKey}-$dataEpoch",
                            factory = ExerciseLibraryViewModel.factory(
                            app.exerciseRepository,
                            app.exerciseMediaRepository,
                        ),
                        )
                        val editorViewModel: SessionEditorViewModel = viewModel(
                            key = "${current.returnTo.draftKey}-$dataEpoch",
                            factory = SessionEditorViewModel.factory(
                                app.workoutRepository,
                                app.bodyRecordRepository,
                                current.returnTo.sessionId,
                                current.returnTo.dateEpochDay,
                                draftStore = app.sessionDraftStore,
                                draftKey = current.returnTo.draftKey,
                                timerAlarmScheduler = app.timerAlarmScheduler,
                            ),
                        )
                        ExerciseLibraryScreen(
                            viewModel = libraryViewModel,
                            pickerMode = true,
                            onBack = { screen = current.returnTo },
                            onPick = { exercise ->
                                editorViewModel.addExercise(exercise)
                                screen = current.returnTo
                            },
                        )
                    }
                }
            }
        }
    }

    if (confirmDiscardActive) {
        AlertDialog(
            onDismissRequest = { confirmDiscardActive = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.training_discard_title)) },
            text = { Text(stringResource(R.string.training_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscardActive = false
                        activeEditorViewModel?.discardTraining()
                        activeDraft = null
                    },
                ) {
                    Text(
                        stringResource(R.string.training_discard),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscardActive = false }) {
                    Text(stringResource(R.string.training_resume))
                }
            },
        )
    }
}

@Composable
private fun TabPane(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val layer = if (visible) {
        Modifier.fillMaxSize().zIndex(1f)
    } else {
        Modifier
            .zIndex(0f)
            .layout { measurable, constraints ->
                measurable.measure(constraints)
                layout(0, 0) {}
            }
            .clearAndSetSemantics {}
    }
    Box(layer) {
        content()
    }
}

@Composable
private fun RootTabBar(
    selected: RootTab,
    onSelect: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = selected == RootTab.Home,
            onClick = { onSelect(RootTab.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_home)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == RootTab.Calendar,
            onClick = { onSelect(RootTab.Calendar) },
            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_calendar)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == RootTab.Train,
            onClick = { onSelect(RootTab.Train) },
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_train)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == RootTab.Progress,
            onClick = { onSelect(RootTab.Progress) },
            icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_progress)) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = selected == RootTab.Mine,
            onClick = { onSelect(RootTab.Mine) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.tab_mine)) },
            colors = itemColors,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(role = Role.Button, onClick = onOpenAbout)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                SettingsTopBarAction(onOpenSettings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTopBar(
    onToday: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .heightIn(min = 56.dp)
                .padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tab_calendar),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.calendar_today))
            }
            SettingsTopBarAction(onOpenSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HistoryScreen(
    sessions: List<SessionDetail>,
    exerciseMedia: Map<String, ExerciseMediaView>,
    measurements: List<BodyMeasurementEntity>,
    calendarMonth: YearMonth,
    selectedDate: LocalDate,
    onCalendarMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onEdit: (SessionDetail) -> Unit,
    onOpenSettings: () -> Unit,
    dayNotes: Map<Long, String>,
    dayKinds: Map<Long, DayKind>,
    dayPaints: Map<Long, CalendarDayPaint>,
    onSaveDayNote: (LocalDate, String, String) -> Unit,
    onSelectDayKind: (LocalDate, DayKind, Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    timer: TrainingTimerUiState,
    onOpenSavedTimer: (String) -> Unit,
) {
    val daySessions = remember(sessions, selectedDate, measurements) {
        sessions.filter { it.session.dateEpochDay == selectedDate.toEpochDay() }
            .orderedForDay(measurements)
    }
    val dayTimers = timer.saved.filter { it.record.dateEpochDay == selectedDate.toEpochDay() }
    val resolvedDayKinds = remember(sessions, dayKinds) {
        DayKindLogic.resolveAll(sessions, dayKinds)
    }
    val hasTraining = remember(daySessions) {
        daySessions.any { detail -> detail.exercises.any { it.sets.isNotEmpty() } }
    }
    val storedKind = dayKinds[selectedDate.toEpochDay()]
    val defaultDayMessage = stringResource(defaultDayNoteRes(storedKind))
    val dayMessage = dayNotes[selectedDate.toEpochDay()] ?: defaultDayMessage
    var editingDayNote by remember { mutableStateOf(false) }
    var editingDayKind by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CalendarTopBar(
                onToday = {
                    onCalendarMonthChange(YearMonth.now())
                    onDayClick(LocalDate.now())
                },
                onOpenSettings = onOpenSettings,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "training-calendar") {
                CalendarMonthOverview(
                    month = calendarMonth,
                    sessions = sessions,
                    measurements = measurements,
                    dayPaints = dayPaints,
                    selectedDate = selectedDate,
                    onMonthChange = onCalendarMonthChange,
                    onDayClick = onDayClick,
                    onOpenFullMonth = onOpenCalendar,
                )
            }
            item(key = "selected-date-header") {
                val partLine = daySessions.partVolumes(measurements)
                    .map { (part, _) -> part.removeSuffix("部") }
                    .distinct()
                    .joinToString("+")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedDate.format(dateFormatter),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (partLine.isNotEmpty()) {
                                Text(
                                    text = partLine,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(6.dp),
                                )
                                .clickable { editingDayKind = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (hasTraining) R.string.calendar_training else R.string.calendar_untrained,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val totalDayDuration = dayTimers.sumOf { it.record.durationMillis }
                    if (totalDayDuration > 0L) {
                        val dayDurationLabel = stringResource(R.string.timer_day_duration_label)
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append(dayDurationLabel)
                                }
                                append(TrainingTimerLogic.formatDurationChinese(totalDayDuration))
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontFeatureSettings = "tnum",
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (dayTimers.size == 1) {
                                        Modifier.clickable {
                                            onOpenSavedTimer(dayTimers.first().record.id)
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
            }
            if (daySessions.isEmpty() || storedKind.isSpecialDay()) {
                item(key = "empty-day") {
                    GymLogCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingDayNote = true },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GymLogSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        if (selectedDate == LocalDate.now()) {
                                            R.string.home_empty_title_today
                                        } else {
                                            R.string.home_empty_title_other
                                        },
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = dayMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (daySessions.isNotEmpty()) {
                items(daySessions, key = { it.session.id }) { session ->
                    SessionCard(
                        detail = session,
                        exerciseMedia = exerciseMedia,
                        measurements = measurements,
                        onClick = { onEdit(session) },
                    )
                }
            }
        }
    }
    if (editingDayNote) {
        DayNoteDialog(
            initialText = dayMessage,
            onDismiss = { editingDayNote = false },
            onSave = { text ->
                onSaveDayNote(selectedDate, text, defaultDayMessage)
                editingDayNote = false
            },
        )
    }
    if (editingDayKind) {
        AlertDialog(
            onDismissRequest = { editingDayKind = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.calendar_mark_day),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                DayKindSelector(
                    selected = resolvedDayKinds[selectedDate.toEpochDay()],
                    onSelect = { kind ->
                        onSelectDayKind(selectedDate, kind, hasTraining)
                        editingDayKind = false
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { editingDayKind = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DayNoteDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.empty_day_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SessionCard(
    detail: SessionDetail,
    exerciseMedia: Map<String, ExerciseMediaView>,
    measurements: List<BodyMeasurementEntity>,
    onClick: () -> Unit,
) {
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = GymLogSpacing.md, end = 0.dp, top = GymLogSpacing.sm, bottom = GymLogSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
        ) {
            if (detail.exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_session_exercises),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                detail.exercises.forEach { item ->
                    SessionExerciseRow(
                        item = item,
                        dateEpochDay = detail.session.dateEpochDay,
                        measurements = measurements,
                        media = exerciseMedia[item.exercise.id],
                    )
                }
            }
            detail.session.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SessionExerciseRow(
    item: SessionExercise,
    dateEpochDay: Long,
    measurements: List<BodyMeasurementEntity>,
    media: ExerciseMediaView?,
) {
    val volume = WeightLogic.totalVolume(
        item.sets.map { set ->
            resolvedLoad(set.weightMode, set.weightKg, dateEpochDay, measurements) to set.reps
        },
    )
    val setCells = item.sets.map { set ->
        val load = resolvedLoad(set.weightMode, set.weightKg, dateEpochDay, measurements)
        "${WeightLogic.displaySet(load)}×${set.reps ?: 0}"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ExerciseClassification.partLabel(
                    ExerciseClassification.exerciseGroupingKey(item.exercise),
                ),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.exercise.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.volume_label,
                    WeightLogic.displayVolume(volume),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.durationMillis?.let { duration ->
                Text(
                    text = stringResource(
                        R.string.training_action_duration,
                        TrainingTimerLogic.formatDurationChinese(duration),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        ExerciseThumbnail(
            exercise = item.exercise,
            media = media,
            modifier = Modifier
                .padding(horizontal = 0.dp)
                .size(64.dp),
        )
        SessionSetGrid(
            cells = sessionSetGridCells(setCells),
            modifier = Modifier.width(164.dp),
        )
    }
}

internal fun sessionSetGridCells(lines: List<String>): List<String> {
    if (lines.size <= SESSION_SET_GRID_CAPACITY) return lines
    return lines.take(SESSION_SET_GRID_CAPACITY - 1) + SESSION_SET_GRID_MORE
}

private const val SESSION_SET_GRID_COLUMNS = 2
private const val SESSION_SET_GRID_CAPACITY = SESSION_SET_GRID_COLUMNS * 3
private const val SESSION_SET_GRID_MORE = "…"

@Composable
private fun SessionSetGrid(
    cells: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(SESSION_SET_GRID_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                repeat(SESSION_SET_GRID_COLUMNS) { index ->
                    Box(modifier = Modifier.weight(1f)) {
                        row.getOrNull(index)?.let { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFeatureSettings = "tnum",
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DayKind?.isSpecialDay(): Boolean {
    return this == DayKind.REST || this == DayKind.INDULGE || this == DayKind.LAZY
}

private fun defaultDayNoteRes(kind: DayKind?): Int {
    return when (kind) {
        DayKind.REST -> R.string.day_note_rest
        DayKind.INDULGE -> R.string.day_note_indulge
        DayKind.LAZY -> R.string.day_note_lazy
        else -> R.string.empty_day
    }
}

private fun resolvedLoad(
    weightMode: String,
    storedKg: Double?,
    dateEpochDay: Long,
    measurements: List<BodyMeasurementEntity>,
): Double {
    return WeightLogic.effectiveKg(
        WeightMode.fromStorage(weightMode),
        storedKg,
        WeightLogic.bodyWeightKg(measurements, dateEpochDay),
    )
}
