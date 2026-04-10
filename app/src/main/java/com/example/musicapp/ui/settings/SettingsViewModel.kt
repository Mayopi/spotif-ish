package com.example.musicapp.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.core.DriveAuthSessionStore
import com.example.musicapp.data.repository.DriveTokenStore
import com.example.musicapp.debug.DriveDebugLogger
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.FolderConnection
import com.example.musicapp.domain.usecase.EnqueueDriveLibraryRefreshUseCase
import com.example.musicapp.domain.usecase.ListDriveFoldersUseCase
import com.example.musicapp.domain.usecase.ObserveDriveSyncStateUseCase
import com.example.musicapp.domain.usecase.ObserveSettingsUseCase
import com.example.musicapp.domain.usecase.RefreshLibrariesUseCase
import com.example.musicapp.domain.usecase.UpdateDriveFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isDriveConnected: Boolean = false,
    val connectedDriveName: String = "Google Drive",
    val connectedDriveFolderName: String = "Not connected",
    val connectedDriveEmail: String = "",
    val selectedFolderCount: Int = 0,
    val isWorking: Boolean = false,
    val isFolderLoading: Boolean = false,
    val isDriveSyncing: Boolean = false,
    val driveSyncStatusText: String = "Idle",
    val availableDriveFolders: List<DriveFolder> = emptyList(),
    val isFolderPickerVisible: Boolean = false,
    val currentDriveFolderPath: String = "My Drive",
    val canNavigateUpFolders: Boolean = false,
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
    private val observeDriveSyncStateUseCase: ObserveDriveSyncStateUseCase,
    private val enqueueDriveLibraryRefreshUseCase: EnqueueDriveLibraryRefreshUseCase,
    private val updateDriveFolderUseCase: UpdateDriveFolderUseCase,
    private val listDriveFoldersUseCase: ListDriveFoldersUseCase,
    private val googleDriveAuthManager: GoogleDriveAuthManager,
    private val driveAuthSessionStore: DriveAuthSessionStore,
    private val driveTokenStore: DriveTokenStore,
    private val driveDebugLogger: DriveDebugLogger,
) : ViewModel() {

    private data class FolderBrowserNode(
        val id: String,
        val path: String,
    )

    private companion object {
        private const val ROOT_ID = "root"
        private const val ROOT_PATH = "My Drive"
    }

    private val workingState = MutableStateFlow(false)
    private val folderLoadingState = MutableStateFlow(false)
    private val driveFoldersState = MutableStateFlow<List<DriveFolder>>(emptyList())
    private val folderPickerVisibleState = MutableStateFlow(false)
    private val currentFolderState = MutableStateFlow(FolderBrowserNode(id = ROOT_ID, path = ROOT_PATH))
    private val folderHistoryState = MutableStateFlow<List<FolderBrowserNode>>(emptyList())
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
                selectedFolderCount = if (settings.connectedDriveFolder != null) 1 else 0,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())
    private val syncAwareState = combine(
        baseState,
        observeDriveSyncStateUseCase(),
    ) { ui, syncState ->
        ui.copy(
            isDriveSyncing = syncState.isSyncing,
            driveSyncStatusText = when {
                syncState.isSyncing -> "${syncState.processedFileCount} files processed"
                !syncState.lastError.isNullOrBlank() -> syncState.lastError.orEmpty()
                syncState.lastSyncedSongCount > 0 -> "${syncState.lastSyncedSongCount} tracks synced"
                else -> "Ready to sync"
            },
        )
    }

    private val pickerState = combine(
        syncAwareState,
        workingState,
        folderLoadingState,
        driveFoldersState,
        folderPickerVisibleState,
    ) { ui, working, folderLoading, folders, pickerVisible ->
        ui.copy(
            isWorking = working,
            isFolderLoading = folderLoading,
            availableDriveFolders = folders,
            isFolderPickerVisible = pickerVisible,
        )
    }

    val screenState: StateFlow<SettingsUiState> = combine(
        pickerState,
        currentFolderState,
        folderHistoryState,
    ) { ui, currentFolder, history ->
        ui.copy(
            currentDriveFolderPath = currentFolder.path,
            canNavigateUpFolders = history.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshLibraries() {
        viewModelScope.launch {
            driveDebugLogger.log("refresh_libraries", "Manual Drive sync requested")
            enqueueDriveLibraryRefreshUseCase()
        }
    }

    fun connectDrive(activity: Activity) {
        viewModelScope.launch {
            if (workingState.value) return@launch
            workingState.value = true
            driveDebugLogger.log("connect_drive", "Connect requested. logFile=${driveDebugLogger.path()}")
            runCatching {
                when (val result = withTimeout(30_000) { googleDriveAuthManager.beginConnection(activity) }) {
                    is DriveConnectResult.Authorized -> finalizeConnection(result.account, result.accessToken)
                    is DriveConnectResult.RequiresResolution -> {
                        pendingDriveAccount = result.account
                        workingState.value = false
                        driveDebugLogger.log("connect_drive_resolution", "Pending authorization launched for ${result.account.email}")
                        _events.emit(SettingsEvent.LaunchDriveAuthorization(result.pendingIntent))
                    }
                }
            }.onFailure { throwable ->
                workingState.value = false
                driveDebugLogger.logError("connect_drive_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not connect Google Drive."))
            }
        }
    }

    fun completeDriveAuthorization(activity: Activity, data: Intent?) {
        val account = pendingDriveAccount ?: return
        viewModelScope.launch {
            workingState.value = true
            driveDebugLogger.log("complete_authorization", "Completing authorization for ${account.email}")
            runCatching {
                val token = withTimeout(30_000) { googleDriveAuthManager.completeConnection(activity, data) }
                finalizeConnection(account, token)
            }.onFailure { throwable ->
                workingState.value = false
                driveDebugLogger.logError("complete_authorization_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Google Drive authorization was cancelled."))
            }
            pendingDriveAccount = null
        }
    }

    fun disconnectDrive(activity: Activity) {
        viewModelScope.launch {
            workingState.value = true
            driveDebugLogger.log("disconnect_drive", "Disconnect requested")
            runCatching {
                googleDriveAuthManager.clearSession(activity)
                driveAuthSessionStore.clear()
                driveTokenStore.clear()
                pendingDriveAccount = null
                updateDriveFolderUseCase(null)
                driveFoldersState.value = emptyList()
                folderPickerVisibleState.value = false
                currentFolderState.value = FolderBrowserNode(id = ROOT_ID, path = ROOT_PATH)
                folderHistoryState.value = emptyList()
                refreshLibrariesUseCase()
                driveDebugLogger.log("disconnect_drive_complete", "Drive disconnected and local session cleared")
                _events.emit(SettingsEvent.Message("Google Drive disconnected."))
            }.onFailure { throwable ->
                driveDebugLogger.logError("disconnect_drive_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not disconnect Google Drive."))
            }
            workingState.value = false
        }
    }

    fun openFolderPicker() {
        viewModelScope.launch {
            driveDebugLogger.log("open_folder_picker", "Folder picker requested")
            runCatching {
                resetFolderBrowser()
                folderPickerVisibleState.value = true
                loadCurrentFolder()
                driveDebugLogger.log("open_folder_picker_complete", "Loaded ${driveFoldersState.value.size} folders at ${currentFolderState.value.path}")
            }.onFailure { throwable ->
                driveDebugLogger.logError("open_folder_picker_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not load Drive folders."))
            }
        }
    }

    fun dismissFolderPicker() {
        folderPickerVisibleState.value = false
    }

    fun navigateIntoDriveFolder(folder: DriveFolder) {
        viewModelScope.launch {
            folderHistoryState.value = folderHistoryState.value + currentFolderState.value
            currentFolderState.value = FolderBrowserNode(id = folder.id, path = folder.path)
            driveDebugLogger.log("navigate_drive_folder", "Navigating into ${folder.path}")
            runCatching {
                loadCurrentFolder()
            }.onFailure { throwable ->
                driveDebugLogger.logError("navigate_drive_folder_failed", throwable)
                val history = folderHistoryState.value
                if (history.isNotEmpty()) {
                    currentFolderState.value = history.last()
                    folderHistoryState.value = history.dropLast(1)
                }
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not open folder."))
            }
        }
    }

    fun navigateUpDriveFolders() {
        val history = folderHistoryState.value
        if (history.isEmpty()) return
        viewModelScope.launch {
            val parent = history.last()
            folderHistoryState.value = history.dropLast(1)
            currentFolderState.value = parent
            driveDebugLogger.log("navigate_drive_folder_up", "Navigating to ${parent.path}")
            runCatching {
                loadCurrentFolder()
            }.onFailure { throwable ->
                driveDebugLogger.logError("navigate_drive_folder_up_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not open parent folder."))
            }
        }
    }

    fun selectCurrentDriveFolder() {
        viewModelScope.launch {
            val currentSettings = baseState.value
            val currentFolder = currentFolderState.value
            folderLoadingState.value = true
            driveDebugLogger.log("select_drive_folder", "Selecting ${currentFolder.path}")
            runCatching {
                updateDriveFolderUseCase(
                    FolderConnection(
                        provider = "google_drive",
                        folderId = currentFolder.id,
                        folderName = currentFolder.path,
                        accountEmail = currentSettings.connectedDriveEmail.ifBlank { null },
                        active = true,
                    ),
                )
                folderPickerVisibleState.value = false
                driveDebugLogger.log("select_drive_folder_complete", "Selected ${currentFolder.path}")
                enqueueDriveLibraryRefreshUseCase()
                _events.emit(SettingsEvent.Message("Selected ${currentFolder.path}. Drive sync started in background."))
            }.onFailure { throwable ->
                driveDebugLogger.logError("select_drive_folder_failed", throwable)
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not select folder."))
            }
            folderLoadingState.value = false
        }
    }

    private suspend fun finalizeConnection(account: DriveAccountProfile, accessToken: String?) {
        driveDebugLogger.log("finalize_connection", "Persisting Drive connection for ${account.email}, tokenPresent=${!accessToken.isNullOrBlank()}")
        updateDriveFolderUseCase(
            FolderConnection(
                provider = "google_drive",
                folderId = "root",
                folderName = "My Drive",
                accountEmail = account.email,
                active = true,
            ),
        )
        if (!accessToken.isNullOrBlank()) {
            driveAuthSessionStore.update(account.email, accessToken)
            // Persist immediately so the very first sync after a process restart can
            // proceed without waiting for the silent re-auth path to kick in.
            driveTokenStore.save(account.email, accessToken)
        }
        driveDebugLogger.log("finalize_connection_saved", "Drive connection saved. Skipping automatic library refresh until folder selection.")
        workingState.value = false
        driveDebugLogger.log("finalize_connection_complete", "Drive connected for ${account.email}")
        _events.emit(SettingsEvent.Message("Connected to Google Drive as ${account.email}. Choose a folder to import music."))
    }

    private suspend fun loadCurrentFolder() {
        folderLoadingState.value = true
        try {
            val accountEmail = baseState.value.connectedDriveEmail.takeIf { it.isNotBlank() }
            val token = accountEmail?.let { driveAuthSessionStore.tokenFor(it) }
            check(!token.isNullOrBlank()) {
                "Drive session expired. Disconnect and reconnect Google Drive, then choose a folder again."
            }
            val currentFolder = currentFolderState.value
            driveFoldersState.value = withTimeout(15_000) {
                withContext(Dispatchers.IO) {
                    listDriveFoldersUseCase(parentId = currentFolder.id, parentPath = currentFolder.path)
                }
            }
        } finally {
            folderLoadingState.value = false
        }
    }

    private fun resetFolderBrowser() {
        currentFolderState.value = FolderBrowserNode(id = ROOT_ID, path = ROOT_PATH)
        folderHistoryState.value = emptyList()
        driveFoldersState.value = emptyList()
    }
}
