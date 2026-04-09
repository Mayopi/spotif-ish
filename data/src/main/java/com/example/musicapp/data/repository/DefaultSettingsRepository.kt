package com.example.musicapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.musicapp.domain.model.AppSettings
import com.example.musicapp.domain.model.FolderConnection
import com.example.musicapp.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject

@Singleton
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("settings.preferences_pb") },
    )

    override fun observeSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences ->
                AppSettings(
                    selectedFolders = preferences[SELECTED_FOLDERS]
                        ?.split("|")
                        ?.filter { it.isNotBlank() }
                        .orEmpty(),
                    isDarkTheme = preferences[IS_DARK_THEME] == "true",
                    connectedDriveFolder = preferences[DRIVE_FOLDER]?.takeIf { it.isNotBlank() }?.let(::decodeFolder),
                )
            }
    }

    override suspend fun updateTheme(isDarkTheme: Boolean) {
        dataStore.edit { it[IS_DARK_THEME] = isDarkTheme.toString() }
    }

    override suspend fun updateSelectedFolders(folders: List<String>) {
        dataStore.edit { it[SELECTED_FOLDERS] = folders.joinToString("|") }
    }

    override suspend fun updateDriveFolder(connection: FolderConnection?) {
        dataStore.edit { prefs ->
            prefs[DRIVE_FOLDER] = connection?.let(::encodeFolder).orEmpty()
        }
    }

    private fun encodeFolder(connection: FolderConnection): String {
        return JSONObject()
            .put("provider", connection.provider)
            .put("folderId", connection.folderId)
            .put("folderName", connection.folderName)
            .put("active", connection.active)
            .toString()
    }

    private fun decodeFolder(raw: String): FolderConnection {
        val json = JSONObject(raw)
        return FolderConnection(
            provider = json.getString("provider"),
            folderId = json.getString("folderId"),
            folderName = json.getString("folderName"),
            active = json.getBoolean("active"),
        )
    }

    private companion object {
        val SELECTED_FOLDERS = stringPreferencesKey("selected_folders")
        val IS_DARK_THEME = stringPreferencesKey("is_dark_theme")
        val DRIVE_FOLDER = stringPreferencesKey("drive_folder")
    }
}

