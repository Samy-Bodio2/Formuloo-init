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

class InvoiceCreateViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Invoice>?>(null)
    val state: StateFlow<UiState<Invoice>?> = _state.asStateFlow()

    fun create(
        clientNom: String,
        clientEmail: String?,
        description: String,
        quantite: Double,
        prixUnitaire: Double,
        tvaTaux: Double,
        dateEcheance: String,
    ) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (
                val result = repository.createInvoice(
                    clientNom = clientNom,
                    clientEmail = clientEmail,
                    lignes = listOf(Triple(description, quantite, prixUnitaire)),
                    tvaTaux = tvaTaux,
                    dateEcheance = dateEcheance,
                )
            ) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
