package com.example.gymlog.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import com.example.gymlog.data.ThemeMode
import com.example.gymlog.ui.theme.GymLogCard
import com.example.gymlog.ui.theme.GymLogSpacing
import java.time.LocalDate

@Composable
fun SettingsTopBarAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_title),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    var clearStep by rememberSaveable { mutableIntStateOf(0) }
    val dialogOpen = clearStep != 0 || uiState.notice != null
    BackHandler(enabled = uiState.notice != null) { viewModel.dismissNotice() }
    BackHandler(enabled = uiState.notice == null && clearStep != 0) { clearStep = 0 }
    BackHandler(enabled = !dialogOpen, onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.appearance_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }
            item {
                GymLogCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = appearance.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.entries.size,
                                    ),
                                    icon = {},
                                    label = { Text(themeModeLabel(mode)) },
                                )
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.wallpaper_colors),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.wallpaper_colors_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Switch(
                                    checked = appearance.useWallpaperColors,
                                    onCheckedChange = viewModel::setUseWallpaperColors,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.settings_section_data),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.data_backup),
                    subtitle = "导出或导入本地 JSON 备份",
                    icon = Icons.Filled.Backup,
                    onClick = onOpenBackup,
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.settings_history),
                    subtitle = "查看身体记录历史",
                    icon = Icons.Filled.History,
                    onClick = onOpenHistory,
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.about_title),
                    subtitle = "版本信息与更新记录",
                    icon = Icons.Filled.Info,
                    onClick = onOpenAbout,
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.clear_data),
                    subtitle = stringResource(R.string.clear_data_hint),
                    icon = Icons.Filled.DeleteOutline,
                    titleColor = MaterialTheme.colorScheme.error,
                    enabled = !uiState.busy,
                    onClick = { clearStep = 1 },
                )
            }
            if (uiState.busy) {
                item {
                    Text(
                        text = stringResource(R.string.clear_data_busy),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (clearStep == 1) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.clear_data_understand_title)) },
            text = { Text(stringResource(R.string.clear_data_understand_message)) },
            confirmButton = {
                TextButton(onClick = { clearStep = 2 }) {
                    Text(stringResource(R.string.clear_data_understand_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (clearStep == 2) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.clear_data_confirm_title)) },
            text = { Text(stringResource(R.string.clear_data_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearStep = 0
                        viewModel.clearLocalData()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.clear_data_confirm_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    uiState.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.clear_data)) },
            text = {
                Text(
                    stringResource(
                        if (notice == ClearNotice.Cleared) {
                            R.string.clear_data_done
                        } else {
                            R.string.clear_data_failed
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }

}

@Composable
private fun themeModeLabel(mode: ThemeMode): String {
    return stringResource(
        when (mode) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        },
    )
}

@Composable
internal fun SettingsLinkRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    GymLogCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = GymLogSpacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (titleColor == MaterialTheme.colorScheme.error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon != null) GymLogSpacing.sm else 0.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
