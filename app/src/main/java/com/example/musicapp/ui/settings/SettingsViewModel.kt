package com.example.musicapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.ObserveSettingsUseCase
import com.example.musicapp.domain.usecase.RefreshLibrariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val connectedDriveFolderName: String = "Not connected",
    val selectedFolderCount: Int = 0,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeSettingsUseCase()
        .map { settings ->
            SettingsUiState(
                isDarkTheme = settings.isDarkTheme,
                connectedDriveFolderName = settings.connectedDriveFolder?.folderName ?: "Not connected",
                selectedFolderCount = settings.selectedFolders.size,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshLibraries() {
        viewModelScope.launch { refreshLibrariesUseCase() }
    }
}

