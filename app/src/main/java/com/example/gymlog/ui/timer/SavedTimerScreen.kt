package com.example.gymlog.ui.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.TimerLapEntity
import com.example.gymlog.data.TrainingTimerLogic
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTimerScreen(
    viewModel: TrainingTimerViewModel,
    recordId: String,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved = state.saved.find { it.record.id == recordId }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    BackHandler(enabled = editing || confirmingDelete) {
        editing = false
        confirmingDelete = false
    }
    BackHandler(enabled = !editing && !confirmingDelete, onBack = onBack)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_record_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    if (saved != null) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (saved == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.timer_saved_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            val laps = saved.laps.sortedBy { it.lapIndex }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = "训练计时",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFBBD0EC),
                        )
                        Text(
                            text = TrainingTimerLogic.formatDuration(saved.record.durationMillis),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = { editing = true })
                                .padding(vertical = 2.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = LocalDate.ofEpochDay(saved.record.dateEpochDay)
                                    .format(savedTimerDateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBBD0EC),
                            )
                            Text(
                                text = if (laps.isEmpty()) {
                                    "无分段"
                                } else {
                                    "${laps.size} 圈"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBBD0EC),
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.timer_laps),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (laps.isNotEmpty()) {
                    val newestIndex = laps.maxOf { it.lapIndex }
                    items(laps.asReversed(), key = { it.id }) { lap ->
                        SavedLapCard(lap = lap, newest = lap.lapIndex == newestIndex)
                    }
                } else {
                    item {
                        GymLogCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.timer_laps_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(GymLogSpacing.md),
                            )
                        }
                    }
                }
            }
        }
    }
    if (editing && saved != null) {
        EditTimerDialog(
            elapsedMillis = saved.record.durationMillis,
            onDismiss = { editing = false },
            onConfirm = { millis ->
                viewModel.updateSavedDuration(recordId, millis)
                editing = false
            },
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.timer_delete_title)) },
            text = { Text(stringResource(R.string.timer_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        viewModel.deleteSaved(recordId)
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SavedLapCard(lap: TimerLapEntity, newest: Boolean) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(GymLogSpacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.timer_lap_index, lap.lapIndex) +
                    if (newest) " · " + stringResource(R.string.timer_latest) else "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.timer_lap_interval,
                    TrainingTimerLogic.formatDuration(lap.intervalMillis),
                ),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    R.string.timer_lap_elapsed,
                    TrainingTimerLogic.formatDuration(lap.elapsedMillis),
                ),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val savedTimerDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
