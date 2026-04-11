package com.example.musicapp.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.data.auth.AuthRepository
import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.data.network.dto.ConnectDriveRequest
import com.example.musicapp.data.network.dto.SetDriveFolderRequest
import com.example.musicapp.data.network.dto.toDomain
import com.example.musicapp.domain.model.DriveFolder
import com.example.musicapp.domain.model.FolderConnection
import com.example.musicapp.domain.usecase.EnqueueDriveLibraryRefreshUseCase
import com.example.musicapp.domain.usecase.ObserveDriveSyncStateUseCase
import com.example.musicapp.domain.usecase.ObserveSettingsUseCase
import com.example.musicapp.domain.usecase.PauseDriveLibraryRefreshUseCase
import com.example.musicapp.domain.usecase.ResumeDriveLibraryRefreshUseCase
import com.example.musicapp.domain.usecase.UpdateDriveFolderUseCase
import com.example.musicapp.ui.auth.DriveAuthorizationResult
import com.example.musicapp.ui.auth.GoogleDriveAuthorizationProvider
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
    val isFolderLoading: Boolean = false,
    val isDriveSyncing: Boolean = false,
    val isDrivePaused: Boolean = false,
    val driveSyncStatusText: String = "Idle",
    val availableDriveFolders: List<DriveFolder> = emptyList(),
    val isFolderPickerVisible: Boolean = false,
    val currentDriveFolderPath: String = "My Drive",
    val canNavigateUpFolders: Boolean = false,
)

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
    object SignOut : SettingsEvent

    /** Drive consent screen needs to be shown via an IntentSender. */
    data class LaunchDriveConsent(val pendingIntent: android.app.PendingIntent) : SettingsEvent
}

/**
 * Settings now talks exclusively to the backend.
 *
 * The on-device Google Drive auth flow (`GoogleDriveAuthManager`,
 * `DriveAuthSessionStore`, `DriveTokenStore`) has been removed — Drive credentials
 * live on the server, and the only "connection" the client cares about is whether
 * the signed-in user has a folder selected on the backend (`GET /v1/me` would tell
 * us via the included drive folder field; here we infer it from the Settings
 * repository which mirrors the backend response).
 *
 * Disconnect calls `DELETE /v1/drive/connection`, the folder picker calls
 * `GET /v1/drive/folders`, and selecting a folder calls `POST /v1/drive/connection`
 * followed by an enqueued sync.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val observeDriveSyncStateUseCase: ObserveDriveSyncStateUseCase,
    private val enqueueDriveLibraryRefreshUseCase: EnqueueDriveLibraryRefreshUseCase,
    private val pauseDriveLibraryRefreshUseCase: PauseDriveLibraryRefreshUseCase,
    private val resumeDriveLibraryRefreshUseCase: ResumeDriveLibraryRefreshUseCase,
    private val updateDriveFolderUseCase: UpdateDriveFolderUseCase,
    private val api: SpotifishApi,
    private val authRepository: AuthRepository,
    private val driveAuthProvider: GoogleDriveAuthorizationProvider,
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

    private val baseState: StateFlow<SettingsUiState> = combine(
        observeSettingsUseCase(),
        authRepository.session,
    ) { settings, session ->
        SettingsUiState(
            isDriveConnected = settings.connectedDriveFolder?.active == true,
            connectedDriveName = settings.connectedDriveFolder?.folderName ?: "Google Drive",
            connectedDriveFolderName = settings.connectedDriveFolder?.folderName ?: "Not connected",
            connectedDriveEmail = session?.email.orEmpty(),
            selectedFolderCount = if (settings.connectedDriveFolder != null) 1 else 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val syncAwareState = combine(
        baseState,
        observeDriveSyncStateUseCase(),
    ) { ui, syncState ->
        ui.copy(
            isDriveSyncing = syncState.isSyncing,
            isDrivePaused = syncState.isPaused,
            driveSyncStatusText = when {
                syncState.isPaused ->
                    "Paused at ${syncState.processedFileCount}" +
                        (syncState.lastSyncedSongCount.takeIf { it > 0 }?.let { " of $it" } ?: "")
                syncState.isSyncing -> "${syncState.processedFileCount} files processed"
                !syncState.lastError.isNullOrBlank() -> syncState.lastError.orEmpty()
                syncState.lastSyncedSongCount > 0 -> "${syncState.lastSyncedSongCount} tracks synced"
                else -> "Ready to sync"
            },
        )
    }

    val screenState: StateFlow<SettingsUiState> = combine(
        syncAwareState,
        workingState,
        folderLoadingState,
        driveFoldersState,
        folderPickerVisibleState,
        currentFolderState,
        folderHistoryState,
    ) { values ->
        val ui = values[0] as SettingsUiState
        val working = values[1] as Boolean
        val folderLoading = values[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val folders = values[3] as List<DriveFolder>
        val pickerVisible = values[4] as Boolean
        val currentFolder = values[5] as FolderBrowserNode
        @Suppress("UNCHECKED_CAST")
        val history = values[6] as List<FolderBrowserNode>
        ui.copy(
            isWorking = working,
            isFolderLoading = folderLoading,
            availableDriveFolders = folders,
            isFolderPickerVisible = pickerVisible,
            currentDriveFolderPath = currentFolder.path,
            canNavigateUpFolders = history.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshLibraries() {
        viewModelScope.launch { enqueueDriveLibraryRefreshUseCase() }
    }

    fun pauseSync() {
        viewModelScope.launch { pauseDriveLibraryRefreshUseCase() }
    }

    fun resumeSync() {
        viewModelScope.launch { resumeDriveLibraryRefreshUseCase() }
    }

    fun signOut() {
        viewModelScope.launch {
            workingState.value = true
            runCatching { authRepository.signOut() }
            workingState.value = false
            _events.emit(SettingsEvent.SignOut)
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch {
            workingState.value = true
            runCatching {
                api.disconnectDrive()
                updateDriveFolderUseCase(null)
                resetFolderBrowser()
                _events.emit(SettingsEvent.Message("Drive disconnected from your account."))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not disconnect Drive."))
            }
            workingState.value = false
        }
    }

    /**
     * Kicks off the Drive grant flow. Asks Identity Services for a server-side OAuth
     * authorization code with the `drive.readonly` scope. If consent is already
     * granted, the code is posted to `POST /v1/drive/connect` immediately. Otherwise
     * the UI is asked to launch the consent intent, and [completeDriveConnect]
     * resumes the flow.
     */
    fun connectDrive(activity: Activity) {
        viewModelScope.launch {
            if (workingState.value) return@launch
            workingState.value = true
            runCatching {
                when (val result = driveAuthProvider.requestServerAuthCode(activity)) {
                    is DriveAuthorizationResult.Authorized -> postAuthCodeToBackend(result.serverAuthCode)
                    is DriveAuthorizationResult.NeedsConsent -> {
                        _events.emit(SettingsEvent.LaunchDriveConsent(result.pendingIntent))
                    }
                }
            }.onFailure { throwable ->
                workingState.value = false
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not start Drive connect."))
            }
        }
    }

    /**
     * Continuation of [connectDrive] after the user has granted consent through the
     * IntentSender. Reads the server auth code out of the returned intent and posts
     * it to the backend.
     */
    fun completeDriveConnect(activity: Activity, data: Intent?) {
        viewModelScope.launch {
            runCatching {
                val code = driveAuthProvider.completeFromIntent(activity, data)
                postAuthCodeToBackend(code)
            }.onFailure { throwable ->
                workingState.value = false
                _events.emit(SettingsEvent.Message(throwable.message ?: "Drive consent was cancelled."))
            }
        }
    }

    private suspend fun postAuthCodeToBackend(authCode: String) {
        api.connectDrive(ConnectDriveRequest(authCode = authCode))
        workingState.value = false
        _events.emit(
            SettingsEvent.Message("Google Drive connected. Choose a folder to import music."),
        )
        // Drive credentials are now stored on the backend, but no folder has been
        // selected yet — `isDriveConnected` (which is folder-based) is still false,
        // so the "Browse" button stays disabled. Auto-open the folder picker so the
        // user can immediately choose a folder without hitting a dead-end.
        openFolderPicker()
    }

    fun openFolderPicker() {
        viewModelScope.launch {
            runCatching {
                resetFolderBrowser()
                folderPickerVisibleState.value = true
                loadCurrentFolder()
            }.onFailure { throwable ->
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
            runCatching {
                loadCurrentFolder()
            }.onFailure { throwable ->
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
            runCatching {
                loadCurrentFolder()
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not open parent folder."))
            }
        }
    }

    fun selectCurrentDriveFolder() {
        viewModelScope.launch {
            val currentFolder = currentFolderState.value
            folderLoadingState.value = true
            runCatching {
                api.setDriveConnection(
                    SetDriveFolderRequest(
                        folderId = currentFolder.id,
                        folderName = currentFolder.path,
                    ),
                )
                // Mirror the connection into the local Settings store so the UI
                // immediately reflects the new selection without waiting on a
                // round-trip to /v1/me/settings.
                updateDriveFolderUseCase(
                    FolderConnection(
                        provider = "google_drive",
                        folderId = currentFolder.id,
                        folderName = currentFolder.path,
                        accountEmail = baseState.value.connectedDriveEmail.ifBlank { null },
                        active = true,
                    ),
                )
                folderPickerVisibleState.value = false
                enqueueDriveLibraryRefreshUseCase()
                _events.emit(SettingsEvent.Message("Selected ${currentFolder.path}. Sync started."))
            }.onFailure { throwable ->
                _events.emit(SettingsEvent.Message(throwable.message ?: "Could not select folder."))
            }
            folderLoadingState.value = false
        }
    }

    private suspend fun loadCurrentFolder() {
        folderLoadingState.value = true
        try {
            val currentFolder = currentFolderState.value
            driveFoldersState.value = api.listDriveFolders(parentId = currentFolder.id.takeIf { it != ROOT_ID })
                .folders
                .map { it.toDomain(parentPath = currentFolder.path) }
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
