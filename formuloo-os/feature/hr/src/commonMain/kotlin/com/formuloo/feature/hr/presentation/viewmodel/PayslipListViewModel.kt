package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Payslip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PayslipListViewModel(private val repository: HrRepository) : ViewModel() {

    private val _payslipsState = MutableStateFlow<UiState<List<Payslip>>>(UiState.Loading)
    val payslipsState: StateFlow<UiState<List<Payslip>>> = _payslipsState.asStateFlow()

    private val _selectedYear = MutableStateFlow(currentYearGuess())
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    init {
        loadPayslips()
    }

    fun selectYear(year: Int) {
        _selectedYear.value = year
        loadPayslips()
    }

    fun loadPayslips() {
        viewModelScope.launch {
            repository.getMyPayslips(annee = _selectedYear.value).collect { result ->
                _payslipsState.value = when (result) {
                    is NetworkResult.Loading -> UiState.Loading
                    is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                    is NetworkResult.Error -> UiState.Error(result.message, result.code)
                }
            }
        }
    }

    private companion object {
        // Pas de Clock multiplateforme disponible dans le projet — année par défaut figée ;
        // l'utilisateur change l'année via le sélecteur de l'écran.
        fun currentYearGuess() = 2026
    }
}
