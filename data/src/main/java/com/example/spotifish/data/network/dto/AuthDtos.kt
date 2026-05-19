package com.example.spotifish.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Body for `POST /v1/auth/google`. The backend's handler validates this as a Google
 * ID token via `google.golang.org/api/idtoken.Validate`, so the field name and type
 * MUST match exactly — see `internal/handler/auth_handler.go:23` in the backend repo.
 */
@Serializable
data class GoogleSignInRequest(
    val idToken: String,
)

/**
 * Successful response from `POST /v1/auth/google`.
 *
 * The backend does NOT return an `accessTokenExpiresAt` field. The client computes
 * its own expiry locally (defaulting to JWT_TTL_MILLIS in [com.example.spotifish.data.auth.AuthRepository]).
 */
@Serializable
data class AuthTokenPair(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String? = null,
    val googleSub: String? = null,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

/**
 * Successful response from `POST /v1/auth/refresh`. The backend's `model.TokenPair`
 * intentionally has NO `user` field — it just rotates the access + refresh tokens.
 * The client preserves the existing user info from its session and only swaps tokens.
 */
@Serializable
data class RefreshedTokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class SignOutRequest(
    val refreshToken: String,
)
