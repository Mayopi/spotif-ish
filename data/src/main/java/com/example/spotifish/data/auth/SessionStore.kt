package com.example.spotifish.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtMillis: Long,
    val userId: String,
    val email: String,
    val displayName: String?,
)

/**
 * Persistent encrypted store for the backend-issued JWT pair.
 *
 * Tokens are encrypted at rest by `EncryptedSharedPreferences` (the same dependency
 * we already use for `DriveTokenStore`). All read/write goes through the in-memory
 * [StateFlow] mirror so the UI can observe sign-in state reactively.
 *
 * On startup we hydrate the `StateFlow` from disk so the auth-aware navigation in
 * `MainActivity` knows whether to show the sign-in screen or jump straight into the
 * app.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ) as SharedPreferences
    }.getOrElse {
        // If the keystore is in a bad state (e.g. after a backup/restore), fall back
        // to plain prefs so the app still launches. The user will be prompted to
        // re-sign-in on the next launch.
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _session = MutableStateFlow(loadFromDisk())
    val session: StateFlow<Session?> = _session.asStateFlow()

    fun current(): Session? = _session.value

    fun save(session: Session) {
        prefs.edit().apply {
            putString(KEY_ACCESS, session.accessToken)
            putString(KEY_REFRESH, session.refreshToken)
            putLong(KEY_EXPIRES, session.accessTokenExpiresAtMillis)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
            putString(KEY_DISPLAY_NAME, session.displayName)
            apply()
        }
        _session.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun loadFromDisk(): Session? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val expires = prefs.getLong(KEY_EXPIRES, 0L)
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        return Session(
            accessToken = access,
            refreshToken = refresh,
            accessTokenExpiresAtMillis = expires,
            userId = userId,
            email = email,
            displayName = displayName,
        )
    }

    private companion object {
        private const val ENCRYPTED_PREFS_NAME = "spotifish_session.encrypted"
        private const val FALLBACK_PREFS_NAME = "spotifish_session.fallback"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES = "access_expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
