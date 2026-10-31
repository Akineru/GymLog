package com.example.gymlog.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseAliases
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseHistoryEntry
import com.example.gymlog.data.ExerciseHistorySetView
import com.example.gymlog.data.ExercisePriority
import com.example.gymlog.data.MuscleCatalog
import com.example.gymlog.data.MuscleHighlight
import com.example.gymlog.data.MuscleLink
import com.example.gymlog.data.MuscleRole
import com.example.gymlog.data.PersonalRecordLogic
import com.example.gymlog.data.WeightLogic
import com.example.gymlog.data.WeightMode
import com.example.gymlog.data.displayName
import com.example.gymlog.ui.muscle.MuscleFigure
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val TAB_OVERVIEW = 0
private const val TAB_HISTORY = 1
private const val TAB_CUES = 2
private const val TAB_COUNT = 3

/**
 * 动作详情页：总览 / 历史记录 / 要点三个页面共用同一个动作。
 *
 * 三个 tab 只是内容切换，动作 ID 由 [ExerciseDetailViewModel] 固定，切换不会重新选动作，
 * 因此不存在串数据的问题；历史查询也按 `exerciseId` 精确过滤。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exercise = state.exercise
    val pagerState = rememberPagerState(initialPage = TAB_OVERVIEW) { TAB_COUNT }
    val pagerScope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showMuscleEditor by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = exercise?.displayName().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                    if (exercise != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.exercise_detail_edit),
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(
                                                if (exercise.isFavorite) {
                                                    R.string.exercise_favorite_remove
                                                } else {
                                                    R.string.exercise_favorite_add
                                                },
                                            ),
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.setFavorite(!exercise.isFavorite)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.exercise_detail_edit)) },
                                    onClick = {
                                        showMenu = false
                                        showEdit = true
                                    },
                                )
                                if (exercise.isCustom) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.muscle_edit)) },
                                        onClick = {
                                            showMenu = false
                                            showMuscleEditor = true
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete_exercise),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            !state.loaded -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            exercise == null -> {
                // 动作已被删除（例如从另一处删除后回到这里）：给出说明并允许返回，
                // 不做自动跳转，避免和返回栈互相打架。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.exercise_detail_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    DetailTabBar(
                        titles = listOf(
                            stringResource(R.string.exercise_detail_tab_overview),
                            stringResource(R.string.exercise_detail_tab_history),
                            stringResource(R.string.exercise_detail_tab_cues),
                        ),
                        selectedIndex = pagerState.currentPage,
                        // 拖动时 currentPage 还是旧值，靠这个比例让指示条跟着手指走
                        offsetFraction = pagerState.currentPageOffsetFraction,
                        onSelect = { index -> pagerScope.launch { pagerState.animateScrollToPage(index) } },
                    )
                    // 三页共用同一个 ViewModel 与同一个动作 ID，滑动切换不会改变动作上下文
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        when (page) {
                            TAB_HISTORY -> HistoryTab(
                                history = state.history,
                                modifier = Modifier.fillMaxSize(),
                            )
                            TAB_CUES -> CuesTab(
                                exercise = exercise,
                                media = state.media,
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> OverviewTab(
                                exercise = exercise,
                                media = state.media,
                                links = state.muscleLinks,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (exercise != null && showEdit) {
        ExerciseDetailEditDialog(
            exercise = exercise,
            onDismiss = { showEdit = false },
            onSave = { aliases, howTo, cues ->
                viewModel.saveTrainingFields(
                    aliases = aliases,
                    howTo = howTo,
                    cues = cues,
                )
                showEdit = false
            },
        )
    }

    if (exercise != null && showMuscleEditor) {
        MuscleEditorDialog(
            initial = state.muscleLinks,
            onDismiss = { showMuscleEditor = false },
            onConfirm = { muscles ->
                viewModel.replaceCustomMuscles(muscles)
                showMuscleEditor = false
            },
        )
    }

    if (exercise != null && showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_exercise_title)) },
            text = { Text(stringResource(R.string.confirm_delete_exercise_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        // 删除是归档，训练记录不受影响；完成后回到动作库。
                        viewModel.delete(onDeleted = onBack)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * 顶部标签栏：等宽三段 + 跟随滑动的指示条。
 *
 * `offsetFraction` 取 pager 的 `currentPageOffsetFraction`，所以手指拖到一半时，
 * 指示条也停在一半的位置，而不是等松手后突然跳过去。
 */
@Composable
private fun DetailTabBar(
    titles: List<String>,
    selectedIndex: Int,
    offsetFraction: Float,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            titles.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clickable(role = Role.Button) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 17.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / titles.size
            val position = (selectedIndex + offsetFraction)
                .coerceIn(0f, (titles.size - 1).toFloat())
            Box(
                modifier = Modifier
                    // 指示条取 tab 宽度的一半并居中，滑动时跟着位置连续移动
                    .offset(x = tabWidth * position + tabWidth * 0.25f)
                    .width(tabWidth * 0.5f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(Modifier.height(GymLogSpacing.xxs))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun OverviewTab(
    exercise: ExerciseEntity,
    media: com.example.gymlog.data.ExerciseMediaView?,
    links: List<MuscleLink>,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.pr_none)
    val primary = muscleNames(links, MuscleRole.PRIMARY).ifEmpty { none }
    val secondary = muscleNames(links, MuscleRole.SECONDARY).ifEmpty { none }
    val aliases = ExerciseAliases.display(exercise.aliases)
    val hidden = linkHiddenNames(links)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
    ) {
        Text(
            text = exercise.displayName(),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (aliases.isNotEmpty()) {
            Text(
                text = stringResource(R.string.exercise_detail_aliases) + "：" + aliases,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ExerciseDemoGif(media = media)

        GymLogCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(GymLogSpacing.md),
                verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
            ) {
                InfoLine(
                    label = stringResource(R.string.muscle_primary),
                    value = primary,
                )
                InfoLine(
                    label = stringResource(R.string.muscle_secondary),
                    value = secondary,
                )
                if (hidden.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.muscle_unsupported_list, hidden.joinToString("、")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (links.isEmpty()) {
                    Text(
                        text = stringResource(R.string.muscle_unconfigured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = GymLogSpacing.xxs))
                PrRow(
                    label = stringResource(R.string.pr_working_prefix),
                    detail = exercise.workingPr()?.let(PersonalRecordLogic::displayWorking),
                    dateEpochDay = exercise.workingPr()?.dateEpochDay,
                )
                PrRow(
                    label = stringResource(R.string.pr_max_prefix),
                    detail = exercise.maxPr()?.let(PersonalRecordLogic::displayMax),
                    dateEpochDay = exercise.maxPr()?.dateEpochDay,
                )
            }
        }

        if (links.isNotEmpty()) {
            Text(
                text = stringResource(R.string.muscle_map_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = GymLogSpacing.xs),
            )
            // 复用现有肌群图：showSwitch 自带正面/背面切换，高亮沿用同一套 MuscleHighlight。
            MuscleFigure(
                highlights = links.map { link ->
                    MuscleHighlight(
                        muscleId = link.muscleId,
                        role = link.role,
                        exercises = emptyList(),
                    )
                },
                onRegionClick = {},
                boundedHeight = 320.dp,
                showSwitch = true,
            )
        }
        Spacer(Modifier.height(GymLogSpacing.md))
    }
}

@Composable
private fun ExerciseDemoGif(media: com.example.gymlog.data.ExerciseMediaView?) {
    ExerciseGifPlayer(
        media = media,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    )
    media?.attribution?.let { attribution ->
        Text(
            text = attribution,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryTab(
    history: List<ExerciseHistoryEntry>,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.exercise_detail_history_empty),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.exercise_detail_history_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymLogSpacing.xs),
                )
            }
        }
        return
    }
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(history, key = { it.sessionId }) { entry ->
            HistoryEntryCard(entry = entry, formatter = formatter)
        }
    }
}

/** PR 行：标签和成绩在左，日期用纯数字格式贴最右。没有 PR 时只显示「暂无」。 */
@Composable
private fun PrRow(
    label: String,
    detail: String?,
    dateEpochDay: Long?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (detail != null && dateEpochDay != null) {
                "$label $detail"
            } else {
                label + " " + stringResource(R.string.pr_none)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (detail != null && dateEpochDay != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = PersonalRecordLogic.formatDate(dateEpochDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ExerciseHistoryEntry,
    formatter: DateTimeFormatter,
) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatter.format(LocalDate.ofEpochDay(entry.dateEpochDay)),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.volume_label,
                        WeightLogic.displayVolume(entry.volumeKg),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = repsSummary(entry),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 序号用「在本次训练里的第几组」，不依赖 setIndex 的起始值
            // （历史数据里它从 1 开始，直接 +1 会显示成「第 2 组」开头）
            entry.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.exercise_detail_history_set_index,
                            index + 1,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(
                        text = setLine(set),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** 「4 组 × 8 次」；各组次数不同时逐项列出，不合并成同一个数字。 */
@Composable
private fun repsSummary(entry: ExerciseHistoryEntry): String {
    val uniform = entry.uniformReps
    return if (uniform != null) {
        stringResource(R.string.exercise_detail_history_reps_uniform, entry.setCount, uniform)
    } else {
        val detail = entry.sets.joinToString("/") { it.reps?.toString() ?: "-" }
        stringResource(R.string.exercise_detail_history_reps_mixed, entry.setCount, detail)
    }
}

/** 单组展示：普通负重显示存储重量；自重与负重自重按模式给出可读文案。 */
@Composable
private fun setLine(set: ExerciseHistorySetView): String {
    val weightText = when (set.weightMode) {
        WeightMode.WEIGHTED -> WeightLogic.displaySet(set.storedWeightKg)
        WeightMode.BODYWEIGHT -> stringResource(R.string.weight_bodyweight)
        WeightMode.BODYWEIGHT_PLUS -> {
            val extra = set.storedWeightKg ?: 0.0
            when {
                extra > 0.0 -> stringResource(
                    R.string.weight_bodyweight_plus,
                    WeightLogic.displaySet(extra),
                )
                extra < 0.0 -> stringResource(
                    R.string.weight_bodyweight_minus,
                    WeightLogic.displaySet(-extra),
                )
                else -> stringResource(R.string.weight_bodyweight)
            }
        }
    }
    val reps = set.reps
    return if (reps == null) {
        stringResource(R.string.set_summary_no_reps, weightText)
    } else {
        stringResource(R.string.set_summary, weightText, reps)
    }
}

@Composable
private fun CuesTab(
    exercise: ExerciseEntity,
    media: com.example.gymlog.data.ExerciseMediaView?,
    modifier: Modifier = Modifier,
) {
    val howTo = exercise.howTo?.trim().orEmpty()
    val cues = exercise.cues?.trim().orEmpty()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
    ) {
        ExerciseGifPlayer(
            media = media,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        if (howTo.isEmpty() && cues.isEmpty()) {
            Text(
                text = stringResource(R.string.exercise_detail_cues_empty),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = GymLogSpacing.sm),
            )
            Text(
                text = stringResource(R.string.exercise_detail_cues_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        CuesSection(
            title = stringResource(R.string.exercise_detail_cues_how_to),
            body = howTo,
        )
        CuesSection(
            title = stringResource(R.string.exercise_detail_cues_details),
            body = cues,
        )
        Spacer(Modifier.height(GymLogSpacing.md))
    }
}

@Composable
private fun CuesSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = body.ifEmpty { stringResource(R.string.exercise_detail_cues_empty) },
            style = MaterialTheme.typography.bodyMedium,
            color = if (body.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** 别名 / 怎么做 / 细节：只改这三个字段，不碰名称、分类与肌群。优先级由收藏功能统一管理，不在这里编辑。 */
@Composable
private fun ExerciseDetailEditDialog(
    exercise: ExerciseEntity,
    onDismiss: () -> Unit,
    onSave: (aliases: String, howTo: String, cues: String) -> Unit,
) {
    var aliases by remember { mutableStateOf(exercise.aliases.orEmpty()) }
    var howTo by remember { mutableStateOf(exercise.howTo.orEmpty()) }
    var cues by remember { mutableStateOf(exercise.cues.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exercise_detail_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
            ) {
                OutlinedTextField(
                    value = aliases,
                    onValueChange = { aliases = it },
                    label = { Text(stringResource(R.string.exercise_detail_edit_aliases_label)) },
                    placeholder = { Text(stringResource(R.string.exercise_detail_edit_aliases_hint)) },
                    // 多别名一行放不下，给两三行的空间；解析结果直接显示在下面，
                    // 用户能立刻看到「逗号/顿号/分号」被拆成了几条、有没有重复被去掉。
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        val parsedAliases = ExerciseAliases.parse(aliases)
                        Text(
                            text = if (parsedAliases.isEmpty()) {
                                stringResource(R.string.exercise_detail_edit_aliases_empty)
                            } else {
                                stringResource(
                                    R.string.exercise_detail_edit_aliases_count,
                                    parsedAliases.size,
                                    parsedAliases.joinToString("、"),
                                )
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = howTo,
                    onValueChange = { howTo = it },
                    label = { Text(stringResource(R.string.exercise_detail_edit_how_to_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cues,
                    onValueChange = { cues = it },
                    label = { Text(stringResource(R.string.exercise_detail_edit_cues_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(aliases, howTo, cues) },
            ) {
                Text(stringResource(R.string.exercise_detail_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun muscleNames(links: List<MuscleLink>, role: MuscleRole): String {
    return links.filter { it.role == role }
        .mapNotNull { MuscleCatalog.find(it.muscleId)?.nameZh }
        .joinToString("、")
}

private fun linkHiddenNames(links: List<MuscleLink>): List<String> {
    return links.mapNotNull { MuscleCatalog.find(it.muscleId) }
        .filter { !it.visualSupported }
        .map { it.nameZh }
}
