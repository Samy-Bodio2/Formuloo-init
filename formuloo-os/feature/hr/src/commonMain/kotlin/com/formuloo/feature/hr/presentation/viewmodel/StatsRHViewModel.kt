package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.StatsRH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsRHViewModel(private val repository: HrRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StatsRH>>(UiState.Loading)
    val state: StateFlow<UiState<StatsRH>> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repository.getStatsRH()) {
                is NetworkResult.Success -> UiState.Success(r.data)
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                else -> UiState.Loading
            }
        }
    }
}
