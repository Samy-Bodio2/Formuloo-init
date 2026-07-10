package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.Payslip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmployeeDetailViewModel(
    private val repository: HrRepository,
    private val employeeId: String,
) : ViewModel() {

    private val _employeeState = MutableStateFlow<UiState<Employee>>(UiState.Loading)
    val employeeState: StateFlow<UiState<Employee>> = _employeeState.asStateFlow()

    private val _contractsState = MutableStateFlow<UiState<List<Contract>>>(UiState.Loading)
    val contractsState: StateFlow<UiState<List<Contract>>> = _contractsState.asStateFlow()

    private val _balancesState = MutableStateFlow<UiState<List<LeaveBalance>>>(UiState.Loading)
    val balancesState: StateFlow<UiState<List<LeaveBalance>>> = _balancesState.asStateFlow()

    private val _leavesState = MutableStateFlow<UiState<List<LeaveRequest>>>(UiState.Loading)
    val leavesState: StateFlow<UiState<List<LeaveRequest>>> = _leavesState.asStateFlow()

    private val _payslipsState = MutableStateFlow<UiState<List<Payslip>>>(UiState.Loading)
    val payslipsState: StateFlow<UiState<List<Payslip>>> = _payslipsState.asStateFlow()

    private val _archiveState = MutableStateFlow<Boolean>(false)
    val archiveState: StateFlow<Boolean> = _archiveState.asStateFlow()

    private val _archiveError = MutableStateFlow<String?>(null)
    val archiveError: StateFlow<String?> = _archiveError.asStateFlow()

    init {
        loadEmployee(employeeId)
        loadContracts(employeeId)
        loadLeaveBalances(employeeId)
        loadLeaves(employeeId)
        loadPayslips(employeeId)
    }

    fun loadEmployee(id: String) {
        viewModelScope.launch {
            _employeeState.value = UiState.Loading
            _employeeState.value = when (val result = repository.getEmployee(id)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    fun loadContracts(employeeId: String) {
        viewModelScope.launch {
            repository.getContracts(employeeId).collect { result ->
                _contractsState.value = when (result) {
                    is NetworkResult.Loading -> UiState.Loading
                    is NetworkResult.Success -> {
                        if (result.data.isEmpty()) UiState.Empty
                        else UiState.Success(result.data)
                    }
                    is NetworkResult.Error -> UiState.Error(result.message, result.code)
                }
            }
        }
    }

    private fun loadLeaveBalances(employeeId: String) {
        viewModelScope.launch {
            _balancesState.value = UiState.Loading
            _balancesState.value = when (val result = repository.getLeaveBalance(employeeId)) {
                is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    private fun loadPayslips(employeeId: String) {
        viewModelScope.launch {
            _payslipsState.value = UiState.Loading
            _payslipsState.value = when (val result = repository.getPayslipsForEmployee(employeeId)) {
                is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    private fun loadLeaves(employeeId: String) {
        viewModelScope.launch {
            _leavesState.value = UiState.Loading
            _leavesState.value = when (val result = repository.getPendingLeaves()) {
                is NetworkResult.Success -> {
                    val filtered = result.data.filter { it.employeeId == employeeId }
                    if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
                }
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    fun archiveEmployee(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _archiveError.value = null
            when (val result = repository.archiveEmployee(employeeId)) {
                is NetworkResult.Success -> onSuccess()
                is NetworkResult.Error -> _archiveError.value = result.message
                else -> Unit
            }
        }
    }

    fun clearArchiveError() {
        _archiveError.value = null
    }
}
