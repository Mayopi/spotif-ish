package com.example.musicapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.ObserveSettingsUseCase
import com.example.musicapp.domain.usecase.RefreshLibrariesUseCase
import com.example.musicapp.domain.usecase.UpdateThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isDarkTheme: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = observeSettingsUseCase()
        .map { MainUiState(isDarkTheme = it.isDarkTheme) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            refreshLibrariesUseCase()
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            updateThemeUseCase(!uiState.value.isDarkTheme)
        }
    }
}

