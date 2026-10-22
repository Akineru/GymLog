package com.example.gymlog.ui.session

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WorkoutClockLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.displayName
import com.example.gymlog.ui.exercise.ExerciseThumbnail
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSecondaryButton
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorScreen(
    viewModel: SessionEditorViewModel,
    exerciseMedia: Map<String, ExerciseMediaView>,
    onBack: () -> Unit,
    onExitAfterSave: () -> Unit,
    onAddExercise: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showClock by remember { mutableStateOf(false) }
    var showTotalEditor by remember { mutableStateOf(false) }
    var showClockEditor by remember { mutableStateOf(false) }
    var clockWasRunning by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.stagePhoto(uri)
    }
    BackHandler(enabled = confirmDelete) { confirmDelete = false }
    BackHandler(enabled = showDatePicker) { showDatePicker = false }
    BackHandler(enabled = !showDatePicker && !confirmDelete && !showClock) {
        if (state.finishing) viewModel.cancelFinish() else onBack()
    }

    LaunchedEffect(viewModel) {
        viewModel.exitAfterSave.collect { onExitAfterSave() }
    }
    LaunchedEffect(Unit) {
        viewModel.clockFinished.collect { vibrateOnce(context) }
    }

    if (state.finishing) {
        TrainingSavePage(
            state = state,
            onBack = viewModel::cancelFinish,
            onNoteChange = viewModel::onNoteChange,
            onDateClick = { showDatePicker = true },
            onPickPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemovePhoto = viewModel::removePhoto,
            onSave = viewModel::saveFinal,
            onDiscard = { confirmDelete = true },
            modifier = Modifier.imePadding(),
        )
    } else {
        TrainingSessionPage(
            state = state,
            exerciseMedia = exerciseMedia,
            onBack = onBack,
            onFinish = viewModel::finish,
            onEditDuration = {
                viewModel.pauseTotalTimer()
                showTotalEditor = true
            },
            onOpenClock = { showClock = true },
            onAddExercise = onAddExercise,
            onRequestBodyWeight = { showWeightDialog = true },
            onRemoveExercise = viewModel::removeExercise,
            onAddSet = viewModel::addSet,
            onRemoveSet = viewModel::removeSet,
            onChangeMode = viewModel::changeMode,
            onUpdateSet = viewModel::updateSet,
            onToggleSet = viewModel::toggleSetCompleted,
            onToggleExerciseCollapsed = viewModel::toggleExerciseCollapsed,
            onFinishExercise = viewModel::finishExercise,
            modifier = Modifier.imePadding(),
        )
    }

    if (showWeightDialog) {
        BodyWeightDialog(
            onDismiss = { showWeightDialog = false },
            onConfirm = { weight ->
                viewModel.saveBodyWeight(weight)
                showWeightDialog = false
            },
        )
    }

    if (showClock) {
        WorkoutClockSheet(
            clock = clock,
            onDismiss = { showClock = false },
            onModeChange = viewModel::setClockMode,
            onToggle = viewModel::toggleClock,
            onReset = viewModel::resetClock,
            onAdjust = viewModel::adjustTimer,
            onEditTime = {
                clockWasRunning = clock.running
                viewModel.pauseClockForEdit()
                showClockEditor = true
            },
        )
    }

    if (showTotalEditor) {
        DurationWheelDialog(
            title = stringResource(R.string.training_edit_duration),
            initialMillis = state.totalElapsedMillis,
            showHours = true,
            onDismiss = {
                showTotalEditor = false
                viewModel.resumeTotalTimer()
            },
            onConfirm = { millis ->
                viewModel.setTotalDuration(millis)
                showTotalEditor = false
            },
        )
    }

    if (showClockEditor) {
        DurationWheelDialog(
            title = stringResource(R.string.clock_edit_time),
            initialMillis = clock.displayMillis,
            showHours = false,
            onDismiss = {
                showClockEditor = false
                if (clockWasRunning) viewModel.toggleClock()
            },
            onConfirm = { millis ->
                viewModel.setTimerDuration(millis)
                showClockEditor = false
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(state.dateEpochDay)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val epochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                                .toEpochDay()
                            viewModel.onDateChange(epochDay)
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    stringResource(
                        if (state.sessionId == null) {
                            R.string.training_discard_title
                        } else {
                            R.string.training_delete_title
                        },
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (state.sessionId == null) {
                            R.string.training_discard_message
                        } else {
                            R.string.training_delete_message
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.discardTraining()
                    },
                ) {
                    Text(
                        stringResource(
                            if (state.sessionId == null) R.string.training_discard else R.string.training_delete,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.training_resume))
                }
            },
        )
    }
}

@Composable
private fun TrainingSessionPage(
    state: SessionEditorUiState,
    exerciseMedia: Map<String, ExerciseMediaView>,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onEditDuration: () -> Unit,
    onOpenClock: () -> Unit,
    onAddExercise: () -> Unit,
    onRequestBodyWeight: () -> Unit,
    onRemoveExercise: (String) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, String) -> Unit,
    onChangeMode: (String, String, WeightMode) -> Unit,
    onUpdateSet: (String, String, (DraftSetUi) -> DraftSetUi) -> Unit,
    onToggleSet: (String, String) -> Unit,
    onToggleExerciseCollapsed: (String) -> Unit,
    onFinishExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingRemove by remember { mutableStateOf<DraftExerciseUi?>(null) }
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TrainingTopBar(
                elapsedMillis = state.totalElapsedMillis,
                onBack = onBack,
                onOpenClock = onOpenClock,
                onFinish = onFinish,
                onEditDuration = onEditDuration,
                enabled = !state.loading,
            )
            TrainingSummary(state)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.exercises.isEmpty()) {
                    item {
                        EmptyTrainingState(onAddExercise)
                    }
                }
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseBlock(
                        item = exercise,
                        media = exerciseMedia[exercise.exercise.id],
                        bodyWeightKg = state.bodyWeightKg,
                        onRemoveExercise = { pendingRemove = exercise },
                        onAddSet = { onAddSet(exercise.id) },
                        onRemoveSet = { setId -> onRemoveSet(exercise.id, setId) },
                        onChangeMode = { setId, mode -> onChangeMode(exercise.id, setId, mode) },
                        onUpdateSet = { setId, transform -> onUpdateSet(exercise.id, setId, transform) },
                        onToggleSet = { setId -> onToggleSet(exercise.id, setId) },
                        onRequestBodyWeight = onRequestBodyWeight,
                        onToggleExpanded = { onToggleExerciseCollapsed(exercise.id) },
                        onFinishExercise = { onFinishExercise(exercise.id) },
                        collapsed = exercise.id in state.collapsedExerciseIds,
                    )
                }
                if (state.exercises.isNotEmpty()) {
                    item {
                        GymLogSecondaryButton(
                            text = stringResource(R.string.training_add_exercise),
                            onClick = onAddExercise,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                state.errorMessage?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    pendingRemove?.let { exercise ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text(stringResource(R.string.exercise_delete_title)) },
            text = { Text(stringResource(R.string.exercise_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRemove = null
                        onRemoveExercise(exercise.id)
                    },
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TrainingTopBar(
    elapsedMillis: Long,
    onBack: () -> Unit,
    onOpenClock: () -> Unit,
    onFinish: () -> Unit,
    onEditDuration: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cancel),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditDuration),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.training_duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = TrainingTimerLogic.formatDuration(elapsedMillis),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
        IconButton(onClick = onOpenClock, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = stringResource(R.string.training_timer),
            )
        }
        GymLogActionButton(
            text = stringResource(R.string.training_complete),
            onClick = onFinish,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun TrainingSummary(state: SessionEditorUiState) {
    val completedSets = state.exercises.flatMap { it.sets }.filter { it.completed }
    val volume = WeightLogic.totalVolume(
        completedSets.map { set -> draftWeightKg(set, state.bodyWeightKg) to set.repsText.trim().toIntOrNull() },
    )
    val completedExercises = state.exercises.size
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GymLogSpacing.md, vertical = GymLogSpacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymLogSpacing.md, vertical = GymLogSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryMetric(
                label = stringResource(R.string.training_volume),
                value = WeightLogic.displayVolume(volume),
                modifier = Modifier.weight(1.3f),
            )
            SummaryMetric(
                label = stringResource(R.string.training_exercises),
                value = completedExercises.toString(),
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = stringResource(R.string.training_sets),
                value = completedSets.size.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SaveSummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBBD0EC),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = "tnum",
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun SaveSectionHeader(title: String, optional: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (optional) {
            Text(
                text = stringResource(R.string.training_save_optional),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun EmptyTrainingState(onAddExercise: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = stringResource(R.string.training_start_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.training_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        GymLogActionButton(
            text = stringResource(R.string.training_add_exercise),
            onClick = onAddExercise,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Filled.Add,
        )
    }
}

@Composable
private fun CollapsedExerciseCard(
    item: DraftExerciseUi,
    media: ExerciseMediaView?,
    bodyWeightKg: Double,
    volume: Double,
    onClick: () -> Unit,
) {
    val cells = item.sets
        .filter { it.completed }
        .take(6)
        .map { set ->
            "${WeightLogic.displaySet(draftWeightKg(set, bodyWeightKg))}×${set.repsText}"
        }
    val hidden = item.sets.count { it.completed } > 6
    val finished = item.completedAtMillis != null || item.durationMillis != null
    val dark = isSystemInDarkTheme()
    val finishedBackground = if (dark) Color(0xFF1B3A2A) else Color(0xFFE6F4EA)
    val finishedBorder = Color(0xFF81C784)
    val finishedInk = if (dark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        containerColor = if (finished) {
            finishedBackground
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            1.dp,
            if (finished) finishedBorder else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GymLogSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExerciseClassification.partLabel(
                        ExerciseClassification.exerciseGroupingKey(item.exercise),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (finished) finishedInk else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.volume_label,
                        WeightLogic.displayVolume(volume),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = actionDurationText(item.durationMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExerciseThumbnail(
                exercise = item.exercise,
                media = media,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(48.dp),
            )
            Column(
                modifier = Modifier.width(124.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (cells.isEmpty()) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    cells.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (hidden) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(
    item: DraftExerciseUi,
    media: ExerciseMediaView?,
    bodyWeightKg: Double,
    onRemoveExercise: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit,
    onChangeMode: (String, WeightMode) -> Unit,
    onUpdateSet: (String, (DraftSetUi) -> DraftSetUi) -> Unit,
    onToggleSet: (String) -> Unit,
    onRequestBodyWeight: () -> Unit,
    onToggleExpanded: () -> Unit,
    onFinishExercise: () -> Unit,
    collapsed: Boolean,
) {
    val completed = item.sets.filter { it.completed }
    val volume = WeightLogic.totalVolume(
        completed.map { set -> draftWeightKg(set, bodyWeightKg) to set.repsText.trim().toIntOrNull() },
    )
    if (collapsed) {
        CollapsedExerciseCard(
            item = item,
            media = media,
            bodyWeightKg = bodyWeightKg,
            volume = volume,
            onClick = onToggleExpanded,
        )
        return
    }
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(
                    exercise = item.exercise,
                    media = media,
                    modifier = Modifier.size(52.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        text = ExerciseClassification.partLabel(
                            ExerciseClassification.exerciseGroupingKey(item.exercise),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.exercise.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    )
                    Text(
                        text = stringResource(
                            R.string.volume_label,
                            WeightLogic.displayVolume(volume),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = actionDurationText(item.durationMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_exercise),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SetColumnHeader()
            item.sets.forEachIndexed { index, set ->
                SetRow(
                    index = index + 1,
                    set = set,
                    bodyWeightKg = bodyWeightKg,
                    onChangeMode = { onChangeMode(set.id, it) },
                    onUpdate = { onUpdateSet(set.id, it) },
                    onToggle = { onToggleSet(set.id) },
                    onRemove = { onRemoveSet(set.id) },
                    onRequestBodyWeight = onRequestBodyWeight,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onAddSet) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(
                        stringResource(R.string.add_set),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                TextButton(onClick = onFinishExercise) {
                    Text(stringResource(R.string.training_finish_action))
                }
            }
        }
    }
}

@Composable
private fun SetColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeaderCell(stringResource(R.string.training_set_header_group), Modifier.width(34.dp))
        HeaderCell(stringResource(R.string.training_set_header_mode), Modifier.width(96.dp))
        HeaderCell(stringResource(R.string.training_set_header_weight), Modifier.weight(1f))
        HeaderCell(stringResource(R.string.training_set_header_reps), Modifier.weight(1f))
        HeaderCell(stringResource(R.string.training_set_header_complete), Modifier.width(34.dp))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    index: Int,
    set: DraftSetUi,
    bodyWeightKg: Double,
    onChangeMode: (WeightMode) -> Unit,
    onUpdate: ((DraftSetUi) -> DraftSetUi) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    onRequestBodyWeight: () -> Unit,
) {
    var modeMenu by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val completedBackground = if (dark) Color(0xFF1B3A2A) else Color(0xFFE6F4EA)
    val completedBorder = if (dark) Color(0xFF81C784) else Color(0xFF81C784)
    val completedInk = if (dark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    val background = if (set.completed) completedBackground else MaterialTheme.colorScheme.surface
    val weightText = when (set.mode) {
        WeightMode.WEIGHTED -> set.normalKgText
        WeightMode.BODYWEIGHT_PLUS -> set.extraKgText
        WeightMode.BODYWEIGHT -> if (bodyWeightKg > 0.0) WeightLogic.formatKg(bodyWeightKg) else ""
    }
    val weightEnabled = set.mode != WeightMode.BODYWEIGHT
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (set.completed) {
                    completedBorder
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp),
        )
        Box(modifier = Modifier.width(96.dp)) {
            OutlinedButton(
                onClick = { modeMenu = true },
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            ) {
                Text(
                    text = set.mode.labelZh,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = modeMenu,
                onDismissRequest = { modeMenu = false },
            ) {
                WeightMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.labelZh) },
                        onClick = {
                            modeMenu = false
                            if (mode == WeightMode.BODYWEIGHT && bodyWeightKg <= 0.0) {
                                onRequestBodyWeight()
                            }
                            onChangeMode(mode)
                        },
                    )
                }
            }
        }
        VerticalNumberControl(
            value = weightText,
            enabled = weightEnabled,
            placeholder = stringResource(
                when (set.mode) {
                    WeightMode.BODYWEIGHT_PLUS -> R.string.field_extra_weight
                    WeightMode.BODYWEIGHT -> R.string.bodyweight_set
                    WeightMode.WEIGHTED -> R.string.field_weight
                },
            ),
            increase = {
                if (set.mode == WeightMode.WEIGHTED) {
                    onUpdate { it.copy(normalKgText = WeightLogic.stepKg(it.normalKgText, true)) }
                } else if (set.mode == WeightMode.BODYWEIGHT_PLUS) {
                    onUpdate { it.copy(extraKgText = WeightLogic.stepSignedKg(it.extraKgText, true)) }
                }
            },
            decrease = {
                if (set.mode == WeightMode.WEIGHTED) {
                    onUpdate { it.copy(normalKgText = WeightLogic.stepKg(it.normalKgText, false)) }
                } else if (set.mode == WeightMode.BODYWEIGHT_PLUS) {
                    onUpdate { it.copy(extraKgText = WeightLogic.stepSignedKg(it.extraKgText, false)) }
                }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
        )
        VerticalNumberControl(
            value = set.repsText,
            enabled = true,
            placeholder = stringResource(R.string.field_reps),
            increase = {
                onUpdate { it.copy(repsText = WeightLogic.stepReps(it.repsText, true)) }
            },
            decrease = {
                onUpdate { it.copy(repsText = WeightLogic.stepReps(it.repsText, false)) }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .combinedClickable(
                    onClick = onToggle,
                    onLongClick = onRemove,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(
                    if (set.completed) {
                        R.string.training_set_incomplete
                    } else {
                        R.string.training_set_complete
                    },
                ),
                tint = if (set.completed) {
                    completedInk
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun VerticalNumberControl(
    value: String,
    enabled: Boolean,
    placeholder: String,
    increase: () -> Unit,
    decrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = increase,
            enabled = enabled,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "+",
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = if (value.isBlank()) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = decrease,
            enabled = enabled,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "-",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TrainingSavePage(
    state: SessionEditorUiState,
    onBack: () -> Unit,
    onNoteChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completedSets = state.exercises.flatMap { it.sets }.filter { it.completed }
    val volume = WeightLogic.totalVolume(
        completedSets.map { set -> draftWeightKg(set, state.bodyWeightKg) to set.repsText.trim().toIntOrNull() },
    )
    val parts = state.exercises
        .filter { exercise -> exercise.sets.any { it.completed } }
        .map { ExerciseClassification.partLabel(ExerciseClassification.exerciseGroupingKey(it.exercise)) }
        .distinct()
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cancel),
                    )
                }
                Text(
                    text = stringResource(R.string.training_save_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                                ),
                            )
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "本次训练",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFBBD0EC),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = LocalDate.ofEpochDay(state.dateEpochDay).format(dateFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBBD0EC),
                            )
                        }
                        Text(
                            text = TrainingTimerLogic.formatDuration(state.finishedDurationMillis),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = Color.White,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SaveSummaryStat(
                                label = stringResource(R.string.training_volume),
                                value = WeightLogic.displayVolume(volume),
                                modifier = Modifier.weight(1f),
                            )
                            SaveSummaryStat(
                                label = stringResource(R.string.training_sets),
                                value = completedSets.size.toString(),
                                modifier = Modifier.weight(1f),
                            )
                            SaveSummaryStat(
                                label = stringResource(R.string.training_parts),
                                value = parts.joinToString("、").ifEmpty { "—" },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                item {
                    SaveSectionHeader(
                        title = stringResource(R.string.training_photo_section),
                        optional = true,
                    )
                }
                item {
                    GymLogCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val bitmap = state.photoBitmap
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(108.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFAFCFF))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable(onClick = onPickPhoto),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AddPhotoAlternate,
                                            contentDescription = stringResource(R.string.training_add_photo),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(26.dp),
                                        )
                                        Text(
                                            text = "添加训练照片",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = stringResource(R.string.training_photo_hint),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onPickPhoto, enabled = !state.loadingPhoto) {
                                    Text(
                                        stringResource(
                                            if (bitmap == null) {
                                                R.string.training_add_photo
                                            } else {
                                                R.string.training_change_photo
                                            },
                                        ),
                                    )
                                }
                                if (bitmap != null) {
                                    TextButton(onClick = onRemovePhoto) {
                                        Text(stringResource(R.string.training_remove_photo))
                                    }
                                }
                            }
                            if (state.loadingPhoto) {
                                Text(
                                    text = stringResource(R.string.data_busy),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                item {
                    SaveSectionHeader(
                        title = stringResource(R.string.field_note),
                        optional = true,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = onNoteChange,
                        label = { Text(stringResource(R.string.training_note_prompt)) },
                        placeholder = { Text(stringResource(R.string.training_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3,
                    )
                }
                item {
                    GymLogCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDateClick),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.field_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            )
                            Text(
                                text = LocalDate.ofEpochDay(state.dateEpochDay).format(dateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 3.dp),
                            )
                        }
                    }
                }
                state.errorMessage?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
                item {
                    GymLogActionButton(
                        text = stringResource(
                            if (state.saving) R.string.data_busy else R.string.training_save_action,
                        ),
                        onClick = onSave,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    TextButton(
                        onClick = onDiscard,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (state.sessionId == null) {
                                    R.string.training_discard
                                } else {
                                    R.string.training_delete
                                },
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyWeightDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val weight = WeightLogic.parseKg(text)?.takeIf { it > 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bodyweight_missing_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.bodyweight_missing_message))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.bodyweight_weight_label)) },
                    singleLine = true,
                    isError = text.isNotBlank() && weight == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (text.isNotBlank() && weight == null) {
                    Text(
                        text = stringResource(R.string.bodyweight_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { weight?.let(onConfirm) },
                enabled = weight != null,
            ) {
                Text(stringResource(R.string.bodyweight_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutClockSheet(
    clock: WorkoutClockUi,
    onDismiss: () -> Unit,
    onModeChange: (WorkoutClockMode) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAdjust: (Long) -> Unit,
    onEditTime: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.clock_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                WorkoutClockMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = clock.mode == mode,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, WorkoutClockMode.entries.size),
                    ) {
                        Text(
                            stringResource(
                                if (mode == WorkoutClockMode.TIMER) {
                                    R.string.clock_timer
                                } else {
                                    R.string.clock_stopwatch
                                },
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = clock.mode == WorkoutClockMode.TIMER, onClick = onEditTime),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = WorkoutClockLogic.formatClock(clock.displayMillis),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    textAlign = TextAlign.Center,
                )
            }
            if (clock.mode == WorkoutClockMode.TIMER) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { onAdjust(-30_000L) }) {
                        Text(stringResource(R.string.clock_minus_30))
                    }
                    TextButton(onClick = { onAdjust(30_000L) }) {
                        Text(stringResource(R.string.clock_plus_30))
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.clock_reset))
                }
                Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (clock.running) R.string.clock_pause else R.string.clock_start,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun actionDurationText(durationMillis: Long?): String {
    return if (durationMillis == null) {
        stringResource(R.string.training_action_duration_empty)
    } else {
        stringResource(
            R.string.training_action_duration,
            TrainingTimerLogic.formatDurationChinese(durationMillis),
        )
    }
}

private fun draftWeightKg(set: DraftSetUi, bodyWeightKg: Double): Double {
    val stored = when (set.mode) {
        WeightMode.BODYWEIGHT -> null
        WeightMode.WEIGHTED -> WeightLogic.parseKg(set.normalKgText)
        WeightMode.BODYWEIGHT_PLUS -> WeightLogic.parseSignedKg(set.extraKgText)
    }
    return WeightLogic.effectiveKg(set.mode, stored, bodyWeightKg)
}

private fun vibrateOnce(context: android.content.Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() != true) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500L)
        }
    }
}
