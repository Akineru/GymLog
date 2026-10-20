package com.example.gymlog.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.gymlog.data.CalendarDayPaint
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
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.displayName
import com.example.gymlog.data.orderedForDay
import com.example.gymlog.data.partVolumes
import com.example.gymlog.ui.home.HomeScreen
import com.example.gymlog.ui.body.BodyRecordHistory
import com.example.gymlog.ui.body.BodyRecordScreen
import com.example.gymlog.ui.muscle.MuscleMapScreen
import com.example.gymlog.ui.muscle.MuscleMapViewModel
import com.example.gymlog.ui.body.BodyRecordViewModel
import com.example.gymlog.ui.calendar.DayKindSelector
import com.example.gymlog.ui.calendar.TrainingCalendarCard
import com.example.gymlog.ui.calendar.TrainingCalendarDetailScreen
import com.example.gymlog.ui.exercise.ExerciseLibraryScreen
import com.example.gymlog.ui.exercise.ExerciseLibraryViewModel
import com.example.gymlog.ui.exercise.ExerciseThumbnail
import com.example.gymlog.ui.photo.PhotoDetailScreen
import com.example.gymlog.ui.photo.PhotoEditorScreen
import com.example.gymlog.ui.photo.PhotoWallScreen
import com.example.gymlog.ui.photo.PhotoWallViewModel
import com.example.gymlog.ui.progress.ProgressScreen
import com.example.gymlog.ui.train.ActiveWorkoutUi
import com.example.gymlog.ui.train.TrainingHomeScreen
import com.example.gymlog.ui.session.SessionEditorScreen
import com.example.gymlog.ui.session.SessionEditorViewModel
import com.example.gymlog.ui.settings.AboutScreen
import com.example.gymlog.ui.settings.MineViewModel
import com.example.gymlog.ui.settings.ReleaseNotesScreen
import com.example.gymlog.ui.settings.AppSettingsScreen
import com.example.gymlog.ui.settings.BackupDataScreen
import com.example.gymlog.ui.settings.DataTransferViewModel
import com.example.gymlog.ui.settings.MineScreen
import com.example.gymlog.ui.settings.SettingsHistoryScreen
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
    data class BackupData(val from: RootTab) : Screen
    data class SettingsHistory(val from: RootTab) : Screen
    data class TimerHistory(val from: RootTab) : Screen
    data class BodyHistory(val from: RootTab) : Screen
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

private val Screen.isRoot: Boolean
    get() = this is Screen.Home || this is Screen.Calendar || this is Screen.Train ||
        this is Screen.Progress || this is Screen.Mine

private fun Screen.toRootTab(): RootTab = when (this) {
    Screen.Home -> RootTab.Home
    Screen.Calendar, Screen.CalendarExpanded, Screen.TrainingTimer, is Screen.SavedTimer, is Screen.SessionEditor, is Screen.PickExercise -> RootTab.Calendar
    Screen.Train, Screen.ExerciseLibrary -> RootTab.Train
    Screen.Progress, Screen.TrainingTrend, Screen.BodyTrend -> RootTab.Progress
    Screen.Mine, is Screen.BodyRecord, is Screen.MuscleMap, Screen.PhotoWall, is Screen.PhotoDetail, Screen.PhotoEditor -> RootTab.Mine
    is Screen.Settings -> from
    is Screen.BackupData -> from
    is Screen.SettingsHistory -> from
    is Screen.TimerHistory -> from
    is Screen.BodyHistory -> from
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
fun GymLogApp(viewModel: GymLogViewModel = viewModel()) {
    val app = LocalContext.current.applicationContext as GymLogApplication
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val exerciseMedia by viewModel.exerciseMedia.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val partsByDate by viewModel.partsByDate.collectAsStateWithLifecycle()
    val dayNotes by viewModel.dayNotes.collectAsStateWithLifecycle()
    val dayKinds by viewModel.dayKinds.collectAsStateWithLifecycle()
    val dayPaints = remember(sessions, measurements, dayKinds) {
        CalendarDayColorLogic.paintsFor(sessions, measurements, dayKinds)
    }
    val dataEpoch by app.backupRepository.dataEpoch.collectAsStateWithLifecycle()
    val photoViewModel: PhotoWallViewModel = viewModel(factory = PhotoWallViewModel.factory(app))
    val timerViewModel: TrainingTimerViewModel = viewModel(
        factory = TrainingTimerViewModel.factory(app),
    )
    val timerState by timerViewModel.uiState.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var activeDraft by remember { mutableStateOf<Screen.SessionEditor?>(null) }
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
            ),
        )
    }
    val activeEditorState = if (activeEditorViewModel != null) {
        activeEditorViewModel.state.collectAsStateWithLifecycle().value
    } else {
        null
    }
    val activeWorkout = activeEditorState?.let { state ->
        ActiveWorkoutUi(
            durationMillis = state.totalElapsedMillis,
            exerciseNames = state.exercises.map { it.exercise.displayName() },
        )
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
                        viewModel.onCalendarDayClick(LocalDate.now())
                        screen = Screen.Calendar
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
                    partsByDate = partsByDate,
                    onCalendarMonthChange = viewModel::onCalendarMonthChange,
                    onDayClick = viewModel::onCalendarDayClick,
                    onAdd = {
                        screen = Screen.SessionEditor(
                            sessionId = null,
                            dateEpochDay = selectedDate.toEpochDay(),
                        )
                    },
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
                    onOpenAbout = { screen = Screen.About(RootTab.Calendar, openedFromSettings = false) },
                )
            }
            TabPane(visible = isRoot && screen.toRootTab() == RootTab.Train) {
                TrainingHomeScreen(
                    activeWorkout = activeWorkout,
                    onResumeWorkout = {
                        activeDraft?.let { screen = it }
                    },
                    onDiscardWorkout = {
                        activeEditorViewModel?.discardTraining()
                        activeDraft = null
                    },
                    onStartTraining = {
                        val draft = activeDraft ?: Screen.SessionEditor(
                            sessionId = null,
                            returnTab = RootTab.Train,
                        ).also { activeDraft = it }
                        screen = draft
                    },
                    onOpenLibrary = { screen = Screen.ExerciseLibrary },
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
            RootTabBar(
                selected = screen.toRootTab(),
                onSelect = { screen = it.toScreen() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        measuredBarHeight = with(density) { size.height.toDp() }
                    },
            )
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
                            sessions = sessions,
                            dayNotes = dayNotes,
                            dayPaints = dayPaints,
                            selectedDate = selectedDate,
                            onMonthChange = viewModel::onCalendarMonthChange,
                            onDayClick = { date ->
                                viewModel.onCalendarDayClick(date)
                                screen = Screen.Calendar
                            },
                            onBack = { screen = Screen.Calendar },
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
                            ),
                        )
                        SessionEditorScreen(
                            viewModel = editorViewModel,
                            exerciseMedia = exerciseMedia,
                            onBack = { screen = current.returnTab.toScreen() },
                            onExitAfterSave = {
                                if (activeDraft?.draftKey == current.draftKey) {
                                    activeDraft = null
                                }
                                screen = current.returnTab.toScreen()
                            },
                            onAddExercise = { screen = Screen.PickExercise(current) },
                        )
                    }

                    is Screen.Settings -> {
                        val settingsViewModel: SettingsViewModel = viewModel(
                            key = "app-settings",
                            factory = SettingsViewModel.factory(app),
                        )
                        AppSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { screen = current.from.toScreen() },
                            onOpenAbout = { screen = Screen.About(current.from) },
                            onOpenBackup = { screen = Screen.BackupData(current.from) },
                            onOpenHistory = { screen = Screen.SettingsHistory(current.from) },
                        )
                    }

                    is Screen.BackupData -> {
                        val transferViewModel: DataTransferViewModel = viewModel(
                            key = "data-transfer",
                            factory = DataTransferViewModel.factory(app),
                        )
                        BackupDataScreen(
                            viewModel = transferViewModel,
                            onBack = { screen = Screen.Settings(current.from) },
                        )
                    }

                    is Screen.SettingsHistory -> {
                        SettingsHistoryScreen(
                            onBack = { screen = Screen.Settings(current.from) },
                            onOpenTimers = { screen = Screen.TimerHistory(current.from) },
                            onOpenBody = { screen = Screen.BodyHistory(current.from) },
                        )
                    }

                    is Screen.TimerHistory -> {
                        TimerHistoryScreen(
                            viewModel = timerViewModel,
                            onBack = { screen = Screen.SettingsHistory(current.from) },
                            onOpenSaved = { recordId ->
                                screen = Screen.SavedTimer(
                                    recordId = recordId,
                                    returnTab = current.from,
                                    returnToTimerHistory = true,
                                )
                            },
                        )
                    }

                    is Screen.BodyHistory -> {
                        val bodyViewModel: BodyRecordViewModel = viewModel(
                            key = "body-$dataEpoch",
                            factory = BodyRecordViewModel.factory(
                                app.bodyRecordRepository,
                                app.workoutRepository,
                            ),
                        )
                        BodyRecordHistory(
                            viewModel = bodyViewModel,
                            onBack = { screen = Screen.SettingsHistory(current.from) },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HistoryScreen(
    sessions: List<SessionDetail>,
    exerciseMedia: Map<String, ExerciseMediaView>,
    measurements: List<BodyMeasurementEntity>,
    calendarMonth: YearMonth,
    selectedDate: LocalDate,
    partsByDate: Map<LocalDate, List<String>>,
    onCalendarMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onAdd: () -> Unit,
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
    onOpenAbout: () -> Unit,
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
    var calendarCollapsed by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            HomeTopBar(
                onOpenSettings = onOpenSettings,
                onOpenAbout = onOpenAbout,
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_record),
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "training-calendar") {
                TrainingCalendarCard(
                    month = calendarMonth,
                    partsByDate = partsByDate,
                    selectedDate = selectedDate,
                    dayPaints = dayPaints,
                    collapsed = calendarCollapsed,
                    onToggleCollapse = { calendarCollapsed = !calendarCollapsed },
                    onMonthChange = onCalendarMonthChange,
                    onDayClick = onDayClick,
                    onOpen = onOpenCalendar,
                )
            }
            item(key = "selected-date-header") {
                val partLine = daySessions.partVolumes(measurements).joinToString("　") { (part, volume) ->
                    "$part：${WeightLogic.displayVolume(volume)}"
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectedDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (partLine.isNotEmpty()) {
                            Text(
                                text = partLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f),
                            )
                        }
                    }
                    DayKindSelector(
                        selected = resolvedDayKinds[selectedDate.toEpochDay()],
                        onSelect = { kind -> onSelectDayKind(selectedDate, kind, hasTraining) },
                    )
                    dayTimers.forEach { saved ->
                        Text(
                            text = stringResource(
                                R.string.timer_day_duration,
                                TrainingTimerLogic.formatDurationChinese(saved.record.durationMillis),
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSavedTimer(saved.record.id) },
                        )
                    }
                }
            }
            if (daySessions.isEmpty() || storedKind.isSpecialDay()) {
                item(key = "empty-day") {
                    Text(
                        text = dayMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.clickable { editingDayNote = true },
                    )
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
        title = { Text(stringResource(R.string.empty_day_edit_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 0.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
