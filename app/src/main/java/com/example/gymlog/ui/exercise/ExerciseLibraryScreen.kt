package com.example.gymlog.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.ui.settings.SettingsTopBarAction
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseMediaView
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleMapLogic
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.displayName
import com.example.gymlog.ui.muscle.MuscleFigure

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    viewModel: ExerciseLibraryViewModel,
    pickerMode: Boolean,
    onBack: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    showBack: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchField by remember {
        mutableStateOf(TextFieldValue(state.query, TextRange(state.query.length)))
    }
    LaunchedEffect(state.query) {
        if (searchField.text != state.query) {
            searchField = TextFieldValue(state.query, TextRange(state.query.length))
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var selected by remember { mutableStateOf<ExerciseEntity?>(null) }
    var editingMuscles by remember { mutableStateOf(false) }
    val libraryDialogOpen = showAddDialog || pendingDelete != null || selected != null
    BackHandler(enabled = libraryDialogOpen) {
        when {
            editingMuscles -> editingMuscles = false
            pendingDelete != null -> pendingDelete = null
            else -> {
                showAddDialog = false
                selected = null
            }
        }
    }
    if (showBack) {
        BackHandler(enabled = !libraryDialogOpen, onBack = onBack)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (pickerMode) R.string.pick_exercise else R.string.exercise_library,
                        ),
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    }
                },
                actions = {
                    if (onOpenSettings != null) {
                        SettingsTopBarAction(onOpenSettings)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchField,
                    onValueChange = { value ->
                        // Keep the caret at the end after every edit; the search
                        // state is updated asynchronously by the ViewModel.
                        searchField = value.copy(
                            selection = TextRange(value.text.length),
                        )
                        viewModel.onQueryChange(value.text)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_exercise)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_custom_exercise),
                    )
                }
            }
            if (state.searchingAll) {
                Text(
                    text = stringResource(R.string.library_search_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                BodyCategoryRail(
                    selected = state.bodyPart,
                    onSelect = viewModel::onBodyPartSelected,
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EquipmentCategoryBar(
                        selected = state.equipmentType,
                        onSelect = viewModel::onEquipmentSelected,
                    )
                    Text(
                        text = if (state.searchingAll) {
                            stringResource(R.string.library_search_count, state.exercises.size)
                        } else {
                            stringResource(
                                R.string.library_section_count,
                                state.bodyPart.labelZh,
                                state.equipmentType.labelZh,
                                state.exercises.size,
                            )
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    ExerciseBrowsePane(
                        state = state,
                        onOpen = { exercise ->
                            if (pickerMode) onPick(exercise) else selected = exercise
                        },
                    )
                }
            }
        }
    }

    pendingDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_exercise_title)) },
            text = { Text(stringResource(R.string.confirm_delete_exercise_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(exercise.id)
                        pendingDelete = null
                        selected = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, part, equipment, mode, muscles ->
                viewModel.addCustomExercise(name, part, equipment, mode, "", muscles)
                showAddDialog = false
            },
        )
    }

    selected?.let { exercise ->
        val links = MuscleMapLogic.linksFor(exercise.id, exercise.isCustom, state.storedLinks)
        if (editingMuscles && exercise.isCustom) {
            MuscleEditorDialog(
                initial = links,
                onDismiss = { editingMuscles = false },
                onConfirm = { muscles ->
                    viewModel.replaceCustomMuscles(exercise.id, muscles)
                    editingMuscles = false
                },
            )
        } else {
            ExerciseMuscleDialog(
                exercise = exercise,
                links = links,
                media = state.media[exercise.id],
                onDismiss = { selected = null },
                onEdit = { editingMuscles = true },
                onDelete = { pendingDelete = exercise },
            )
        }
    }
}

@Composable
private fun ExerciseBrowsePane(
    state: ExerciseLibraryUiState,
    onOpen: (ExerciseEntity) -> Unit,
) {
    if (!state.ready) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.exercises.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Text(
                text = stringResource(
                    if (state.searchingAll) R.string.library_empty_search else R.string.library_empty_category,
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            ExerciseArtCredit()
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.exercises, key = { it.id }) { exercise ->
            ExerciseGridCard(
                exercise = exercise,
                media = state.media[exercise.id],
                muscles = muscleSummary(exercise, state.storedLinks),
                onOpen = { onOpen(exercise) },
            )
        }
        item(key = "exercise-art-credit", span = { GridItemSpan(2) }) {
            ExerciseArtCredit()
        }
    }
}

@Composable
private fun ExerciseGridCard(
    exercise: ExerciseEntity,
    media: ExerciseMediaView?,
    muscles: String,
    onOpen: () -> Unit,
) {
    val customLabel = stringResource(R.string.exercise_custom)
    val classificationLabel = when (exercise.classificationStatus) {
        ExerciseClassification.STATUS_AMBIGUOUS ->
            stringResource(R.string.exercise_classification_ambiguous)
        ExerciseClassification.STATUS_UNMAPPED ->
            stringResource(R.string.exercise_classification_unmapped)
        else -> null
    }
    val detail = buildString {
        append(equipmentLabel(exercise.equipmentCategoryId ?: exercise.equipmentType))
        if (exercise.isCustom) {
            append(" · ")
            append(customLabel)
        }
        if (classificationLabel != null) {
            append(" · ")
            append(classificationLabel)
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            ExerciseThumbnail(
                exercise = exercise,
                media = media,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
            )
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (muscles.isNotEmpty()) {
                Text(
                    text = muscles,
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
private fun ExerciseArtCredit() {
    Text(
        text = stringResource(R.string.exercise_art_credit),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        String,
        ExerciseClassification.BodyCategory,
        ExerciseClassification.EquipmentCategory,
        WeightMode,
        List<MuscleLink>,
    ) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var bodyPart by remember { mutableStateOf(ExerciseClassification.CHEST) }
    var equipment by remember { mutableStateOf(ExerciseClassification.equipmentStandard.first()) }
    var primary by remember { mutableStateOf(setOf<String>()) }
    var secondary by remember { mutableStateOf(setOf<String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_exercise)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_exercise)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.filter_body_part), style = MaterialTheme.typography.labelLarge)
                FlowChipBlock(
                    options = ExerciseClassification.bodyStandard.map { it to it.labelZh },
                    selected = bodyPart,
                    onSelect = { bodyPart = it },
                )
                Text(stringResource(R.string.filter_equipment), style = MaterialTheme.typography.labelLarge)
                FlowChipBlock(
                    options = ExerciseClassification.equipmentStandard.map { it to it.labelZh },
                    selected = equipment,
                    onSelect = { equipment = it },
                )
                MuscleRolePicker(
                    primary = primary,
                    secondary = secondary,
                    onPrimary = { id ->
                        val next = toggleRole(primary, secondary, id)
                        primary = next.first
                        secondary = next.second
                    },
                    onSecondary = { id ->
                        val next = toggleRole(secondary, primary, id)
                        secondary = next.first
                        primary = next.second
                    },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isEmpty()) {
                        error = "请填写动作名称"
                    } else {
                        onConfirm(name, bodyPart, equipment, WeightMode.WEIGHTED, linksFrom(primary, secondary))
                    }
                },
            ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChipBlock(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

private fun bodyPartLabel(value: String): String {
    return ExerciseClassification.body(value)?.labelZh
        ?: ExerciseClassification.bodyForLegacy(value).labelZh
}

private fun equipmentLabel(value: String): String {
    return ExerciseClassification.equipment(value)?.labelZh
        ?: ExerciseClassification.equipmentForLegacy(value).labelZh
}

@Composable
private fun ExerciseMuscleDialog(
    exercise: ExerciseEntity,
    links: List<MuscleLink>,
    media: ExerciseMediaView?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val highlights = links.map { link ->
        MuscleHighlight(muscleId = link.muscleId, role = link.role, exercises = emptyList())
    }
    val primary = namesFor(links, MuscleRole.PRIMARY)
    val secondary = namesFor(links, MuscleRole.SECONDARY)
    val none = stringResource(R.string.pr_none)
    var tab by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.displayName()) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${bodyPartLabel(exercise.bodyCategoryId ?: exercise.bodyPart)} · " +
                    "${equipmentLabel(exercise.equipmentCategoryId ?: exercise.equipmentType)}" +
                        if (exercise.isCustom) " · " + stringResource(R.string.exercise_custom) else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (exercise.classificationStatus == ExerciseClassification.STATUS_AMBIGUOUS) {
                    Text(
                        text = stringResource(R.string.exercise_classification_ambiguous),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (exercise.classificationStatus == ExerciseClassification.STATUS_UNMAPPED) {
                    Text(
                        text = stringResource(R.string.exercise_classification_unmapped),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.pr_working,
                        exercise.workingPr()?.let(PersonalRecordLogic::displayWorking)
                            ?: stringResource(R.string.pr_none),
                    ),
                )
                Text(
                    text = stringResource(
                        R.string.pr_max,
                        exercise.maxPr()?.let(PersonalRecordLogic::displayMax)
                            ?: stringResource(R.string.pr_none),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally,
                    ),
                ) {
                    FilterChip(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        label = { Text(stringResource(R.string.exercise_demo)) },
                    )
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        label = { Text(stringResource(R.string.exercise_muscles_front)) },
                    )
                    FilterChip(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        label = { Text(stringResource(R.string.exercise_muscles_back)) },
                    )
                }
                when (tab) {
                    0 -> {
                        ExerciseGifPlayer(
                            media = media,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                        )
                        media?.attribution?.let { attribution ->
                            Text(
                                text = attribution,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        if (links.isNotEmpty()) {
                            MuscleFigure(
                                highlights = highlights,
                                onRegionClick = {},
                                boundedHeight = 280.dp,
                                forcedFront = tab == 1,
                                showSwitch = false,
                            )
                        }
                    }
                }
                if (links.isNotEmpty()) {
                    Text(stringResource(R.string.muscle_primary) + "：" + primary.ifEmpty { none })
                    Text(stringResource(R.string.muscle_secondary) + "：" + secondary.ifEmpty { none })
                    val hidden = links.mapNotNull { MuscleCatalog.find(it.muscleId) }
                        .filter { !it.visualSupported }
                        .map { it.nameZh }
                    if (hidden.isNotEmpty()) {
                        Text(stringResource(R.string.muscle_unsupported_list, hidden.joinToString("、")))
                    }
                } else {
                    Text(stringResource(R.string.muscle_unconfigured))
                }
            }
        },
        confirmButton = {
            if (exercise.isCustom) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.muscle_edit))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ok))
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(
                        text = stringResource(R.string.delete_exercise),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (exercise.isCustom) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        },
    )
}

@Composable
private fun MuscleEditorDialog(
    initial: List<MuscleLink>,
    onDismiss: () -> Unit,
    onConfirm: (List<MuscleLink>) -> Unit,
) {
    var primary by remember {
        mutableStateOf(initial.filter { it.role == MuscleRole.PRIMARY }.map { it.muscleId }.toSet())
    }
    var secondary by remember {
        mutableStateOf(initial.filter { it.role == MuscleRole.SECONDARY }.map { it.muscleId }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.muscle_edit)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                MuscleRolePicker(
                    primary = primary,
                    secondary = secondary,
                    onPrimary = { id ->
                        val next = toggleRole(primary, secondary, id)
                        primary = next.first
                        secondary = next.second
                    },
                    onSecondary = { id ->
                        val next = toggleRole(secondary, primary, id)
                        secondary = next.first
                        primary = next.second
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(linksFrom(primary, secondary)) }) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleRolePicker(
    primary: Set<String>,
    secondary: Set<String>,
    onPrimary: (String) -> Unit,
    onSecondary: (String) -> Unit,
) {
    Text(stringResource(R.string.muscle_primary), style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MuscleCatalog.all.forEach { muscle ->
            FilterChip(
                selected = muscle.id in primary,
                onClick = { onPrimary(muscle.id) },
                label = { Text(muscle.nameZh) },
            )
        }
    }
    Text(
        stringResource(R.string.muscle_secondary),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MuscleCatalog.all.forEach { muscle ->
            FilterChip(
                selected = muscle.id in secondary,
                onClick = { onSecondary(muscle.id) },
                label = { Text(muscle.nameZh) },
            )
        }
    }
}

private fun toggleRole(selected: Set<String>, other: Set<String>, id: String): Pair<Set<String>, Set<String>> {
    return if (id in selected) {
        selected - id to other
    } else {
        selected + id to (other - id)
    }
}

private fun linksFrom(primary: Set<String>, secondary: Set<String>): List<MuscleLink> {
    val primaryLinks = primary.map { id ->
        MuscleLink(exerciseId = "", muscleId = id, role = MuscleRole.PRIMARY, sortOrder = 0)
    }
    val secondaryLinks = secondary.map { id ->
        MuscleLink(exerciseId = "", muscleId = id, role = MuscleRole.SECONDARY, sortOrder = 0)
    }
    return primaryLinks + secondaryLinks
}

private fun namesFor(links: List<MuscleLink>, role: MuscleRole): String {
    return links.filter { it.role == role }
        .mapNotNull { MuscleCatalog.find(it.muscleId)?.nameZh }
        .joinToString("、")
}

private fun muscleSummary(exercise: ExerciseEntity, stored: List<MuscleLink>): String {
    val links = MuscleMapLogic.linksFor(exercise.id, exercise.isCustom, stored)
    if (links.isEmpty()) return ""
    return links.mapNotNull { MuscleCatalog.find(it.muscleId)?.nameZh }.joinToString("、")
}
