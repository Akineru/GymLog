package com.example.gymlog.ui.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gymlog.R
import com.example.gymlog.data.Gender
import com.example.gymlog.data.MineDurationUnit
import com.example.gymlog.ui.theme.dashboardCardBorder
import com.example.gymlog.ui.theme.dashboardCardColors
import com.example.gymlog.ui.theme.dashboardCardElevation
import com.example.gymlog.ui.theme.dashboardCardShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    viewModel: MineViewModel,
    onOpenBodyRecord: () -> Unit,
    onOpenPhotoWall: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editingProfile by remember { mutableStateOf(false) }
    var durationInMinutes by remember { mutableStateOf(false) }
    var showTrainingYears by remember { mutableStateOf(false) }
    var showBodyWeight by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_mine)) },
                actions = { SettingsTopBarAction(onOpenSettings) },
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
                ProfileStatsCard(
                    name = state.displayName,
                    avatarPath = state.avatarPath,
                    gender = state.gender,
                    ageYears = state.ageYears,
                    state = state,
                    showTrainingYears = showTrainingYears,
                    onToggleTrainingSpan = { showTrainingYears = !showTrainingYears },
                    durationInMinutes = durationInMinutes,
                    onToggleDuration = { durationInMinutes = !durationInMinutes },
                    showBodyWeight = showBodyWeight,
                    onToggleBodyMetric = { showBodyWeight = !showBodyWeight },
                    onClick = { editingProfile = true },
                )
            }
            item {
                BodyRecordEntry(onClick = onOpenBodyRecord)
            }
            item {
                PhotoWallEntry(onClick = onOpenPhotoWall)
            }
        }
    }
    if (editingProfile) {
        ProfileEditorDialog(
            initialName = state.displayName.orEmpty(),
            initialGender = state.gender,
            avatarPath = state.avatarPath,
            onDismiss = { editingProfile = false },
            onSave = { name, gender, bytes, clearAvatar ->
                viewModel.saveProfile(name, gender, bytes, clearAvatar)
                editingProfile = false
            },
        )
    }
}

@Composable
private fun ProfileStatsCard(
    name: String?,
    avatarPath: String?,
    gender: Gender?,
    ageYears: Int?,
    state: MineUiState,
    showTrainingYears: Boolean,
    onToggleTrainingSpan: () -> Unit,
    durationInMinutes: Boolean,
    onToggleDuration: () -> Unit,
    showBodyWeight: Boolean,
    onToggleBodyMetric: () -> Unit,
    onClick: () -> Unit,
) {
    val bodyValue = if (showBodyWeight) state.bodyWeightText else state.bodyFatText
    val durationValue = if (durationInMinutes) state.durationMinutesValue else state.durationValue
    val durationUnit = if (durationInMinutes) MineDurationUnit.MINUTE else state.durationUnit
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF060B14), Color(0xFF11263F), Color(0xFF234A73)),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(
                path = avatarPath,
                bytes = null,
                modifier = Modifier.size(56.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name ?: stringResource(R.string.mine_profile_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    gender?.let {
                        Icon(
                            imageVector = genderIcon(it),
                            contentDescription = genderLabel(it),
                            tint = if (it == Gender.FEMALE) {
                                Color(0xFFFFA9C8)
                            } else {
                                Color(0xFFBBD0EC)
                            },
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(18.dp),
                        )
                    }
                }
                Text(
                    text = when {
                        ageYears != null -> stringResource(R.string.mine_age, ageYears)
                        gender == null -> stringResource(R.string.mine_profile_empty)
                        else -> genderLabel(gender)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBBD0EC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFBBD0EC),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.16f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DarkStatItem(
                icon = Icons.Filled.Today,
                label = stringResource(
                    if (showTrainingYears) R.string.mine_stat_years else R.string.mine_stat_days,
                ),
                value = if (showTrainingYears) state.trainingYears else state.trainingDays.toString(),
                unit = stringResource(
                    if (showTrainingYears) R.string.mine_unit_year else R.string.mine_unit_day,
                ),
                onClick = onToggleTrainingSpan,
                modifier = Modifier.weight(1f),
            )
            StatsDivider()
            DarkStatItem(
                icon = Icons.Filled.Schedule,
                label = stringResource(R.string.mine_stat_duration),
                value = durationValue,
                unit = durationUnitLabel(durationUnit),
                onClick = onToggleDuration,
                modifier = Modifier.weight(1f),
            )
            StatsDivider()
            DarkStatItem(
                icon = if (showBodyWeight) Icons.Filled.MonitorWeight else Icons.Filled.Percent,
                label = stringResource(
                    if (showBodyWeight) R.string.mine_stat_weight else R.string.mine_stat_fat,
                ),
                value = bodyValue ?: stringResource(R.string.mine_value_empty),
                unit = when {
                    bodyValue == null -> ""
                    showBodyWeight -> stringResource(R.string.mine_unit_kg)
                    else -> stringResource(R.string.mine_unit_percent)
                },
                onClick = onToggleBodyMetric,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(38.dp)
            .background(Color.White.copy(alpha = 0.14f)),
    )
}

@Composable
private fun DarkStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9FC4FF),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBBD0EC),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD6E4F5),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoWallEntry(onClick: () -> Unit) {
    DashboardCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.photo_wall_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.photo_wall_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BodyRecordEntry(onClick: () -> Unit) {
    DashboardCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.mine_body_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.mine_body_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DashboardCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = dashboardCardShape(),
        colors = dashboardCardColors(),
        elevation = dashboardCardElevation(),
        border = dashboardCardBorder(),
    ) {
        content()
    }
}

@Composable
private fun genderLabel(gender: Gender): String {
    return when (gender) {
        Gender.MALE -> stringResource(R.string.gender_male)
        Gender.FEMALE -> stringResource(R.string.gender_female)
        Gender.OTHER -> stringResource(R.string.gender_other)
    }
}

private fun genderIcon(gender: Gender): ImageVector {
    return when (gender) {
        Gender.MALE -> Icons.Filled.Male
        Gender.FEMALE -> Icons.Filled.Female
        Gender.OTHER -> Icons.Filled.Person
    }
}

@Composable
private fun durationUnitLabel(unit: MineDurationUnit): String {
    return when (unit) {
        MineDurationUnit.SECOND -> stringResource(R.string.mine_unit_second)
        MineDurationUnit.MINUTE -> stringResource(R.string.mine_unit_minute)
        MineDurationUnit.HOUR -> stringResource(R.string.mine_unit_hour)
    }
}

@Composable
private fun AvatarImage(
    path: String?,
    bytes: ByteArray?,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(path, bytes) {
        when {
            bytes != null -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            path != null -> BitmapFactory.decodeFile(path)
            else -> null
        }
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditorDialog(
    initialName: String,
    initialGender: Gender?,
    avatarPath: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, gender: Gender?, avatarBytes: ByteArray?, clearAvatar: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(initialGender) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var clearAvatar by remember { mutableStateOf(false) }
    var showAvatarEditor by remember { mutableStateOf(false) }
    val previewBytes = pendingBytes
    val previewPath = if (clearAvatar || previewBytes != null) null else avatarPath
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.mine_edit_profile),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .clickable(onClick = { showAvatarEditor = true }),
                    ) {
                        AvatarImage(
                            path = previewPath,
                            bytes = previewBytes,
                            modifier = Modifier.size(76.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text(stringResource(R.string.mine_name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.field_gender),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Gender.entries.forEach { option ->
                        FilterChip(
                            selected = gender == option,
                            onClick = { gender = if (gender == option) null else option },
                            label = { Text(genderLabel(option)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, gender, pendingBytes, clearAvatar) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
    if (showAvatarEditor) {
        AvatarEditorDialog(
            avatarPath = avatarPath,
            pendingBytes = pendingBytes,
            clearAvatar = clearAvatar,
            onDismiss = { showAvatarEditor = false },
            onCropped = { bytes ->
                pendingBytes = bytes
                clearAvatar = false
            },
            onRemove = {
                pendingBytes = null
                clearAvatar = true
            },
        )
    }
}

@Composable
private fun AvatarEditorDialog(
    avatarPath: String?,
    pendingBytes: ByteArray?,
    clearAvatar: Boolean,
    onDismiss: () -> Unit,
    onCropped: (ByteArray) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropSource by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeAvatarForCrop(context, uri) }
            if (bitmap != null) cropSource = bitmap
        }
    }
    val previewBytes = pendingBytes
    val previewPath = if (clearAvatar || previewBytes != null) null else avatarPath
    val hasAvatar = previewBytes != null || previewPath != null
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.mine_avatar_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AvatarImage(
                        path = previewPath,
                        bytes = previewBytes,
                        modifier = Modifier.size(180.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Text(stringResource(R.string.mine_pick_avatar))
                    }
                    if (hasAvatar) {
                        TextButton(onClick = onRemove) {
                            Text(stringResource(R.string.mine_remove_avatar))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.mine_avatar_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
    cropSource?.let { bitmap ->
        AvatarCropDialog(
            bitmap = bitmap,
            onDismiss = { cropSource = null },
            onCropped = { bytes ->
                onCropped(bytes)
                cropSource = null
            },
        )
    }
}
