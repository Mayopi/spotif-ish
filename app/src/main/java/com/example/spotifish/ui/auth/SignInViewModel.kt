package com.example.spotifish.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotifish.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInUiState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val signInProvider: GoogleSignInProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun signIn(activity: Activity) {
        if (_state.value.isWorking) return
        _state.value = SignInUiState(isWorking = true)
        viewModelScope.launch {
            runCatching {
                // Credential Manager surfaces the system account picker as part of
                // getCredential(); there is no separate "needs resolution" step like
                // the old AuthorizationClient flow had, so we don't need a launcher.
                val idToken = signInProvider.requestIdToken(activity)
                authRepository.signInWithGoogle(idToken)
            }.onSuccess {
                _state.value = SignInUiState(isWorking = false)
            }.onFailure { throwable ->
                _state.value = SignInUiState(
                    isWorking = false,
                    errorMessage = throwable.message ?: "Sign-in failed.",
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
