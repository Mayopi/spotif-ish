package com.example.spotifish.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.spotifish.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

class AuthFlowException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Asks Google to issue a **Google ID token** for the signed-in user.
 *
 * The Spotifish backend's `POST /v1/auth/google` handler expects an ID token (which
 * it verifies against Google's JWKS via `idtoken.Validate`), NOT a server-side OAuth
 * authorization code. We use the Credential Manager flow with `GetSignInWithGoogleOption`,
 * which surfaces the system account picker and returns a `GoogleIdTokenCredential`
 * once the user picks an account — no server-side OAuth code exchange needed.
 *
 * Drive scope is granted **separately** by the backend later (server-side OAuth on
 * its own callback URL); the client doesn't ask for it here, so the original
 * `requestOfflineAccess(...)` call is gone.
 */
class GoogleSignInProvider @Inject constructor() {

    suspend fun requestIdToken(activity: Activity): String {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw AuthFlowException(
                "Missing GOOGLE_WEB_CLIENT_ID. Add it to the project's .env file.",
            )
        }

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val result = try {
            credentialManager.getCredential(context = activity, request = request)
        } catch (e: GetCredentialException) {
            throw AuthFlowException(e.message ?: "Google sign-in was cancelled.", e)
        }

        val customCredential = result.credential as? CustomCredential
            ?: throw AuthFlowException("Google sign-in did not return a Google credential.")

        val googleCredential = GoogleIdTokenCredential.createFrom(customCredential.data)
        return googleCredential.idToken
    }
}
