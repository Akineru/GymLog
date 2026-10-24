package com.example.gymlog.ui.photo

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.PhotoWallEntity
import com.example.gymlog.ui.theme.ActionOrange
import com.example.gymlog.ui.theme.GymLogActionButton
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA)
private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoWallScreen(
    viewModel: PhotoWallViewModel,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.wall.collectAsStateWithLifecycle()
    var pickingMonth by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickMultipleVisualMedia(20)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.stageUris(uris) { ok -> if (ok) onOpenEditor() }
    }
    val camera = rememberLauncherForActivityResult(TakePicture()) { saved ->
        if (!saved) {
            viewModel.discardCapture()
            return@rememberLauncherForActivityResult
        }
        viewModel.stageCapture { ok -> if (ok) onOpenEditor() }
    }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_wall_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { pickingMonth = true }) {
                        Text(state.month?.format(monthFormatter) ?: stringResource(R.string.photo_wall_all_dates))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { adding = true },
                containerColor = ActionOrange,
                contentColor = androidx.compose.ui.graphics.Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.photo_wall_add))
            }
        },
    ) { padding ->
        if (state.photos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(GymLogSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            stringResource(R.string.photo_wall_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.photo_wall_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            val rows = remember(state.photos) { groupByMonth(state.photos) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEach { (month, photos) ->
                    item(key = "m-$month", span = { GridItemSpan(3) }) {
                        Text(
                            "${month.format(monthFormatter)} · ${photos.size} 张",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items(photos, key = { it.id }) { photo ->
                        PhotoTile(photo = photo, load = viewModel::loadThumbnail, onClick = { onOpen(photo.id) })
                    }
                }
            }
        }
        state.message?.let { text ->
            AlertDialog(
                onDismissRequest = viewModel::dismissMessage,
                shape = RoundedCornerShape(24.dp),
                text = { Text(text) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.save)) }
                },
            )
        }
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.photo_wall_add),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    TextButton(onClick = {
                        adding = false
                        picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }) { Text(stringResource(R.string.photo_wall_pick)) }
                    TextButton(onClick = {
                        adding = false
                        camera.launch(viewModel.captureUri())
                    }) { Text(stringResource(R.string.photo_wall_camera)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (pickingMonth) {
        AlertDialog(
            onDismissRequest = { pickingMonth = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.photo_wall_pick_month),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.setMonth(null)
                        pickingMonth = false
                    }) { Text(stringResource(R.string.photo_wall_all_dates)) }
                    state.months.forEach { month ->
                        TextButton(onClick = {
                            viewModel.setMonth(month)
                            pickingMonth = false
                        }) { Text(month.format(monthFormatter)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingMonth = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PhotoTile(
    photo: PhotoWallEntity,
    load: (PhotoWallEntity) -> Bitmap?,
    onClick: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(null, photo.id, photo.updatedAt) {
        value = withContext(Dispatchers.IO) { runCatching { load(photo) }.getOrNull() }
    }
    val date = LocalDate.ofEpochDay(photo.dateEpochDay)
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(stringResource(R.string.photo_wall_missing), style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            date.format(dayFormatter),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (photo.note.isNotBlank()) {
            Text(
                photo.note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photo: PhotoWallEntity?,
    loadFull: (PhotoWallEntity) -> Bitmap?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangeDate: (Long) -> Unit,
    onChangeNote: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    BackHandler(enabled = confirmDelete || pickingDate || editingNote) {
        confirmDelete = false
        pickingDate = false
        editingNote = false
    }
    BackHandler(enabled = !confirmDelete && !pickingDate && !editingNote, onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_wall_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
            )
        },
    ) { padding ->
        if (photo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.photo_wall_missing))
            }
            return@Scaffold
        }
        val bitmap by produceState<Bitmap?>(null, photo.id, photo.updatedAt) {
            value = withContext(Dispatchers.IO) { runCatching { loadFull(photo) }.getOrNull() }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val image = bitmap
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(stringResource(R.string.photo_wall_missing))
                }
            }
            GymLogCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(GymLogSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = LocalDate.ofEpochDay(photo.dateEpochDay).format(dayFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = photo.note.ifBlank { stringResource(R.string.photo_wall_no_note) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            GymLogActionButton(
                text = stringResource(R.string.photo_wall_edit),
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { pickingDate = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.photo_wall_change_date))
                }
                TextButton(onClick = { editingNote = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.photo_wall_change_note))
                }
            }
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.photo_wall_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.photo_wall_delete)) },
            text = { Text(stringResource(R.string.photo_wall_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.photo_wall_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (pickingDate && photo != null) {
        PhotoDateDialog(
            initialEpochDay = photo.dateEpochDay,
            onDismiss = { pickingDate = false },
            onConfirm = {
                pickingDate = false
                onChangeDate(it)
            },
        )
    }
    if (editingNote && photo != null) {
        NoteDialog(
            initial = photo.note,
            onDismiss = { editingNote = false },
            onConfirm = {
                editingNote = false
                onChangeNote(it)
            },
        )
    }
}

internal fun groupByMonth(photos: List<PhotoWallEntity>): List<Pair<YearMonth, List<PhotoWallEntity>>> {
    return photos.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
        .toSortedMap(compareByDescending { it })
        .map { it.key to it.value }
}
