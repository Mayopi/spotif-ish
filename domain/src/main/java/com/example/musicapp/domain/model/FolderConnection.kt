package com.example.musicapp.domain.model

data class FolderConnection(
    val provider: String,
    val folderId: String,
    val folderName: String,
    val accountEmail: String? = null,
    val active: Boolean,
)
