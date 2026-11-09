package com.example.gymlog.ui.exercise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.gymlog.R
import com.example.gymlog.data.ExerciseAliases
import com.example.gymlog.data.ExerciseClassification
import com.example.gymlog.data.ExerciseEntity
import com.example.gymlog.data.ExerciseGuide
import com.example.gymlog.data.ExerciseGuideBuilder
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
import com.example.gymlog.ui.muscle.rememberMuscleSex
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogRadii
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
    // 要点页的「怎么做」来自数据集自带的中文步骤，单独收一条，避免撑大 uiState
    val instructionSteps by viewModel.instructionSteps.collectAsStateWithLifecycle()
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
                    // 标题由总览页的头部横幅承担，顶栏留空避免动作名出现两次
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
                                // 规则生成，只在这里算一次；动作或步骤变了才重算
                                guide = remember(exercise, instructionSteps) {
                                    ExerciseGuideBuilder.build(exercise, instructionSteps)
                                },
                                media = state.media,
                                onEdit = { showEdit = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> OverviewTab(
                                exercise = exercise,
                                media = state.media,
                                links = state.muscleLinks,
                                history = state.history,
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
    history: List<ExerciseHistoryEntry>,
    modifier: Modifier = Modifier,
) {
    val none = stringResource(R.string.pr_none)
    val primary = muscleNames(links, MuscleRole.PRIMARY).ifEmpty { none }
    val secondary = muscleNames(links, MuscleRole.SECONDARY).ifEmpty { none }
    val aliases = ExerciseAliases.parse(exercise.aliases)
    val hidden = linkHiddenNames(links)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(GymLogSpacing.sm))
        // 头部名片：动图完整显示在浅色卡片里，名字与标签用深色字排在下方
        ExerciseHeaderBanner(exercise = exercise, media = media)

        // 别名做成可换行的小胶囊，比「别名：A、B、C」一行长句更好扫读
        if (aliases.isNotEmpty()) {
            Spacer(Modifier.height(GymLogSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                aliases.forEach { alias ->
                    AliasChip(text = alias)
                }            }
        }

        Spacer(Modifier.height(GymLogSpacing.sm))
        // 快捷数据带：一眼看到「我和这个动作的关系」
        StatsStrip(history = history)

        Spacer(Modifier.height(GymLogSpacing.sm))
        // 个人纪录：做组 / 极限左右两栏，数值放大，日期退到第三行
        GymLogCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = GymLogSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrColumn(
                    modifier = Modifier.weight(1f).padding(start = GymLogSpacing.md),
                    label = stringResource(R.string.pr_working_prefix),
                    detail = exercise.workingPr()?.let(PersonalRecordLogic::displayWorking),
                    dateEpochDay = exercise.workingPr()?.dateEpochDay,
                )
                StatDivider(height = 44.dp)
                PrColumn(
                    modifier = Modifier.weight(1f).padding(start = GymLogSpacing.md),
                    label = stringResource(R.string.pr_max_prefix),
                    detail = exercise.maxPr()?.let(PersonalRecordLogic::displayMax),
                    dateEpochDay = exercise.maxPr()?.dateEpochDay,
                )
            }
        }

        if (links.isNotEmpty()) {
            Spacer(Modifier.height(GymLogSpacing.sm))
            // 肌群：左边人形图，右边「标签在上、肌肉名放大在下」，去掉重复的标题
            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(GymLogSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MuscleFigure(
                        sex = rememberMuscleSex(),
                        highlights = links.map { link ->
                            MuscleHighlight(
                                muscleId = link.muscleId,
                                role = link.role,
                                exercises = emptyList(),
                            )
                        },
                        onRegionClick = {},
                        boundedHeight = 200.dp,
                        showSwitch = true,
                        modifier = Modifier.weight(0.5f),
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(start = GymLogSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
                    ) {
                        MuscleRoleBlock(
                            label = stringResource(R.string.muscle_primary),
                            value = primary,
                            emphasized = true,
                        )
                        MuscleRoleBlock(
                            label = stringResource(R.string.muscle_secondary),
                            value = secondary,
                            emphasized = false,
                        )
                        if (hidden.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.muscle_unsupported_list, hidden.joinToString("、")),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            // 未配置肌群时保留说明卡
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
                    Text(
                        text = stringResource(R.string.muscle_unconfigured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(GymLogSpacing.lg))
        // GIF 版权说明弱化放在页面最底部
        media?.attribution?.let { attribution ->
            Text(
                text = attribution,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(GymLogSpacing.md))
        }
    }
}

/** 肌群角色块：小标签在上，肌肉名放大在下；没有配置时用浅色显示「暂无」。 */
@Composable
private fun MuscleRoleBlock(label: String, value: String, emphasized: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (emphasized) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * 快捷数据带：累计组数 / 累计容量 / 最近训练。
 * 数据直接从已加载的历史记录汇总，不额外查库；没有历史时各项显示「—」。
 */
@Composable
private fun StatsStrip(history: List<ExerciseHistoryEntry>) {
    val totalSets = history.sumOf { it.setCount }
    val totalVolume = history.sumOf { it.volumeKg }
    val lastDay = history.maxOfOrNull { it.dateEpochDay }
    val today = LocalDate.now().toEpochDay()
    val lastText = when {
        lastDay == null -> "—"
        lastDay >= today -> stringResource(R.string.exercise_stats_today)
        else -> stringResource(R.string.exercise_stats_days_ago, today - lastDay)
    }
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GymLogSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(
                modifier = Modifier.weight(1f),
                value = if (totalSets > 0) totalSets.toString() else "—",
                label = stringResource(R.string.exercise_stats_total_sets),
            )
            StatDivider()
            StatCell(
                modifier = Modifier.weight(1f),
                value = if (totalVolume > 0.0) WeightLogic.displayVolume(totalVolume) else "—",
                label = stringResource(R.string.exercise_stats_total_volume),
            )
            StatDivider()
            StatCell(
                modifier = Modifier.weight(1f),
                value = lastText,
                label = stringResource(R.string.exercise_stats_last),
            )
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, value: String, label: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun StatDivider(height: Dp = 28.dp) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(height)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * 头部名片：GIF 完整显示（不裁切，全身可见）在卡片上部，名字与部位/器械标签
 * 用深色字排在图片下方。不再用黑色渐变压字——动图本身是白底，压一层暗色会把下半截弄脏。
 */
@Composable
private fun ExerciseHeaderBanner(
    exercise: ExerciseEntity,
    media: com.example.gymlog.data.ExerciseMediaView?,
) {
    GymLogCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GymLogRadii.panel),
    ) {
        // 留边底色取白：动图本来就是白底，浅灰会在两侧露出两道色块
        ExerciseGifPlayer(
            media = media,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        Column(
            modifier = Modifier.padding(
                start = GymLogSpacing.md,
                end = GymLogSpacing.md,
                top = GymLogSpacing.xs,
                bottom = GymLogSpacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
        ) {
            Text(
                text = exercise.displayName(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 25.sp,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BannerChip(text = bodyPartLabel(exercise))
                BannerChip(
                    text = equipmentLabel(
                        exercise.equipmentCategoryId ?: exercise.equipmentType,
                    ),
                )
            }
        }
    }
}

/** 头部标签：淡主色底 + 主色字，跟全 App「选中/强调＝钴蓝」的语言一致。 */
@Composable
private fun BannerChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** 别名胶囊：细描边、无底色，数量多时可以自然换行，不抢头部名片的视觉重量。 */
@Composable
private fun AliasChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun bodyPartLabel(exercise: ExerciseEntity): String {
    return ExerciseClassification.body(exercise.bodyCategoryId)?.labelZh
        ?: exercise.bodyPartDetail?.takeIf { it.isNotBlank() }
        ?: com.example.gymlog.data.BodyPart.entries.find { it.name == exercise.bodyPart }?.labelZh
        ?: exercise.bodyPart
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

/**
 * PR 栏：标签在上、成绩放大、日期最淡，三行竖排。
 * 比原来「左标签 + 右侧成绩和日期挤一行」更好扫读，做组/极限并排也不用上下找。
 */
@Composable
private fun PrColumn(
    label: String,
    detail: String?,
    dateEpochDay: Long?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (detail != null && dateEpochDay != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = PersonalRecordLogic.formatDate(dateEpochDay),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        } else {
            Text(
                text = stringResource(R.string.pr_none),
                style = MaterialTheme.typography.titleMedium,
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
    guide: ExerciseGuide,
    media: com.example.gymlog.data.ExerciseMediaView?,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 用户自己写过的一律优先；留空才用规则整理的内容
    val userHowTo = exercise.howTo?.trim().orEmpty()
    val userCues = exercise.cues?.trim().orEmpty()
    val howTo = userHowTo.ifEmpty { guide.howTo }
    val cues = userCues.ifEmpty { guide.cues }
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
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = GymLogSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.exercise_detail_cues_empty),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.exercise_detail_cues_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymLogSpacing.xs),
                )
                // 空态给一条直达编辑的路径，不用绕顶栏菜单
                Button(
                    onClick = onEdit,
                    modifier = Modifier.padding(top = GymLogSpacing.sm),
                ) {
                    Text(stringResource(R.string.exercise_detail_cues_go_edit))
                }
            }
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
    var aliases by remember { mutableStateOf(ExerciseAliases.parse(exercise.aliases)) }
    var howTo by remember { mutableStateOf(exercise.howTo.orEmpty()) }
    var cues by remember { mutableStateOf(exercise.cues.orEmpty()) }
    var addingAlias by remember { mutableStateOf(false) }
    var aliasDraft by remember { mutableStateOf("") }

    /**
     * 收下输入框里的内容：空白丢弃，忽略大小写去重。
     * [close] 为 true 表示这次是失焦收尾（收起输入框）；回车提交则保持输入态，方便连着加几个。
     */
    fun commitAlias(close: Boolean) {
        val value = aliasDraft.trim()
        if (value.isNotEmpty() && aliases.none { it.equals(value, ignoreCase = true) }) {
            aliases = aliases + value
        }
        aliasDraft = ""
        if (close) addingAlias = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(GymLogRadii.panel),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymLogSpacing.lg)
                .imePadding(),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.exercise_detail_edit_title),
                    modifier = Modifier.padding(
                        start = GymLogSpacing.lg,
                        end = GymLogSpacing.lg,
                        top = GymLogSpacing.lg,
                        bottom = GymLogSpacing.sm,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = GymLogSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(GymLogSpacing.md),
                ) {
                    AliasEditor(
                        aliases = aliases,
                        adding = addingAlias,
                        draft = aliasDraft,
                        onDraftChange = { aliasDraft = it },
                        onStartAdding = { addingAlias = true },
                        onSubmit = { commitAlias(false) },
                        onDismissDraft = { commitAlias(true) },
                        onRemove = { index ->
                            aliases = aliases.filterIndexed { i, _ -> i != index }
                        },
                    )
                    EditField(
                        label = stringResource(R.string.exercise_detail_edit_how_to_label),
                        value = howTo,
                        onValueChange = { howTo = it },
                        placeholder = stringResource(R.string.exercise_detail_edit_auto_hint),
                        minLines = 3,
                    )
                    EditField(
                        label = stringResource(R.string.exercise_detail_edit_cues_label),
                        value = cues,
                        onValueChange = { cues = it },
                        placeholder = stringResource(R.string.exercise_detail_edit_auto_hint),
                        minLines = 3,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = GymLogSpacing.md,
                            end = GymLogSpacing.md,
                            top = GymLogSpacing.sm,
                            bottom = GymLogSpacing.md,
                        ),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(Modifier.width(GymLogSpacing.xs))
                    Button(
                        onClick = { onSave(ExerciseAliases.join(aliases).orEmpty(), howTo, cues) },
                        shape = RoundedCornerShape(GymLogRadii.input),
                    ) {
                        Text(stringResource(R.string.exercise_detail_edit_save))
                    }
                }
            }
        }
    }
}

/** 别名编辑区：一个别名一张可删的小卡，末尾是虚线「＋」；点＋原地变成输入框，回车即加卡。 */
@Composable
private fun AliasEditor(
    aliases: List<String>,
    adding: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onStartAdding: () -> Unit,
    onSubmit: () -> Unit,
    onDismissDraft: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.exercise_detail_edit_aliases_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(10.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                aliases.forEachIndexed { index, alias ->
                    AliasCard(text = alias, onRemove = { onRemove(index) })
                }
                if (adding) {
                    AliasDraftField(
                        value = draft,
                        onValueChange = onDraftChange,
                        onSubmit = onSubmit,
                        onDismiss = onDismissDraft,
                    )
                } else {
                    AliasAddTile(onClick = onStartAdding, wide = aliases.isEmpty())
                }
            }
        }
    }
}

@Composable
private fun AliasCard(text: String, onRemove: () -> Unit) {
    val deleteLabel = stringResource(R.string.exercise_detail_edit_alias_delete)
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(34.dp),
    ) {
        Box {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .widthIn(max = 190.dp)
                    .padding(start = 12.dp, end = 26.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(26.dp)
                    // 用 pointerInput 而不是 clickable：clickable 会被系统撑到 48dp，
                    // 热区溢出卡片、压到下一张卡的「＋」上。
                    .pointerInput(text) { detectTapGestures { onRemove() } }
                    .semantics {
                        contentDescription = deleteLabel
                        role = Role.Button
                        onClick {
                            onRemove()
                            true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                CrossIcon(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun AliasDraftField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // onFocusChanged 首次组合会先回调一次 false，等真的拿到过焦点再判断失焦。
    var hadFocus by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.5.dp, accent),
        modifier = Modifier
            .width(132.dp)
            .height(34.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.exercise_detail_edit_alias_hint),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                    Box(Modifier.padding(horizontal = 10.dp)) { inner() }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        // 点到别处即收尾：有内容就收成卡片，空内容直接收起。
                        onDismiss()
                    }
                },
        )
    }
}

@Composable
private fun AliasAddTile(onClick: () -> Unit, wide: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val addLabel = stringResource(R.string.exercise_detail_edit_alias_add)
    Box(
        modifier = Modifier
            .then(if (wide) Modifier.fillMaxWidth() else Modifier.width(40.dp))
            .height(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(accent.copy(alpha = 0.06f))
            .drawBehind {
                val inset = 0.75.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = 0.45f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(11.dp.toPx() - inset),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                        ),
                    ),
                )
            }
            // 同删除按钮：绕开 clickable 的 48dp 最小热区，热区就是这块虚线本身。
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .semantics {
                contentDescription = addLabel
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        PlusIcon(color = accent)
    }
}

@Composable
private fun CrossIcon(color: Color) {
    Canvas(Modifier.size(8.dp)) {
        val stroke = 1.5.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
    }
}

@Composable
private fun PlusIcon(color: Color) {
    Canvas(Modifier.size(12.dp)) {
        val stroke = 1.6.dp.toPx()
        drawLine(
            color,
            Offset(size.width / 2, 0f),
            Offset(size.width / 2, size.height),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            stroke,
            StrokeCap.Round,
        )
    }
}

/** 弹窗输入项：标签放在框外，输入区用淡底无边框，避免三个重边框框在一起。 */
@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
