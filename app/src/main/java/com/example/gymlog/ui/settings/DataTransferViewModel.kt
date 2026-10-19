package com.example.gymlog.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.BackupCodec
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.GymLogSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

enum class TransferNotice {
    Exported,
    Imported,
    ExportFailed,
    ImportFailed,
    InvalidFile,
}

data class ImportPreview(
    val sessionCount: Int,
    val measurementCount: Int,
    val snapshot: GymLogSnapshot,
)

data class DataTransferUiState(
    val busy: Boolean = false,
    val preview: ImportPreview? = null,
    val notice: TransferNotice? = null,
)

class DataTransferViewModel(
    private val app: Application,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DataTransferUiState())
    val state: StateFlow<DataTransferUiState> = mutableState.asStateFlow()

    fun exportTo(uri: Uri) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, notice = null) }
            val notice = withContext(Dispatchers.IO) {
                runCatching {
                    val text = BackupCodec.encode(
                        snapshot = backupRepository.exportSnapshot(),
                        exportedAt = System.currentTimeMillis(),
                    )
                    app.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("NO_STREAM")
                }.fold(
                    onSuccess = { TransferNotice.Exported },
                    onFailure = { TransferNotice.ExportFailed },
                )
            }
            mutableState.update { it.copy(busy = false, notice = notice) }
        }
    }

    fun prepareImport(uri: Uri) {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, notice = null, preview = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = app.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        if (bytes.size > MAX_BYTES) error("TOO_LARGE")
                        bytes.toString(Charsets.UTF_8)
                    } ?: error("NO_STREAM")
                    BackupCodec.decode(text)
                }
            }
            result.fold(
                onSuccess = { snapshot ->
                    mutableState.update {
                        it.copy(
                            busy = false,
                            preview = ImportPreview(
                                sessionCount = snapshot.sessions.size,
                                measurementCount = snapshot.measurements.size,
                                snapshot = snapshot,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    val notice = if (error is JSONException || error is IllegalArgumentException) {
                        TransferNotice.InvalidFile
                    } else {
                        TransferNotice.ImportFailed
                    }
                    mutableState.update { it.copy(busy = false, notice = notice) }
                },
            )
        }
    }

    fun confirmImport() {
        val snapshot = mutableState.value.preview?.snapshot ?: return
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, preview = null) }
            val notice = withContext(Dispatchers.IO) {
                runCatching { backupRepository.importSnapshot(snapshot) }.fold(
                    onSuccess = { TransferNotice.Imported },
                    onFailure = { TransferNotice.ImportFailed },
                )
            }
            mutableState.update { it.copy(busy = false, notice = notice) }
        }
    }

    fun cancelImport() {
        mutableState.update { it.copy(preview = null) }
    }

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    companion object {
        private const val MAX_BYTES = 20 * 1024 * 1024

        fun factory(app: GymLogApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return DataTransferViewModel(app, app.backupRepository) as T
                }
            }
        }
    }
}
