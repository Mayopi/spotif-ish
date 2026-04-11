package com.example.musicapp.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInRequest(
    /**
     * Server-side OAuth authorization code obtained from Identity Services with
     * `requestServerAuthCode(serverClientId, forceCodeForRefreshToken = true)`.
     * The backend exchanges this for ID token + Drive access/refresh tokens in one
     * round trip, so we never send raw bearer tokens over the wire.
     */
    val serverAuthCode: String,
)

@Serializable
data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String, // RFC3339
    val user: AuthUser,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)
