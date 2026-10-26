package com.example.gymlog.ui.photo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.LocalDate

/**
 * Single-photo editor: image stage plus tool panel, with the photo's date and
 * note edited in place. Gesture handling lives in [PhotoEditorCanvas], the
 * shared dialogs in [PhotoDialogs].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoEditorScreen(
    viewModel: PhotoWallViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.editorState.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    var askText by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }
    val current = state
    BackHandler(enabled = confirmLeave || askText || pickingDate) {
        confirmLeave = false
        askText = false
        pickingDate = false
    }
    BackHandler(enabled = current?.dirty == true && !confirmLeave && !askText && !pickingDate) {
        confirmLeave = true
    }
    BackHandler(enabled = current?.dirty != true && !confirmLeave && !askText && !pickingDate, onBack = {
        viewModel.skipOrCancel(onClose)
    })
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.photo_wall_opening))
        }
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (current.total > 1) {
                            stringResource(R.string.photo_wall_editing_progress, current.index, current.total)
                        } else {
                            stringResource(R.string.photo_wall_editor)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (current.dirty) confirmLeave = true else viewModel.skipOrCancel(onClose)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = current.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.photo_wall_undo))
                    }
                    IconButton(onClick = viewModel::redo, enabled = current.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.photo_wall_redo))
                    }
                    TextButton(onClick = viewModel::resetToOpened) { Text(stringResource(R.string.photo_wall_reset)) }
                    TextButton(
                        enabled = !current.saving,
                        onClick = { viewModel.save { ok -> if (ok && viewModel.editorState.value == null) onClose() } },
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (current.continueFromSaved) {
                Text(
                    stringResource(R.string.photo_wall_edit_saved_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoEditorCanvas(
                bitmap = current.bitmap,
                tool = current.tool,
                pendingText = pendingText,
                onPlaceText = { x, y ->
                    viewModel.placeText(pendingText, x, y)
                    pendingText = ""
                },
                onDraw = viewModel::draw,
                onCrop = viewModel::crop,
                onMosaic = viewModel::mosaic,
                onBlur = viewModel::blur,
            )
            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(horizontal = GymLogSpacing.sm, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "工具",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            PhotoTool.MOSAIC to R.string.photo_wall_mosaic,
                            PhotoTool.BLUR to R.string.photo_wall_blur,
                            PhotoTool.DRAW to R.string.photo_wall_draw,
                            PhotoTool.TEXT to R.string.photo_wall_text,
                            PhotoTool.CROP to R.string.photo_wall_crop,
                        ).forEach { (tool, label) ->
                            FilterChip(
                                selected = current.tool == tool,
                                onClick = { viewModel.setTool(tool) },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = viewModel::rotate) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                            Text(
                                stringResource(R.string.photo_wall_rotate),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        if (current.tool == PhotoTool.TEXT) {
                            Button(onClick = { askText = true }) {
                                Text(stringResource(R.string.photo_wall_enter_text))
                            }
                        }
                        if (current.tool == PhotoTool.BLUR) {
                            TextButton(
                                onClick = {
                                    viewModel.setBlurRadius(
                                        if (current.blurRadius >= 16) 4 else current.blurRadius * 2,
                                    )
                                },
                            ) {
                                Text(stringResource(R.string.photo_wall_blur_strength, current.blurRadius))
                            }
                        }
                    }
                    if (current.tool == PhotoTool.DRAW) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                0xFF172033.toInt(),
                                0xFFFFFFFF.toInt(),
                                0xFFE53935.toInt(),
                                0xFF246BFD.toInt(),
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            if (current.brushColor == color) 2.dp else 1.dp,
                                            if (current.brushColor == color) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            CircleShape,
                                        )
                                        .clickable { viewModel.setBrushColor(color) },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.photo_wall_censor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GymLogCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickingDate = true },
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
                        text = "照片日期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    )
                    Text(
                        text = LocalDate.ofEpochDay(current.dateEpochDay).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedTextField(
                value = current.note,
                onValueChange = viewModel::setNote,
                label = { Text(stringResource(R.string.photo_wall_note)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
            )
        }
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            shape = RoundedCornerShape(24.dp),
            text = { Text(stringResource(R.string.photo_wall_discard)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    viewModel.skipOrCancel(onClose)
                }) {
                    Text(
                        text = stringResource(R.string.photo_wall_discard_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (askText) {
        NoteDialog(
            initial = "",
            title = stringResource(R.string.photo_wall_enter_text),
            onDismiss = { askText = false },
            onConfirm = {
                pendingText = it
                askText = false
            },
        )
    }
    if (pickingDate) {
        PhotoDateDialog(
            initialEpochDay = current.dateEpochDay,
            onDismiss = { pickingDate = false },
            onConfirm = {
                viewModel.setDate(it)
                pickingDate = false
            },
        )
    }
}
