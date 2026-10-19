package com.example.gymlog.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.ui.exercise.ExerciseThumbnail
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.displayName
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
    onAddExercise: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(enabled = confirmDelete) { confirmDelete = false }
    BackHandler(enabled = showDatePicker) { showDatePicker = false }
    BackHandler(enabled = !showDatePicker && !confirmDelete, onBack = onBack)

    LaunchedEffect(viewModel) {
        viewModel.exitAfterSave.collect { onBack() }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.sessionId == null) R.string.add_record else R.string.edit_record,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    if (state.sessionId != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                    TextButton(onClick = viewModel::save) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = LocalDate.ofEpochDay(state.dateEpochDay).format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_date)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.pick_date))
                }
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text(stringResource(R.string.field_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                Button(
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.add_exercise_to_session))
                }
            }
            state.errorMessage?.let { message ->
                item {
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                ExerciseBlock(
                    item = exercise,
                    media = exerciseMedia[exercise.exercise.id],
                    bodyWeightKg = state.bodyWeightKg,
                    onRemoveExercise = { viewModel.removeExercise(exercise.id) },
                    onAddSet = { viewModel.addSet(exercise.id) },
                    onRemoveSet = { setId -> viewModel.removeSet(exercise.id, setId) },
                    onChangeMode = { setId, mode -> viewModel.changeMode(exercise.id, setId, mode) },
                    onUpdateSet = { setId, transform -> viewModel.updateSet(exercise.id, setId, transform) },
                )
            }
        }
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
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumbnail(
                    exercise = item.exercise,
                    media = media,
                    modifier = Modifier.size(56.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        text = item.exercise.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.volume_label,
                            WeightLogic.displayVolume(
                                WeightLogic.totalVolume(
                                    item.sets.map { set ->
                                        draftWeightKg(set, bodyWeightKg) to set.repsText.trim().toIntOrNull()
                                    },
                                ),
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.pr_working,
                            item.exercise.workingPr()?.let(PersonalRecordLogic::displayWorking)
                                ?: stringResource(R.string.pr_none),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.pr_max,
                            item.exercise.maxPr()?.let(PersonalRecordLogic::displayMax)
                                ?: stringResource(R.string.pr_none),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemoveExercise) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_exercise),
                    )
                }
            }
            item.sets.forEachIndexed { index, set ->
                SetRow(
                    index = index + 1,
                    set = set,
                    bodyWeightKg = bodyWeightKg,
                    canDelete = item.sets.size > 1,
                    onRemove = { onRemoveSet(set.id) },
                    onChangeMode = { onChangeMode(set.id, it) },
                    onUpdate = { onUpdateSet(set.id, it) },
                )
            }
            OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.add_set), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetRow(
    index: Int,
    set: DraftSetUi,
    bodyWeightKg: Double,
    canDelete: Boolean,
    onRemove: () -> Unit,
    onChangeMode: (WeightMode) -> Unit,
    onUpdate: ((DraftSetUi) -> DraftSetUi) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.set_index, index),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.volume_label,
                    WeightLogic.displayVolume(
                        WeightLogic.setVolume(
                            draftWeightKg(set, bodyWeightKg),
                            set.repsText.trim().toIntOrNull(),
                        ),
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (canDelete) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_set),
                    )
                }
            }
        }
        Text(
            text = WeightLogic.displaySet(draftWeightKg(set, bodyWeightKg)),
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightMode.entries.forEach { mode ->
                FilterChip(
                    selected = set.mode == mode,
                    onClick = { onChangeMode(mode) },
                    label = { Text(mode.labelZh) },
                )
            }
        }
        if (set.mode == WeightMode.WEIGHTED) {
            WeightField(
                label = stringResource(R.string.field_weight),
                value = set.normalKgText,
                onValueChange = { text -> onUpdate { it.copy(normalKgText = text) } },
            )
        }
        if (set.mode == WeightMode.BODYWEIGHT_PLUS) {
            WeightField(
                label = stringResource(R.string.field_extra_weight),
                value = set.extraKgText,
                onValueChange = { text -> onUpdate { it.copy(extraKgText = text) } },
                allowNegative = true,
            )
            Text(
                text = stringResource(R.string.bodyweight_plus_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (set.mode == WeightMode.BODYWEIGHT) {
            Text(
                text = stringResource(R.string.bodyweight_hint),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = set.repsText,
                onValueChange = { text -> onUpdate { it.copy(repsText = text) } },
                label = { Text(stringResource(R.string.field_reps)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { onUpdate { it.copy(repsText = WeightLogic.stepReps(set.repsText, false)) } },
            ) {
                Text("-")
            }
            OutlinedButton(
                onClick = { onUpdate { it.copy(repsText = WeightLogic.stepReps(set.repsText, true)) } },
            ) {
                Text("+")
            }
        }
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

@Composable
private fun WeightField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String = "",
    allowNegative: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            prefix = if (prefix.isEmpty()) null else ({ Text(prefix) }),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = {
            onValueChange(
                if (allowNegative) WeightLogic.stepSignedKg(value, false) else WeightLogic.stepKg(value, false),
            )
        }) {
            Text("-")
        }
        OutlinedButton(onClick = {
            onValueChange(
                if (allowNegative) WeightLogic.stepSignedKg(value, true) else WeightLogic.stepKg(value, true),
            )
        }) {
            Text("+")
        }
    }
}
