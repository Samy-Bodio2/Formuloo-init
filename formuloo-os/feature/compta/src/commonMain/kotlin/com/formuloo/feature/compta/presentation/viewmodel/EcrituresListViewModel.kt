package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EcrituresListUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val ecritures: List<EcritureDto> = emptyList(),
    val journalCodeFilter: String? = null,
    val expandedEcritureId: Int? = null,
    val exerciceAnnee: Int? = null,
) {
    val filteredEcritures: List<EcritureDto>
        get() = if (journalCodeFilter == null) ecritures
                else ecritures.filter { it.journalCode == journalCodeFilter }

    val nbEnAttente: Int
        get() = ecritures.count { it.statut != "VALIDEE" }

    val totalDebit: Double
        get() = ecritures.sumOf { it.totalDebit.toDoubleOrNull() ?: 0.0 }

    val totalCredit: Double
        get() = ecritures.sumOf { it.totalCredit.toDoubleOrNull() ?: 0.0 }

    val distinctJournalCodes: List<String>
        get() = ecritures.map { it.journalCode }.distinct().sorted()
}

class EcrituresListViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(EcrituresListUiState())
    val state: StateFlow<EcrituresListUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }

            val statsDeferred = async { repository.getStats() }
            val ecrituresDeferred = async { repository.getEcritures(page = 1) }

            val stats = (statsDeferred.await() as? NetworkResult.Success)?.data
            val ecrituresResult = ecrituresDeferred.await()

            if (ecrituresResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, loadError = ecrituresResult.message) }
                return@launch
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    ecritures = (ecrituresResult as NetworkResult.Success).data,
                    exerciceAnnee = stats?.exerciceAnnee,
                )
            }
        }
    }

    fun setJournalCodeFilter(code: String?) = _state.update { it.copy(journalCodeFilter = code) }

    fun toggleEcriture(id: Int) = _state.update {
        it.copy(expandedEcritureId = if (it.expandedEcritureId == id) null else id)
    }
}
