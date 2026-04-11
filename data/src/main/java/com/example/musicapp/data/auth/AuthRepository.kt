package com.example.musicapp.data.auth

import com.example.musicapp.data.network.SpotifishApi
import com.example.musicapp.data.network.dto.AuthTokenPair
import com.example.musicapp.data.network.dto.GoogleSignInRequest
import com.example.musicapp.data.network.dto.RefreshTokenRequest
import dagger.Lazy
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the lifecycle of the user's authenticated session against the Spotifish
 * backend. This is the only class that ever calls the auth endpoints directly — every
 * other repository goes through the authenticated [SpotifishApi] which has the JWT
 * attached automatically by [com.example.musicapp.data.network.AuthInterceptor].
 *
 * The [SpotifishApi] is injected lazily because the OkHttp client that backs it
 * depends on this class via the interceptor (for refresh-on-401), creating a
 * circular dependency that Hilt resolves with `Lazy<T>`.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: Lazy<SpotifishApi>,
) {

    val session: StateFlow<Session?> = sessionStore.session

    fun current(): Session? = sessionStore.current()

    suspend fun signInWithGoogle(serverAuthCode: String): Session {
        val response = api.get().signInWithGoogle(GoogleSignInRequest(serverAuthCode))
        return persist(response)
    }

    /**
     * Forces a token refresh. Called from [com.example.musicapp.data.network.AuthInterceptor]
     * synchronously when an in-flight request comes back 401 — uses [runBlocking] on
     * the OkHttp dispatcher thread, which is acceptable because the wait is bounded
     * by the refresh endpoint's own timeout.
     */
    suspend fun refresh(): Session? {
        val current = sessionStore.current() ?: return null
        return runCatching {
            val response = api.get().refresh(RefreshTokenRequest(current.refreshToken))
            persist(response)
        }.getOrElse {
            // Refresh failed irrecoverably — clear the session so the UI bounces the
            // user back to the sign-in screen.
            sessionStore.clear()
            null
        }
    }

    suspend fun signOut() {
        runCatching { api.get().signOut() }
        sessionStore.clear()
    }

    private fun persist(pair: AuthTokenPair): Session {
        val expiresAt = runCatching {
            Instant.parse(pair.accessTokenExpiresAt).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis() + DEFAULT_TTL_MILLIS)

        val session = Session(
            accessToken = pair.accessToken,
            refreshToken = pair.refreshToken,
            accessTokenExpiresAtMillis = expiresAt,
            userId = pair.user.id,
            email = pair.user.email,
            displayName = pair.user.displayName,
        )
        sessionStore.save(session)
        return session
    }

    private companion object {
        private const val DEFAULT_TTL_MILLIS = 15L * 60L * 1000L
    }
}
