package com.example.spotifish.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spotifish.data.auth.AuthRepository
import com.example.spotifish.ui.auth.SignInScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Tiny ViewModel that hands the [AuthRepository.session] StateFlow to Compose so the
 * `AuthGate` composable can swap between the sign-in screen and the main app
 * reactively. Lives in the app module because the data module doesn't depend on
 * Compose / lifecycle.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepository: AuthRepository,
) : androidx.lifecycle.ViewModel() {
    val session: StateFlow<com.example.spotifish.data.auth.Session?> = authRepository.session
}

/**
 * Top-level navigation gate. Renders the sign-in screen until the user has a valid
 * backend session, then mounts [SpotifishApp]. Sign-out anywhere in the app clears the
 * SessionStore, which causes this composable to recompose into the sign-in screen
 * automatically.
 */
@Composable
fun AuthGate(viewModel: AuthGateViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    AppTheme {
        if (session == null) {
            SignInScreen()
        } else {
            SpotifishApp()
        }
    }
}
