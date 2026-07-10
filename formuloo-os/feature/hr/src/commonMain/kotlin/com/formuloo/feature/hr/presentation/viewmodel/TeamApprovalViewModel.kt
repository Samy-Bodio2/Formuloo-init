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

class TeamApprovalViewModel(private val repository: HrRepository) : ViewModel() {

    private val _pendingState = MutableStateFlow<UiState<List<LeaveRequest>>>(UiState.Loading)
    val pendingState: StateFlow<UiState<List<LeaveRequest>>> = _pendingState.asStateFlow()

    private val _conflictMessage = MutableStateFlow<String?>(null)
    val conflictMessage: StateFlow<String?> = _conflictMessage.asStateFlow()

    init {
        loadPending()
    }

    fun loadPending() {
        viewModelScope.launch {
            repository.getTeamPendingLeaves().collect { result ->
                _pendingState.value = when (result) {
                    is NetworkResult.Loading -> UiState.Loading
                    is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                    is NetworkResult.Error -> UiState.Error(result.message, result.code)
                }
            }
        }
    }

    fun approve(id: String) {
        viewModelScope.launch {
            handleResult(repository.approveLeave(id))
        }
    }

    fun reject(id: String, reason: String) {
        viewModelScope.launch {
            handleResult(repository.rejectLeave(id, reason))
        }
    }

    fun dismissConflictMessage() {
        _conflictMessage.value = null
    }

    private fun handleResult(result: NetworkResult<LeaveRequest>) {
        if (result is NetworkResult.Error && (result.code == 409 || result.code == 400)) {
            _conflictMessage.value = "Cette demande a déjà été traitée."
        }
        if (result is NetworkResult.Success || result is NetworkResult.Error) {
            loadPending()
        }
    }
}
