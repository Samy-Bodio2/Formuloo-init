package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.LeaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyLeavesViewModel(private val repository: HrRepository) : ViewModel() {

    private val _leavesState = MutableStateFlow<UiState<List<LeaveRequest>>>(UiState.Loading)
    val leavesState: StateFlow<UiState<List<LeaveRequest>>> = _leavesState.asStateFlow()

    init {
        loadLeaves()
    }

    fun loadLeaves() {
        viewModelScope.launch {
            repository.getMyLeaves().collect { result ->
                _leavesState.value = when (result) {
                    is NetworkResult.Loading -> UiState.Loading
                    is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                    is NetworkResult.Error -> UiState.Error(result.message, result.code)
                }
            }
        }
    }

    fun cancelLeave(id: String) {
        viewModelScope.launch {
            val result = repository.cancelLeave(id)
            if (result is NetworkResult.Success || result is NetworkResult.Error) {
                loadLeaves()
            }
        }
    }
}
