package com.example.musicapp.domain.model

data class AppSettings(
    val selectedFolders: List<String> = emptyList(),
    val isDarkTheme: Boolean = false,
    val connectedDriveFolder: FolderConnection? = null,
)

