package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PurchaseInvoiceDetailViewModel(
    private val repository: ComptaRepository,
    private val purchaseInvoiceId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PurchaseInvoice>>(UiState.Loading)
    val state: StateFlow<UiState<PurchaseInvoice>> = _state.asStateFlow()

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        loadPurchaseInvoice()
    }

    fun loadPurchaseInvoice() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.getPurchaseInvoice(purchaseInvoiceId)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    fun recevoir() = runAction { repository.recevoirPurchaseInvoice(purchaseInvoiceId) }

    fun valider() = runAction { repository.validerPurchaseInvoice(purchaseInvoiceId) }

    fun emettreAvoir() = runAction { repository.emettreAvoirFournisseur(purchaseInvoiceId) }

    fun payer(montant: Double, modePaiement: PaymentMode, datePaiement: String, reference: String?) {
        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null
            when (
                val result = repository.payerPurchaseInvoice(
                    purchaseInvoiceId,
                    montant,
                    modePaiement,
                    datePaiement,
                    reference,
                )
            ) {
                is NetworkResult.Success -> loadPurchaseInvoice()
                is NetworkResult.Error -> _actionError.value = result.message
                is NetworkResult.Loading -> Unit
            }
            _actionInProgress.value = false
        }
    }

    private fun runAction(block: suspend () -> NetworkResult<PurchaseInvoice>) {
        viewModelScope.launch {
            _actionInProgress.value = true
            _actionError.value = null
            when (val result = block()) {
                is NetworkResult.Success -> _state.value = UiState.Success(result.data)
                is NetworkResult.Error -> _actionError.value = result.message
                is NetworkResult.Loading -> Unit
            }
            _actionInProgress.value = false
        }
    }
}
