package com.example.gymlog.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing

/**
 * 训练偏好：做组 PR 最低次数 + 刷新成绩。
 *
 * 与通知设置页保持同一套结构（TopAppBar + LazyColumn + GymLogCard），
 * 深浅色都走 MaterialTheme 颜色，不写死颜色值。
 *
 * 布局要点：
 * - 「刷新成绩」紧跟步进器同一行（右对齐填满剩余宽度）；窄屏 / 大字号下自动折到下一行，
 *   不让按钮被压扁或挤压输入框。
 * - 所有解释性小字都收进右上角问号对话框，卡片里只留标题、控件和校验错误，
 *   后续再加新的偏好项时也不会堆满灰字。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPreferencesScreen(
    viewModel: TrainingPreferencesViewModel,
    onBack: () -> Unit,
) {
    val minReps by viewModel.minWorkingReps.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHelp by remember { mutableStateOf(false) }

    var input by remember { mutableStateOf(minReps.toString()) }
    // 存储值被外部改动（例如从默认值纠正）时同步输入框
    LaunchedEffect(minReps) { input = minReps.toString() }

    val donePattern = stringResource(R.string.training_refresh_done)
    val doneCountPattern = stringResource(R.string.training_refresh_done_count)
    val failedText = stringResource(R.string.training_refresh_failed)
    LaunchedEffect(state.result) {
        when (val result = state.result) {
            is RefreshResult.Done -> {
                val message = if (result.updatedExercises > 0) {
                    doneCountPattern.format(result.updatedExercises)
                } else {
                    donePattern
                }
                snackbarHostState.showSnackbar(message)
                viewModel.consumeResult()
            }
            RefreshResult.Failed -> {
                snackbarHostState.showSnackbar(failedText)
                viewModel.consumeResult()
            }
            null -> Unit
        }
    }

    BackHandler(onBack = onBack)

    if (showHelp) {
        TrainingPreferencesHelp(onDismiss = { showHelp = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.training_preferences_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(GymLogSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm),
                    ) {
                        // 标题行：右侧的问号只解释这张卡里的设置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.working_pr_min_reps_title),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            HelpIconButton(onClick = { showHelp = true })
                        }
                        // 步进器 + 刷新按钮：够宽就同一行，屏幕很窄时自动折行
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val inline = maxWidth >= INLINE_MIN_WIDTH
                            if (inline) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
                                ) {
                                    WorkingRepsStepper(
                                        input = input,
                                        enabled = !state.refreshing,
                                        onInputChange = { raw ->
                                            input = raw
                                            raw.toIntOrNull()
                                                ?.takeIf { it in TrainingPreferencesViewModel.minRepsRange }
                                                ?.let(viewModel::setMinWorkingReps)
                                        },
                                        onBlur = {
                                            // 失焦时若文本框不是合法值，回落到已保存的值，
                                            // 避免「界面显示 99、实际存着 9」这种不一致
                                            val parsedOnBlur = input.toIntOrNull()
                                            if (parsedOnBlur == null ||
                                                parsedOnBlur !in TrainingPreferencesViewModel.minRepsRange
                                            ) {
                                                input = minReps.toString()
                                            }
                                        },
                                        onStep = { delta ->
                                            val next = (input.toIntOrNull() ?: minReps) + delta
                                            val clamped = next.coerceIn(
                                                TrainingPreferencesViewModel.minRepsRange.first,
                                                TrainingPreferencesViewModel.minRepsRange.last,
                                            )
                                            input = clamped.toString()
                                            viewModel.setMinWorkingReps(clamped)
                                        },
                                    )
                                    RefreshButton(
                                        refreshing = state.refreshing,
                                        onClick = viewModel::refreshRecords,
                                        // 占满剩余宽度，按钮本身不会被步进器挤压
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
                                ) {
                                    WorkingRepsStepper(
                                        input = input,
                                        enabled = !state.refreshing,
                                        onInputChange = { raw ->
                                            input = raw
                                            raw.toIntOrNull()
                                                ?.takeIf { it in TrainingPreferencesViewModel.minRepsRange }
                                                ?.let(viewModel::setMinWorkingReps)
                                        },
                                        onBlur = {
                                            val parsedOnBlur = input.toIntOrNull()
                                            if (parsedOnBlur == null ||
                                                parsedOnBlur !in TrainingPreferencesViewModel.minRepsRange
                                            ) {
                                                input = minReps.toString()
                                            }
                                        },
                                        onStep = { delta ->
                                            val next = (input.toIntOrNull() ?: minReps) + delta
                                            val clamped = next.coerceIn(
                                                TrainingPreferencesViewModel.minRepsRange.first,
                                                TrainingPreferencesViewModel.minRepsRange.last,
                                            )
                                            input = clamped.toString()
                                            viewModel.setMinWorkingReps(clamped)
                                        },
                                    )
                                    RefreshButton(
                                        refreshing = state.refreshing,
                                        onClick = viewModel::refreshRecords,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        // 校验错误是即时反馈，留在卡片里；它不属于「解释小字」
                        val parsed = input.toIntOrNull()
                        if (parsed == null || parsed !in TrainingPreferencesViewModel.minRepsRange) {
                            Text(
                                text = stringResource(
                                    R.string.working_pr_min_reps_error,
                                    TrainingPreferencesViewModel.minRepsRange.first,
                                    TrainingPreferencesViewModel.minRepsRange.last,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 卡片标题右侧的小问号：只解释这张卡里的设置。
 *
 * 用裸 Box 而不是 IconButton —— Material3 的 IconButton 会强制 48dp 最小触摸区，
 * 把标题行撑高、和文字不在一条基线上；这里用 32dp 视觉尺寸保持紧凑。
 */
@Composable
private fun HelpIconButton(onClick: () -> Unit) {
    val description = stringResource(R.string.training_preferences_help)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 问号的说明对话框：所有解释文案集中在这里，卡片本身保持干净。
 */
@Composable
private fun TrainingPreferencesHelp(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.training_preferences_help_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GymLogSpacing.sm)) {
                HelpParagraph(
                    title = stringResource(R.string.working_pr_min_reps_title),
                    body = stringResource(
                        R.string.working_pr_min_reps_help_body,
                        TrainingPreferencesViewModel.minRepsRange.first,
                        TrainingPreferencesViewModel.minRepsRange.last,
                    ),
                )
                HorizontalDivider()
                HelpParagraph(
                    title = stringResource(R.string.max_pr_help_title),
                    body = stringResource(R.string.working_pr_min_reps_note),
                )
                HorizontalDivider()
                HelpParagraph(
                    title = stringResource(R.string.training_refresh),
                    body = stringResource(R.string.training_refresh_desc),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.training_preferences_help_got_it))
            }
        },
    )
}

@Composable
private fun HelpParagraph(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(GymLogSpacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RefreshButton(
    refreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !refreshing,
        modifier = modifier,
        // 默认水平内边距偏大，挤在同一行里会把文字压掉，这里收紧
        contentPadding = PaddingValues(
            horizontal = GymLogSpacing.sm,
            vertical = GymLogSpacing.xs,
        ),
    ) {
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = if (refreshing) {
                stringResource(R.string.training_refresh_running_short)
            } else {
                stringResource(R.string.training_refresh)
            },
            modifier = Modifier.padding(start = GymLogSpacing.xxs),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun WorkingRepsStepper(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onBlur: () -> Unit,
    onStep: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GymLogSpacing.xs),
    ) {
        FilledTonalIconButton(
            onClick = { onStep(-1) },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.working_pr_min_reps_decrease),
            )
        }
        OutlinedTextField(
            value = input,
            // 只保留数字：非法字符直接进不来，也就不会存进设置
            onValueChange = { raw -> onInputChange(raw.filter { it.isDigit() }.take(2)) },
            modifier = Modifier
                .width(88.dp)
                .onFocusChanged { state -> if (!state.isFocused) onBlur() },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text(stringResource(R.string.working_pr_min_reps_unit)) },
        )
        FilledTonalIconButton(
            onClick = { onStep(1) },
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.working_pr_min_reps_increase),
            )
        }
    }
}

/**
 * 卡片内容宽度低于这个值（约等于 320dp 屏）就把按钮折到下一行，
 * 避免步进器与按钮互相挤压。
 */
private val INLINE_MIN_WIDTH = 292.dp
