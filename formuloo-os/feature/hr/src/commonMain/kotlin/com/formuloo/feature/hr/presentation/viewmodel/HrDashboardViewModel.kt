package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.domain.model.LeaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HrStats(
    val totalEmployees: Int = 0,
    val activeEmployees: Int = 0,
    val onLeaveEmployees: Int = 0,
    val pendingLeavesCount: Int = 0,
)

class HrDashboardViewModel(private val repository: HrRepository) : ViewModel() {

    private val _pendingLeaves = MutableStateFlow<UiState<List<LeaveRequest>>>(UiState.Loading)
    val pendingLeaves: StateFlow<UiState<List<LeaveRequest>>> = _pendingLeaves.asStateFlow()

    private val _hrStats = MutableStateFlow(HrStats())
    val hrStats: StateFlow<HrStats> = _hrStats.asStateFlow()

    init {
        loadPendingLeaves()
        loadEmployeeStats()
    }

    fun loadPendingLeaves() {
        viewModelScope.launch {
            _pendingLeaves.value = UiState.Loading
            _pendingLeaves.value = when (val result = repository.getPendingLeaves()) {
                is NetworkResult.Success -> {
                    val list = result.data
                    _hrStats.value = _hrStats.value.copy(pendingLeavesCount = list.size)
                    if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                }
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    private fun loadEmployeeStats() {
        viewModelScope.launch {
            repository.getEmployees().collect { result ->
                if (result is NetworkResult.Success) {
                    val employees = result.data
                    _hrStats.value = _hrStats.value.copy(
                        totalEmployees = employees.size,
                        activeEmployees = employees.count { it.status == EmployeeStatus.ACTIVE },
                        onLeaveEmployees = employees.count { it.status == EmployeeStatus.ON_LEAVE },
                    )
                }
            }
        }
    }

    fun approveLeave(id: String, comment: String? = null) {
        viewModelScope.launch {
            val result = repository.approveLeave(id, comment)
            if (result is NetworkResult.Success || result is NetworkResult.Error) {
                loadPendingLeaves()
            }
        }
    }

    fun rejectLeave(id: String, reason: String) {
        viewModelScope.launch {
            val result = repository.rejectLeave(id, reason)
            if (result is NetworkResult.Success || result is NetworkResult.Error) {
                loadPendingLeaves()
            }
        }
    }
}
