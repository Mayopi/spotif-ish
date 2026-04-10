package com.example.musicapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.musicapp.core.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PersistedDriveToken(
    val accountEmail: String,
    val accessToken: String,
    val acquiredAtMillis: Long,
)

/**
 * Persistent store for the OAuth2 access token granted by Google Identity Services.
 *
 * The Drive sync layer used to keep the token only in memory (`DriveAuthSessionStore`),
 * which made the connection appear to "expire" on every cold start: after process death
 * the token was gone, and the legacy `GoogleAuthUtil.getToken` fallback could not
 * recover it because the user signed in via Credential Manager (no `com.google` entry
 * in the system AccountManager). Persisting the token here lets the in-memory cache
 * be rehydrated on launch, and the silent re-auth flow in
 * [com.example.musicapp.data.drive.GoogleDriveMusicDataSource] handles refresh once
 * the cached token actually expires.
 *
 * Tokens are stored in `EncryptedSharedPreferences` so they're encrypted at rest with
 * a key from the Android Keystore. If keystore initialization fails (e.g. corrupted
 * after a backup/restore), the store falls back to plain `SharedPreferences` so the
 * sync subsystem still functions and the user can simply reconnect.
 */
@Singleton
class DriveTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatchersProvider: DispatchersProvider,
) {

    private val mutex = Mutex()

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
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun load(): PersistedDriveToken? = mutex.withLock {
        withContext(dispatchersProvider.io) {
            val email = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }
            val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
            val acquiredAt = prefs.getLong(KEY_ACQUIRED_AT, 0L)
            if (email != null && token != null) {
                PersistedDriveToken(email, token, acquiredAt)
            } else {
                null
            }
        }
    }

    suspend fun save(accountEmail: String, accessToken: String) {
        mutex.withLock {
            withContext(dispatchersProvider.io) {
                prefs.edit().apply {
                    putString(KEY_EMAIL, accountEmail)
                    putString(KEY_TOKEN, accessToken)
                    putLong(KEY_ACQUIRED_AT, System.currentTimeMillis())
                    apply()
                }
            }
        }
    }

    suspend fun clear() {
        mutex.withLock {
            withContext(dispatchersProvider.io) {
                prefs.edit().clear().apply()
            }
        }
    }

    private companion object {
        private const val ENCRYPTED_PREFS_NAME = "drive_tokens.encrypted"
        private const val FALLBACK_PREFS_NAME = "drive_tokens.fallback"
        private const val KEY_EMAIL = "account_email"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_ACQUIRED_AT = "acquired_at_millis"
    }
}
