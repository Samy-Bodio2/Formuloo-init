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

class InvoiceListViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Invoice>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Invoice>>> = _state.asStateFlow()

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.getInvoices()) {
                is NetworkResult.Success -> {
                    val items = result.data.items
                    if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                }
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
