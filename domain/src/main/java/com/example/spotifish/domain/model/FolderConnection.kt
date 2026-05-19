package com.example.spotifish.domain.model

data class FolderConnection(
    val provider: String,
    val folderId: String,
    val folderName: String,
    val accountEmail: String? = null,
    val active: Boolean,
)
