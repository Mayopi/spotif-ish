package com.example.musicapp.ui.auth

import android.app.Activity
import com.example.musicapp.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthFlowException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Asks Google Identity Services for a **server-side OAuth authorization code** that
 * the backend can exchange for both an ID token (to identify the user) and a Drive
 * refresh token (to scan the user's library on the user's behalf).
 *
 * The Drive scope is requested **at sign-in time** so the backend gets the refresh
 * token in the same handshake — no separate "Connect Drive" UI flow needed
 * post-sign-in.
 *
 * Requires `GOOGLE_WEB_CLIENT_ID` to be set in the project's `.env` file (it's
 * already wired into BuildConfig). The same OAuth client must also be configured on
 * the backend so it can exchange the auth code at `/v1/auth/google`.
 */
class GoogleAuthCodeProvider @Inject constructor() {

    suspend fun requestServerAuthCode(activity: Activity): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw AuthFlowException(
                "Missing GOOGLE_WEB_CLIENT_ID. Add it to the project's .env file.",
            )
        }

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("openid"),
                    Scope("email"),
                    Scope("profile"),
                    Scope("https://www.googleapis.com/auth/drive.readonly"),
                ),
            )
            // Asks Google for a one-shot auth code that the backend will exchange.
            // forceCodeForRefreshToken = true ensures the backend always gets a
            // refresh token, even on subsequent sign-ins.
            .requestOfflineAccess(clientId, /* forceCodeForRefreshToken = */ true)
            .build()

        val result = withContext(Dispatchers.IO) {
            Tasks.await(Identity.getAuthorizationClient(activity).authorize(request))
        }

        if (result.hasResolution()) {
            // We need to launch the consent UI. The activity is required to show
            // the resolution intent — UI layer is responsible for catching this and
            // launching the IntentSender.
            throw NeedsResolutionException(
                pendingIntent = result.pendingIntent
                    ?: throw AuthFlowException("Authorization needs resolution but no PendingIntent was returned."),
            )
        }

        return result.serverAuthCode
            ?: throw AuthFlowException("Google did not return a server auth code. Is offline access configured?")
    }

    suspend fun completeFromIntent(activity: Activity, data: android.content.Intent?): String {
        val result = withContext(Dispatchers.IO) {
            Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        }
        return result.serverAuthCode
            ?: throw AuthFlowException("Google did not return a server auth code after consent.")
    }
}

class NeedsResolutionException(
    val pendingIntent: android.app.PendingIntent,
) : Exception("Needs user consent")
