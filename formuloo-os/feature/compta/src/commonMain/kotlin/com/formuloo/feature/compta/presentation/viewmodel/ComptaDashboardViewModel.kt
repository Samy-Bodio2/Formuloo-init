package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.DashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComptaDashboardViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _statsState = MutableStateFlow<UiState<DashboardStats>>(UiState.Loading)
    val statsState: StateFlow<UiState<DashboardStats>> = _statsState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _statsState.value = UiState.Loading
            _statsState.value = when (val result = repository.getStats()) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
