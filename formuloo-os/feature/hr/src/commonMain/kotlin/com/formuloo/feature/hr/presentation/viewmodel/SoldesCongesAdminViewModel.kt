package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.AdminLeaveBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoldesCongesAdminViewModel(private val repository: HrRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AdminLeaveBalance>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminLeaveBalance>>> = _state.asStateFlow()

    private val _annee = MutableStateFlow(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
    val annee: StateFlow<Int> = _annee.asStateFlow()

    init { load() }

    fun setAnnee(annee: Int) {
        _annee.value = annee
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repository.getAdminLeaveBalances(annee = _annee.value)) {
                is NetworkResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                else -> UiState.Loading
            }
        }
    }
}
