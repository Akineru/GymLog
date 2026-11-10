package com.example.gymlog.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
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
import com.example.gymlog.ui.theme.BrandAccent
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseLibraryScreen(
    viewModel: ExerciseLibraryViewModel,
    pickerMode: Boolean,
    onBack: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    showBack: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    /** 浏览态点击动作卡片时进入详情页；选择器模式下仍然直接返回所选动作。 */
    onOpenDetail: ((String) -> Unit)? = null,
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
    val libraryDialogOpen = showAddDialog
    BackHandler(enabled = libraryDialogOpen) { showAddDialog = false }
    val gridState = rememberLazyGridState()
    // 换部位 / 换器械后，新结果必须从顶部开始（不能沿用上一组滚到的位置）。
    // 放在状态变化上做，而不是在点击回调里抢动画：列表数据是异步刷新的，
    // 点击瞬间滚动容易被随后的重建覆盖，表现就是「还停在原来滚下去的距离」。
    // topRequest 用来覆盖「点的就是当前已选中的分类」的情况——那时筛选状态没变，
    // 光靠 key 不会重新触发，计数器一变就会再滚一次顶部。
    var topRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.bodyPart.id, state.equipmentType.id, topRequest) {
        gridState.scrollToItem(0)
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
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text(stringResource(R.string.search_exercise)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandAccent),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_custom_exercise),
                        tint = androidx.compose.ui.graphics.Color.White,
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
                    // 点分类一律回顶：换部位靠筛选状态变化，重复点当前部位靠计数器
                    onSelect = { part ->
                        viewModel.onBodyPartSelected(part)
                        topRequest++
                    },
                )
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    EquipmentCategoryBar(
                        selected = state.equipmentType,
                        onSelect = { type ->
                            viewModel.onEquipmentSelected(type)
                            topRequest++
                        },
                    )
                    ExerciseSummaryText(
                        count = state.exercises.size,
                        bodyPart = state.bodyPart,
                        equipmentType = state.equipmentType,
                        searchingAll = state.searchingAll,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    ExerciseBrowsePane(
                        state = state,
                        listState = gridState,
                        onToggleFavorite = viewModel::setFavorite,
                        onOpen = { exercise ->
                            when {
                                pickerMode -> onPick(exercise)
                                onOpenDetail != null -> onOpenDetail(exercise.id)
                                // 没有提供详情入口时保持原有的「不响应」行为，避免误触
                                else -> Unit
                            }
                        },
                    )
                }
            }
        }
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
}

@Composable
private fun ExerciseBrowsePane(
    state: ExerciseLibraryUiState,
    listState: LazyGridState,
    onOpen: (ExerciseEntity) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
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
        state = listState,
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
                onToggleFavorite = { onToggleFavorite(exercise.id, !exercise.isFavorite) },
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
    onToggleFavorite: () -> Unit,
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
    GymLogCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Column(modifier = Modifier.padding(GymLogSpacing.sm)) {
            Box {
                ExerciseThumbnail(
                    exercise = exercise,
                    media = media,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp),
                )
                // 收藏星标：自己的 clickable 会消费点击，不会冒泡触发卡片的 onOpen
                val favoriteLabel = stringResource(
                    if (exercise.isFavorite) {
                        R.string.exercise_favorite_remove
                    } else {
                        R.string.exercise_favorite_add
                    },
                )
                // 点亮时缩放弹跳：1 → 1.2 → 1，给收藏一个轻量的「按下感」
                val starScale = remember { androidx.compose.animation.core.Animatable(1f) }
                LaunchedEffect(exercise.isFavorite) {
                    if (exercise.isFavorite) {
                        starScale.snapTo(1f)
                        starScale.animateTo(1.2f, androidx.compose.animation.core.tween(75))
                        starScale.animateTo(1f, androidx.compose.animation.core.tween(75))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        // 触摸区 44dp（无障碍建议值），用 offset 抵消尺寸变化让星星视觉位置不变
                        // x 正值向右，星星贴卡片右上角，不压缩略图
                        .offset(x = 18.dp, y = (-15).dp)
                        .size(44.dp)
                        .clickable(onClick = onToggleFavorite, role = Role.Button)
                        .semantics { contentDescription = favoriteLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (exercise.isFavorite) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.StarOutline
                        },
                        contentDescription = null,
                        tint = if (exercise.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(17.dp)
                            .scale(starScale.value),
                    )
                }
            }
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
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
    var musclesDialog by remember { mutableStateOf(false) }
    var equipmentDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.add_custom_exercise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_exercise_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.add_exercise_name_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(2.dp))
                SectionHeader(
                    label = stringResource(R.string.filter_body_part),
                    value = bodyPart.labelZh,
                )
                FlowChipBlock(
                    options = ExerciseClassification.bodyStandard.map { it to it.labelZh },
                    selected = bodyPart,
                    onSelect = { bodyPart = it },
                )
                Spacer(Modifier.height(6.dp))
                PickerRow(
                    label = stringResource(R.string.filter_equipment),
                    value = equipment.labelZh,
                    onClick = { equipmentDialog = true },
                )
                Spacer(Modifier.height(8.dp))
                MusclePickerRow(
                    count = primary.size + secondary.size,
                    onClick = { musclesDialog = true },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, bodyPart, equipment, WeightMode.WEIGHTED, linksFrom(primary, secondary))
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
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

    if (equipmentDialog) {
        ChipPickerDialog(
            title = stringResource(R.string.filter_equipment),
            options = ExerciseClassification.equipmentStandard.map { it to it.labelZh },
            selected = equipment,
            onSelect = { equipment = it },
            onDismiss = { equipmentDialog = false },
        )
    }

    if (musclesDialog) {
        MuscleEditorDialog(
            initial = linksFrom(primary, secondary),
            title = stringResource(R.string.add_exercise_muscle_title),
            onDismiss = { musclesDialog = false },
            onConfirm = { links ->
                primary = links.filter { it.role == MuscleRole.PRIMARY }.map { it.muscleId }.toSet()
                secondary = links.filter { it.role == MuscleRole.SECONDARY }.map { it.muscleId }.toSet()
                musclesDialog = false
            },
        )
    }
}

/** 一行"名字 + 当前值 + 右箭头"，点进去在子对话框里选。 */
@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 通用单选子对话框：点一项即选中，关掉回主表单。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipPickerDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                FlowChipBlock(options = options, selected = selected, onSelect = onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

/** 分组标题：左侧名字，右侧当前选中值，省得在十几个 chip 里找蓝的那块。 */@Composable
private fun SectionHeader(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 肌群选择收成一行，点进去在子对话框里选，主对话框不用再滚。 */
@Composable
private fun MusclePickerRow(count: Int, onClick: () -> Unit) {
    val countText = if (count > 0) {
        stringResource(R.string.add_exercise_muscle_count, count)
    } else {
        stringResource(R.string.add_exercise_muscle_optional)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.add_exercise_muscle_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = if (count > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 全项目统一的选择 chip：未选淡灰底无描边，选中实心主色。 */
@Composable
private fun SelectChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(10.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChipBlock(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            SelectChip(
                selected = selected == value,
                label = label,
                onClick = { onSelect(value) },
            )
        }
    }
}

internal fun equipmentLabel(value: String): String {
    return ExerciseClassification.equipment(value)?.labelZh
        ?: ExerciseClassification.equipmentForLegacy(value).labelZh
}

@Composable
internal fun MuscleEditorDialog(
    initial: List<MuscleLink>,
    onDismiss: () -> Unit,
    onConfirm: (List<MuscleLink>) -> Unit,
    title: String? = null,
) {
    var primary by remember {
        mutableStateOf(initial.filter { it.role == MuscleRole.PRIMARY }.map { it.muscleId }.toSet())
    }
    var secondary by remember {
        mutableStateOf(initial.filter { it.role == MuscleRole.SECONDARY }.map { it.muscleId }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = title ?: stringResource(R.string.muscle_edit),
                style = MaterialTheme.typography.titleLarge,
            )
        },
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
    Text(stringResource(R.string.muscle_primary), style = MaterialTheme.typography.titleSmall)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuscleCatalog.all.forEach { muscle ->
            SelectChip(
                selected = muscle.id in primary,
                label = muscle.nameZh,
                onClick = { onPrimary(muscle.id) },
            )
        }
    }
    Text(
        stringResource(R.string.muscle_secondary),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MuscleCatalog.all.forEach { muscle ->
            SelectChip(
                selected = muscle.id in secondary,
                label = muscle.nameZh,
                onClick = { onSecondary(muscle.id) },
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

private fun muscleSummary(exercise: ExerciseEntity, stored: List<MuscleLink>): String {
    val links = MuscleMapLogic.linksFor(exercise.id, exercise.isCustom, stored)
    if (links.isEmpty()) return ""
    return links.mapNotNull { MuscleCatalog.find(it.muscleId)?.nameZh }.joinToString("、")
}
