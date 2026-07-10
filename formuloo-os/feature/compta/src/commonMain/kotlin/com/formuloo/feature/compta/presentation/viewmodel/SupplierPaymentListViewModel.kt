package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.SupplierPayment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupplierPaymentListViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<SupplierPayment>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SupplierPayment>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val r = repository.getSupplierPayments()) {
                is NetworkResult.Success -> {
                    val items = r.data.items
                    if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                }
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
