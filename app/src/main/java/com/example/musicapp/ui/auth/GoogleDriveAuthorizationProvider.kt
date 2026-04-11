package com.example.musicapp.ui.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.example.musicapp.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of attempting to obtain a server-side OAuth authorization code with the
 * Drive `drive.readonly` scope.
 */
sealed interface DriveAuthorizationResult {
    /** User has already granted consent — auth code is ready to be posted to the backend. */
    data class Authorized(val serverAuthCode: String) : DriveAuthorizationResult

    /** User must grant consent first; UI should launch the [pendingIntent] via an IntentSender. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthorizationResult
}

/**
 * Asks Google Identity Services for a **server-side OAuth authorization code** that
 * the backend (`POST /v1/drive/connect`) can exchange for a Drive access + refresh
 * token via `oauth2.Config.Exchange`.
 *
 * This is intentionally a SEPARATE flow from [GoogleSignInProvider]:
 *
 *   - Sign-in (`/v1/auth/google`)  -> Credential Manager -> Google **ID token**
 *   - Drive grant (`/v1/drive/connect`) -> AuthorizationClient -> server **auth code**
 *
 * The two grants use the same web client id (`BuildConfig.GOOGLE_WEB_CLIENT_ID`),
 * which MUST also be the client id configured on the backend so its OAuth config can
 * exchange the code. The redirect URI baked into that client must match the
 * backend's `GOOGLE_REDIRECT_URI` env var, otherwise the exchange returns
 * `redirect_uri_mismatch`.
 *
 * `requestOfflineAccess(serverClientId, forceCodeForRefreshToken = true)` ensures the
 * backend always receives a refresh token, even on subsequent re-grants.
 */
class GoogleDriveAuthorizationProvider @Inject constructor() {

    suspend fun requestServerAuthCode(activity: Activity): DriveAuthorizationResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw AuthFlowException(
                "Missing GOOGLE_WEB_CLIENT_ID. Add it to the project's .env file.",
            )
        }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(Scope("https://www.googleapis.com/auth/drive.readonly")),
            )
            .requestOfflineAccess(clientId, /* forceCodeForRefreshToken = */ true)
            .build()

        val result = withContext(Dispatchers.IO) {
            Tasks.await(Identity.getAuthorizationClient(activity).authorize(request))
        }

        if (result.hasResolution()) {
            return DriveAuthorizationResult.NeedsConsent(
                pendingIntent = result.pendingIntent
                    ?: throw AuthFlowException(
                        "Drive authorization needs consent but no PendingIntent was returned.",
                    ),
            )
        }

        val code = result.serverAuthCode
            ?: throw AuthFlowException(
                "Drive authorization succeeded but no server auth code was returned. " +
                    "Check that the OAuth client id is a Web client and offline access is enabled.",
            )
        return DriveAuthorizationResult.Authorized(code)
    }

    suspend fun completeFromIntent(activity: Activity, data: Intent?): String {
        val result = withContext(Dispatchers.IO) {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        }
        return result.serverAuthCode
            ?: throw AuthFlowException(
                "Drive consent screen returned without a server auth code.",
            )
    }
}
