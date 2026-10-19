package com.example.gymlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymlog.GymLogApplication
import com.example.gymlog.data.AppearanceSettings
import com.example.gymlog.data.AppearanceStore
import com.example.gymlog.data.BackupRepository
import com.example.gymlog.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ClearNotice {
    Cleared,
    Failed,
}

data class SettingsUiState(
    val busy: Boolean = false,
    val notice: ClearNotice? = null,
)

class SettingsViewModel(
    private val appearanceStore: AppearanceStore,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    val appearance: StateFlow<AppearanceSettings> = appearanceStore.state

    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        appearanceStore.setThemeMode(mode)
    }

    fun setUseWallpaperColors(enabled: Boolean) {
        appearanceStore.setUseWallpaperColors(enabled)
    }

    fun clearLocalData() {
        if (mutableState.value.busy) return
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, notice = null) }
            val notice = withContext(Dispatchers.IO) {
                runCatching { backupRepository.clearLocalData() }.fold(
                    onSuccess = { ClearNotice.Cleared },
                    onFailure = { ClearNotice.Failed },
                )
            }
            mutableState.update { it.copy(busy = false, notice = notice) }
        }
    }

    fun dismissNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    companion object {
        fun factory(app: GymLogApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(app.appearanceStore, app.backupRepository) as T
                }
            }
        }
    }
}
