package com.example.gymlog.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gymlog.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupDataScreen(
    viewModel: DataTransferViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.preview != null) { viewModel.cancelImport() }
    BackHandler(enabled = state.notice != null) { viewModel.dismissNotice() }
    BackHandler(
        enabled = state.preview == null && state.notice == null,
        onBack = onBack,
    )
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.prepareImport(uri) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.data_backup)) },
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
                    text = stringResource(R.string.photo_wall_backup_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.data_export),
                    subtitle = "导出全部训练和身体数据",
                    icon = Icons.Filled.FileUpload,
                    enabled = !state.busy,
                    onClick = { exportLauncher.launch("gymlog-${today}.json") },
                )
            }
            item {
                SettingsLinkRow(
                    title = stringResource(R.string.data_import),
                    subtitle = "从 JSON 备份恢复数据",
                    icon = Icons.Filled.FileDownload,
                    enabled = !state.busy,
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream"),
                        )
                    },
                )
            }
            if (state.busy) {
                item {
                    Text(
                        text = stringResource(R.string.data_busy),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    state.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.data_import_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.data_import_confirm_message,
                        preview.sessionCount,
                        preview.measurementCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.data_import))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    state.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            title = { Text(stringResource(R.string.data_backup)) },
            text = { Text(transferNoticeMessage(notice)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissNotice) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

private val today: String
    get() = LocalDate.now().toString()

@Composable
private fun transferNoticeMessage(notice: TransferNotice): String {
    return when (notice) {
        TransferNotice.Exported -> stringResource(R.string.data_export_ok)
        TransferNotice.Imported -> stringResource(R.string.data_import_ok)
        TransferNotice.ExportFailed -> stringResource(R.string.data_export_failed)
        TransferNotice.ImportFailed -> stringResource(R.string.data_import_failed)
        TransferNotice.InvalidFile -> stringResource(R.string.data_import_invalid)
    }
}
