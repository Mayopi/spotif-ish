package com.example.musicapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapp.domain.usecase.RefreshLocalLibraryUseCase
import com.example.musicapp.domain.usecase.RefreshLibrariesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val refreshLibrariesUseCase: RefreshLibrariesUseCase,
    private val refreshLocalLibraryUseCase: RefreshLocalLibraryUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            refreshLibrariesUseCase()
        }
    }

    fun refreshLocalLibrary() {
        viewModelScope.launch {
            refreshLocalLibraryUseCase()
        }
    }
}
