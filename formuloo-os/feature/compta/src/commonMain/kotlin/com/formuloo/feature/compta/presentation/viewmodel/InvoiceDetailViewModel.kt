package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvoiceDetailViewModel(
    private val repository: ComptaRepository,
    private val invoiceId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Invoice>>(UiState.Loading)
    val state: StateFlow<UiState<Invoice>> = _state.asStateFlow()

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    init {
        loadInvoice()
    }

    fun loadInvoice() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.getInvoice(invoiceId)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    fun emettre() {
        viewModelScope.launch {
            _actionInProgress.value = true
            when (val result = repository.emettreInvoice(invoiceId)) {
                is NetworkResult.Success -> _state.value = UiState.Success(result.data)
                is NetworkResult.Error -> _state.value = UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> Unit
            }
            _actionInProgress.value = false
        }
    }

    fun emettreAvoir() {
        viewModelScope.launch {
            _actionInProgress.value = true
            when (val result = repository.emettreAvoirClient(invoiceId)) {
                is NetworkResult.Success -> _state.value = UiState.Success(result.data)
                is NetworkResult.Error -> _state.value = UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> Unit
            }
            _actionInProgress.value = false
        }
    }
}
