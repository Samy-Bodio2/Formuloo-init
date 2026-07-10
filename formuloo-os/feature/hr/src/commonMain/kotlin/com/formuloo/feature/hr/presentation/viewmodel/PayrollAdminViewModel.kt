package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayrollRunResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PayrollRunState(
    val isLoading: Boolean = false,
    val result: PayrollRunResult? = null,
    val error: String? = null,
)

class PayrollAdminViewModel(private val repository: HrRepository) : ViewModel() {

    // Both targets are JVM (Android + Desktop), so java.util.Calendar is available in commonMain.
    private val now = java.util.Calendar.getInstance()

    private val _mois = MutableStateFlow(now.get(java.util.Calendar.MONTH) + 1)
    val mois: StateFlow<Int> = _mois.asStateFlow()

    private val _annee = MutableStateFlow(now.get(java.util.Calendar.YEAR))
    val annee: StateFlow<Int> = _annee.asStateFlow()

    private val _periodeState = MutableStateFlow<UiState<List<Payslip>>>(UiState.Loading)
    val periodeState: StateFlow<UiState<List<Payslip>>> = _periodeState.asStateFlow()

    private val _runState = MutableStateFlow(PayrollRunState())
    val runState: StateFlow<PayrollRunState> = _runState.asStateFlow()

    init {
        loadPeriode()
    }

    fun prevMonth() {
        val m = _mois.value
        val a = _annee.value
        if (m == 1) {
            _mois.value = 12
            _annee.value = a - 1
        } else {
            _mois.value = m - 1
        }
        loadPeriode()
    }

    fun nextMonth() {
        val m = _mois.value
        val a = _annee.value
        if (m == 12) {
            _mois.value = 1
            _annee.value = a + 1
        } else {
            _mois.value = m + 1
        }
        loadPeriode()
    }

    fun loadPeriode() {
        viewModelScope.launch {
            _periodeState.value = UiState.Loading
            _periodeState.value = when (val r = repository.getPayrollByPeriode(_mois.value, _annee.value)) {
                is NetworkResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                else -> UiState.Loading
            }
        }
    }

    fun runPayroll() {
        viewModelScope.launch {
            _runState.value = PayrollRunState(isLoading = true)
            when (val r = repository.runPayroll(_mois.value, _annee.value)) {
                is NetworkResult.Success -> {
                    _runState.value = PayrollRunState(result = r.data)
                    loadPeriode()
                }
                is NetworkResult.Error -> _runState.value = PayrollRunState(error = r.message)
                else -> Unit
            }
        }
    }

    fun dismissRunResult() {
        _runState.value = PayrollRunState()
    }
}
