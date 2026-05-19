package com.example.spotifish.data.auth

import com.example.spotifish.data.network.SpotifishApi
import com.example.spotifish.data.network.dto.AuthTokenPair
import com.example.spotifish.data.network.dto.GoogleSignInRequest
import com.example.spotifish.data.network.dto.RefreshTokenRequest
import com.example.spotifish.data.network.dto.RefreshedTokenPair
import com.example.spotifish.data.network.dto.SignOutRequest
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the lifecycle of the user's authenticated session against the Spotifish
 * backend. This is the only class that ever calls the auth endpoints directly —
 * every other repository goes through the authenticated [SpotifishApi] which has
 * the JWT attached automatically by [com.example.spotifish.data.network.AuthInterceptor].
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

    suspend fun signInWithGoogle(idToken: String): Session {
        val response = api.get().signInWithGoogle(GoogleSignInRequest(idToken = idToken))
        return persistSignIn(response)
    }

    /**
     * Forces a token refresh. Called from
     * [com.example.spotifish.data.network.AuthInterceptor] synchronously when an
     * in-flight request comes back 401 — runs on the OkHttp dispatcher thread, which
     * is acceptable because the wait is bounded by the refresh endpoint's own
     * timeout.
     *
     * The backend's refresh response only carries new tokens (no `user`), so we
     * preserve the user info from the existing session and just swap the tokens.
     */
    suspend fun refresh(): Session? {
        val current = sessionStore.current() ?: return null
        return runCatching {
            val refreshed = api.get().refresh(RefreshTokenRequest(current.refreshToken))
            persistRefreshedTokens(current, refreshed)
        }.getOrElse {
            // Refresh failed irrecoverably — clear the session so the UI bounces the
            // user back to the sign-in screen.
            sessionStore.clear()
            null
        }
    }

    suspend fun signOut() {
        val current = sessionStore.current()
        if (current != null) {
            runCatching { api.get().signOut(SignOutRequest(refreshToken = current.refreshToken)) }
        }
        sessionStore.clear()
    }

    private fun persistSignIn(pair: AuthTokenPair): Session {
        val session = Session(
            accessToken = pair.accessToken,
            refreshToken = pair.refreshToken,
            // Backend doesn't return an expiry timestamp; the JWT TTL is fixed at
            // 15 minutes per the backend PRD. We use that as a hint only — the
            // 401-retry path in AuthInterceptor handles real expiry detection.
            accessTokenExpiresAtMillis = System.currentTimeMillis() + DEFAULT_TTL_MILLIS,
            userId = pair.user.id,
            email = pair.user.email,
            displayName = pair.user.displayName,
        )
        sessionStore.save(session)
        return session
    }

    private fun persistRefreshedTokens(
        previous: Session,
        refreshed: RefreshedTokenPair,
    ): Session {
        val session = previous.copy(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            accessTokenExpiresAtMillis = System.currentTimeMillis() + DEFAULT_TTL_MILLIS,
        )
        sessionStore.save(session)
        return session
    }

    private companion object {
        private const val DEFAULT_TTL_MILLIS = 15L * 60L * 1000L
    }
}
