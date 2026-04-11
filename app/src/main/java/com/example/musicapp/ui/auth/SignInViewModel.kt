package com.example.musicapp.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInUiState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface SignInEvent {
    data class LaunchResolution(val pendingIntent: android.app.PendingIntent) : SignInEvent
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authCodeProvider: GoogleAuthCodeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SignInEvent>()
    val events = _events.asSharedFlow()

    private var pendingActivity: Activity? = null

    fun signIn(activity: Activity) {
        if (_state.value.isWorking) return
        _state.value = SignInUiState(isWorking = true)
        viewModelScope.launch {
            runCatching {
                val code = authCodeProvider.requestServerAuthCode(activity)
                authRepository.signInWithGoogle(code)
            }.onSuccess {
                _state.value = SignInUiState(isWorking = false)
            }.onFailure { throwable ->
                if (throwable is NeedsResolutionException) {
                    pendingActivity = activity
                    _events.emit(SignInEvent.LaunchResolution(throwable.pendingIntent))
                    // Stay in the working state until completeResolution returns.
                } else {
                    _state.value = SignInUiState(
                        isWorking = false,
                        errorMessage = throwable.message ?: "Sign-in failed.",
                    )
                }
            }
        }
    }

    fun completeResolution(data: Intent?) {
        val activity = pendingActivity ?: return
        pendingActivity = null
        viewModelScope.launch {
            runCatching {
                val code = authCodeProvider.completeFromIntent(activity, data)
                authRepository.signInWithGoogle(code)
            }.onSuccess {
                _state.value = SignInUiState(isWorking = false)
            }.onFailure { throwable ->
                _state.value = SignInUiState(
                    isWorking = false,
                    errorMessage = throwable.message ?: "Sign-in cancelled.",
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
