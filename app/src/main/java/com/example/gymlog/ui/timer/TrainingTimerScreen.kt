package com.example.gymlog.ui.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.TimerPhase
import com.example.gymlog.data.TrainingTimerLogic
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTimerScreen(
    viewModel: TrainingTimerViewModel,
    selectedDateEpochDay: Long,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.timer_saved)
    val failedMessage = stringResource(R.string.timer_save_failed)
    val emptyMessage = stringResource(R.string.timer_nothing_to_save)
    val recoveredMessage = stringResource(R.string.timer_recovered)
    var editing by remember { mutableStateOf(false) }
    var confirmingReset by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                TimerMessage.SAVED -> savedMessage
                TimerMessage.SAVE_FAILED -> failedMessage
                TimerMessage.NOTHING_TO_SAVE -> emptyMessage
                TimerMessage.RECOVERED -> recoveredMessage
            }
            snackbarHostState.showSnackbar(text)
        }
    }
    BackHandler(enabled = editing || confirmingReset) {
        editing = false
        confirmingReset = false
    }
    BackHandler(
        enabled = !editing && !confirmingReset,
        onBack = onBack,
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_title)) },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = phaseLabel(state.phase),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.recoveredAfterReboot) {
                    Text(
                        text = stringResource(R.string.timer_recovered),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = state.formatted,
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = { editing = true })
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { viewModel.toggle(selectedDateEpochDay) }) {
                        Icon(
                            imageVector = if (state.phase == TimerPhase.RUNNING) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = primaryActionLabel(state.phase),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = viewModel::lap,
                        enabled = state.phase != TimerPhase.IDLE,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = stringResource(R.string.timer_lap),
                        )
                    }
                    IconButton(onClick = { confirmingReset = true }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.timer_reset),
                        )
                    }
                    Button(
                        onClick = {
                            val date = state.recordDateEpochDay ?: selectedDateEpochDay
                            viewModel.finish(date)
                        },
                        enabled = !state.saving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.timer_finish))
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.timer_laps),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.laps.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.timer_laps_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                val newestIndex = state.laps.maxOf { it.index }
                items(state.laps.asReversed(), key = { it.index }) { lap ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = dashboardCardShape(),
                        colors = dashboardCardColors(),
                        elevation = dashboardCardElevation(),
                        border = dashboardCardBorder(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.timer_lap_index, lap.index) +
                                    if (lap.index == newestIndex) {
                                        " · " + stringResource(R.string.timer_latest)
                                    } else {
                                        ""
                                    },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(
                                    R.string.timer_lap_interval,
                                    TrainingTimerLogic.formatDuration(lap.intervalMillis),
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                            )
                            Text(
                                text = stringResource(
                                    R.string.timer_lap_elapsed,
                                    TrainingTimerLogic.formatDuration(lap.elapsedMillis),
                                ),
                                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                            )
                        }
                    }
                }
            }
        }
    }
    if (editing) {
        EditTimerDialog(
            elapsedMillis = state.elapsedMillis,
            onDismiss = { editing = false },
            onConfirm = { millis ->
                viewModel.edit(millis)
                editing = false
            },
        )
    }
    if (confirmingReset) {
        ResetTimerDialog(
            hasLaps = state.laps.isNotEmpty(),
            onDismiss = { confirmingReset = false },
            onConfirm = { clearLaps ->
                viewModel.reset(clearLaps)
                confirmingReset = false
            },
        )
    }
}

@Composable
private fun phaseLabel(phase: TimerPhase): String {
    return stringResource(
        when (phase) {
            TimerPhase.IDLE -> R.string.timer_status_idle
            TimerPhase.RUNNING -> R.string.timer_status_running
            TimerPhase.PAUSED -> R.string.timer_status_paused
        },
    )
}

@Composable
private fun primaryActionLabel(phase: TimerPhase): String {
    return stringResource(
        when (phase) {
            TimerPhase.IDLE -> R.string.timer_start
            TimerPhase.RUNNING -> R.string.timer_pause
            TimerPhase.PAUSED -> R.string.timer_resume
        },
    )
}

@Composable
internal fun EditTimerDialog(
    elapsedMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val totalSeconds = elapsedMillis.coerceAtLeast(0) / 1000
    var hours by remember { mutableIntStateOf((totalSeconds / 3600).toInt()) }
    var minutes by remember { mutableIntStateOf(((totalSeconds % 3600) / 60).toInt()) }
    var seconds by remember { mutableIntStateOf((totalSeconds % 60).toInt()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_edit)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWheel(
                    label = stringResource(R.string.timer_hours),
                    maxValue = maxOf(99, hours),
                    value = hours,
                    onValueChange = { hours = it },
                    modifier = Modifier.weight(1f),
                )
                TimeWheel(
                    label = stringResource(R.string.timer_minutes),
                    maxValue = 59,
                    value = minutes,
                    onValueChange = { minutes = it },
                    modifier = Modifier.weight(1f),
                )
                TimeWheel(
                    label = stringResource(R.string.timer_seconds),
                    maxValue = 59,
                    value = seconds,
                    onValueChange = { seconds = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    onConfirm(millis)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeWheel(
    label: String,
    maxValue: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight = 44.dp
    val selected = value.coerceIn(0, maxValue)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    var positioned by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.find { it.index == selected }
        if (item != null) {
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            val delta = item.offset + item.size / 2 - viewportCenter
            if (abs(delta) > 1) {
                listState.scroll { scrollBy(delta.toFloat()) }
            }
        }
        positioned = true
    }
    LaunchedEffect(positioned) {
        if (!positioned) return@LaunchedEffect
        snapshotFlow {
            val layout = listState.layoutInfo
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index
        }
            .distinctUntilChanged()
            .collect { index ->
                if (index != null && index != value.coerceIn(0, maxValue)) onValueChange(index)
            }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(itemHeight * 3),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = itemHeight),
                flingBehavior = rememberSnapFlingBehavior(
                    lazyListState = listState,
                    snapPosition = SnapPosition.Center,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(maxValue + 1) { index ->
                    val distance = abs(index - selected)
                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "%02d".format(index),
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            color = if (distance == 0) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = if (distance == 1) 0.72f else 0.38f,
                                )
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetTimerDialog(
    hasLaps: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var clearLaps by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_reset_title)) },
        text = {
            Column {
                Text(stringResource(R.string.timer_reset_message))
                if (hasLaps) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearLaps, onCheckedChange = { clearLaps = it })
                        Text(stringResource(R.string.timer_reset_clear_laps))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clearLaps) }) {
                Text(stringResource(R.string.timer_reset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

