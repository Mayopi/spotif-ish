package com.example.musicapp.domain.model

data class FolderConnection(
    val provider: String,
    val folderId: String,
    val folderName: String,
    val active: Boolean,
)

