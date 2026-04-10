package com.example.musicapp.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.model.FolderConnection
import com.example.musicapp.domain.usecase.ObserveSettingsUseCase
import com.example.musicapp.domain.usecase.RefreshLibrariesUseCase
import com.example.musicapp.domain.usecase.UpdateDriveFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDriveConnected: Boolean = false,
    val connectedDriveName: String = "Google Drive",
    val connectedDriveFolderName: String = "Not connected",
    val connectedDriveEmail: String = "",
    val selectedFolderCount: Int = 0,
    val isWorking: Boolean = false,
)

sealed interface SettingsEvent {
    data class LaunchDriveAuthorization(
        val pendingIntent: android.app.PendingIntent,
    ) : SettingsEvent

    data class Message(
        val text: String,
    ) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val updateDriveFolderUseCase: UpdateDriveFolderUseCase,
    private val googleDriveAuthManager: GoogleDriveAuthManager,
) : ViewModel() {

    private val workingState = MutableStateFlow(false)
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()
    private var pendingDriveAccount: DriveAccountProfile? = null

    private val baseState: StateFlow<SettingsUiState> = observeSettingsUseCase()
        .map { settings ->
            SettingsUiState(
                isDriveConnected = settings.connectedDriveFolder?.active == true,
                connectedDriveName = settings.connectedDriveFolder?.folderName ?: "Google Drive",
                connectedDriveFolderName = settings.connectedDriveFolder?.folderName ?: "Not connected",
                connectedDriveEmail = settings.connectedDriveFolder?.accountEmail.orEmpty(),
                selectedFolderCount = settings.selectedFolders.size,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
    val screenState: StateFlow<SettingsUiState> = combine(baseState, workingState) { ui, working ->
        ui.copy(isWorking = working)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshLibraries() {
        viewModelScope.launch { refreshLibrariesUseCase() }
    }

    fun connectDrive(activity: Activity) {
        viewModelScope.launch {
            if (workingState.value) return@launch
            workingState.value = true
            runCatching {
                when (val result = googleDriveAuthManager.beginConnection(activity)) {
                    is DriveConnectResult.Authorized -> finalizeConnection(result.account)
                    is DriveConnectResult.RequiresResolution -> {
                        pendingDriveAccount = result.account
                        _events.emit(SettingsEvent.LaunchDriveAuthorization(result.pendingIntent))
                    }
                }
            }.onFailure { throwable ->
                workingState.value = false
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not connect Google Drive."))
            }
        }
    }

    fun completeDriveAuthorization(activity: Activity, data: Intent?) {
        val account = pendingDriveAccount ?: return
        viewModelScope.launch {
            runCatching {
                googleDriveAuthManager.completeConnection(activity, data)
                finalizeConnection(account)
            }.onFailure { throwable ->
                workingState.value = false
                _events.emit(SettingsEvent.Message(throwable.message ?: "Google Drive authorization was cancelled."))
            }
            pendingDriveAccount = null
        }
    }

    fun disconnectDrive(activity: Activity) {
        viewModelScope.launch {
            workingState.value = true
            runCatching {
                googleDriveAuthManager.clearSession(activity)
                updateDriveFolderUseCase(null)
                refreshLibrariesUseCase()
                _events.emit(SettingsEvent.Message("Google Drive disconnected."))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not disconnect Google Drive."))
            }
            workingState.value = false
        }
    }

    private suspend fun finalizeConnection(account: DriveAccountProfile) {
        updateDriveFolderUseCase(
            FolderConnection(
                provider = "google_drive",
                folderId = "root",
                folderName = account.displayName,
                accountEmail = account.email,
                active = true,
            ),
        )
        refreshLibrariesUseCase()
        workingState.value = false
        _events.emit(SettingsEvent.Message("Connected to Google Drive as ${account.email}."))
    }
}
