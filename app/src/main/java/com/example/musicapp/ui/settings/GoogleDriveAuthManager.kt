package com.example.musicapp.ui.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.musicapp.BuildConfig
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
    ) : DriveConnectResult

    data class RequiresResolution(
        val pendingIntent: PendingIntent,
        val account: DriveAccountProfile,
    ) : DriveConnectResult
}

@ActivityRetainedScoped
class GoogleDriveAuthManager @Inject constructor() {

    suspend fun beginConnection(activity: Activity): DriveConnectResult {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        check(clientId.isNotBlank()) {
            "Missing GOOGLE_WEB_CLIENT_ID. Add it to the project's .env file."
        }

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
            DriveConnectResult.RequiresResolution(
                pendingIntent = authorizationResult.pendingIntent
                    ?: error("Authorization requires resolution but no PendingIntent was returned."),
                account = account,
            )
        } else {
            DriveConnectResult.Authorized(account)
        }
    }

    fun completeConnection(activity: Activity, resultData: Intent?) {
        Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(resultData)
    }

    suspend fun clearSession(activity: Activity) {
        CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
    }
}
