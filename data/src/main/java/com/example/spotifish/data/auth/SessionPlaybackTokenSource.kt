package com.example.spotifish.data.auth

import com.example.spotifish.core.PlaybackTokenSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter that exposes the in-memory access token from [SessionStore] through the
 * `core`-level [PlaybackTokenSource] contract so the player module can read it
 * without depending on the data layer.
 */
@Singleton
class SessionPlaybackTokenSource @Inject constructor(
    private val sessionStore: SessionStore,
) : PlaybackTokenSource {
    override fun currentAccessToken(): String? = sessionStore.current()?.accessToken
}
