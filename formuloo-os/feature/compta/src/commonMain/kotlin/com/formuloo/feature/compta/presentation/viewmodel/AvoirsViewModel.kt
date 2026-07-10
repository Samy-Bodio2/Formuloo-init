package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AvoirsUiState(
    val selectedTab: Int = 0,
    val isLoadingClients: Boolean = true,
    val errorClients: String? = null,
    val avoirsClients: List<Invoice> = emptyList(),
    val isLoadingFournisseurs: Boolean = true,
    val errorFournisseurs: String? = null,
    val avoirsFournisseurs: List<PurchaseInvoice> = emptyList(),
) {
    val totalMontantClients: Double get() = avoirsClients.sumOf { it.montantTtc }
    val totalMontantFournisseurs: Double get() = avoirsFournisseurs.sumOf { it.montantTtc }
}

class AvoirsViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(AvoirsUiState())
    val state: StateFlow<AvoirsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        loadClients()
        loadFournisseurs()
    }

    fun selectTab(index: Int) = _state.update { it.copy(selectedTab = index) }

    private fun loadClients() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingClients = true, errorClients = null) }
            when (val r = repository.getAvoirsClients()) {
                is NetworkResult.Success -> _state.update { it.copy(isLoadingClients = false, avoirsClients = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingClients = false, errorClients = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private fun loadFournisseurs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingFournisseurs = true, errorFournisseurs = null) }
            when (val r = repository.getAvoirsFournisseurs()) {
                is NetworkResult.Success -> _state.update { it.copy(isLoadingFournisseurs = false, avoirsFournisseurs = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingFournisseurs = false, errorFournisseurs = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
