package com.example.musicapp.ui.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.musicapp.BuildConfig
import com.example.musicapp.debug.DriveDebugLogger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

data class DriveAccountProfile(
    val displayName: String,
    val email: String,
)

sealed interface DriveConnectResult {
    data class Authorized(
        val account: DriveAccountProfile,
        val accessToken: String?,
    ) : DriveConnectResult

    data class RequiresResolution(
        val pendingIntent: PendingIntent,
        val account: DriveAccountProfile,
    ) : DriveConnectResult
}

@ActivityRetainedScoped
class GoogleDriveAuthManager @Inject constructor(
    private val driveDebugLogger: DriveDebugLogger,
) {

    suspend fun beginConnection(activity: Activity): DriveConnectResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        check(clientId.isNotBlank()) {
            "Missing GOOGLE_WEB_CLIENT_ID. Add it to the project's .env file."
        }
        driveDebugLogger.log("begin_connection", "Starting Google credential flow")

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request,
        )

        val customCredential = result.credential as? CustomCredential
            ?: error("Google sign-in did not return a Google credential.")
        val googleCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
        val account = DriveAccountProfile(
            displayName = googleCredential.displayName?.takeIf { it.isNotBlank() }
                ?: googleCredential.id.substringBefore("@"),
            email = googleCredential.id,
        )
        driveDebugLogger.log("credential_result", "Received Google credential for ${account.email}")

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/drive.readonly"),
                ),
            )
            .build()

        val authorizationResult = Identity.getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .await()

        return if (authorizationResult.hasResolution()) {
            driveDebugLogger.log("authorization_resolution", "Authorization requires resolution for ${account.email}")
            DriveConnectResult.RequiresResolution(
                pendingIntent = authorizationResult.pendingIntent
                    ?: error("Authorization requires resolution but no PendingIntent was returned."),
                account = account,
            )
        } else {
            driveDebugLogger.log(
                "authorization_complete",
                "Authorization completed immediately for ${account.email}, tokenPresent=${!authorizationResult.accessToken.isNullOrBlank()}",
            )
            DriveConnectResult.Authorized(account, authorizationResult.accessToken)
        }
    }

    fun completeConnection(activity: Activity, resultData: Intent?): String? {
        driveDebugLogger.log("authorization_callback", "Handling Google authorization callback")
        val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(resultData)
        driveDebugLogger.log("authorization_callback_complete", "Callback resolved, tokenPresent=${!result.accessToken.isNullOrBlank()}")
        return result.accessToken
    }

    suspend fun clearSession(activity: Activity) {
        driveDebugLogger.log("clear_session", "Clearing Credential Manager session state")
        CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        driveDebugLogger.log("clear_session_complete", "Credential Manager session cleared")
    }
}
