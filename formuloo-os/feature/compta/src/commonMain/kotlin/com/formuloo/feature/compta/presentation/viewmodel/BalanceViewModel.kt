package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.BalanceDto
import com.formuloo.core.network.dto.compta.BalanceLigneDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BalanceSoldeFilter { TOUS, DEBITEURS, CREDITEURS }

data class BalanceUiState(
    val isLoadingInit: Boolean = true,
    val initError: String? = null,
    val exercices: List<ExerciceDto> = emptyList(),
    val selectedExerciceId: Int? = null,
    val balance: BalanceDto? = null,
    val isLoadingBalance: Boolean = false,
    val balanceError: String? = null,
    val searchQuery: String = "",
    val classeFilter: Int? = null,
    val soldeFilter: BalanceSoldeFilter = BalanceSoldeFilter.TOUS,
    val showZeroMovements: Boolean = false,
) {
    val selectedExercice: ExerciceDto? get() = exercices.firstOrNull { it.id == selectedExerciceId }

    private val allLignes: List<BalanceLigneDto> get() = balance?.lignes ?: emptyList()

    val filteredLignes: List<BalanceLigneDto>
        get() {
            var result = allLignes
            if (!showZeroMovements) {
                result = result.filter {
                    (it.totalDebit.toDoubleOrNull() ?: 0.0) > 0 ||
                        (it.totalCredit.toDoubleOrNull() ?: 0.0) > 0
                }
            }
            if (classeFilter != null) result = result.filter { it.classe == classeFilter }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                result = result.filter { it.compteNumero.startsWith(q) || it.compteLibelle.lowercase().contains(q) }
            }
            result = when (soldeFilter) {
                BalanceSoldeFilter.DEBITEURS -> result.filter { (it.soldeDebiteur.toDoubleOrNull() ?: 0.0) > 0 }
                BalanceSoldeFilter.CREDITEURS -> result.filter { (it.soldeCrediteur.toDoubleOrNull() ?: 0.0) > 0 }
                BalanceSoldeFilter.TOUS -> result
            }
            return result
        }

    val groupedByClasse: Map<Int, List<BalanceLigneDto>>
        get() = filteredLignes.groupBy { it.classe }.toSortedMap()

    val nbComptes: Int get() = allLignes.size
    val nbComptesActifs: Int
        get() = allLignes.count {
            (it.totalDebit.toDoubleOrNull() ?: 0.0) > 0 || (it.totalCredit.toDoubleOrNull() ?: 0.0) > 0
        }
    val totalMouvementsDebit: Double get() = balance?.totalDebit?.toDoubleOrNull() ?: 0.0
    val totalMouvementsCredit: Double get() = balance?.totalCredit?.toDoubleOrNull() ?: 0.0
    val totalSoldesDebiteurs: Double
        get() = allLignes.sumOf { it.soldeDebiteur.toDoubleOrNull() ?: 0.0 }
    val totalSoldesCrediteurs: Double
        get() = allLignes.sumOf { it.soldeCrediteur.toDoubleOrNull() ?: 0.0 }
    val availableClasses: List<Int>
        get() = allLignes.map { it.classe }.distinct().sorted()
    val nbFiltered: Int get() = filteredLignes.size
    val isEquilibre: Boolean
        get() {
            if (balance == null) return false
            return kotlin.math.abs(totalSoldesDebiteurs - totalSoldesCrediteurs) < 0.01
        }
    val isFiltered: Boolean
        get() = classeFilter != null || searchQuery.isNotBlank() || soldeFilter != BalanceSoldeFilter.TOUS || showZeroMovements
}

class BalanceViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(BalanceUiState())
    val state: StateFlow<BalanceUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInit = true, initError = null) }
            when (val r = repository.getExercices()) {
                is NetworkResult.Success -> {
                    val exercices = r.data
                    val defaultId = exercices.firstOrNull { it.statut == "OUVERT" }?.id
                        ?: exercices.firstOrNull()?.id
                    _state.update {
                        it.copy(
                            isLoadingInit = false,
                            exercices = exercices,
                            selectedExerciceId = it.selectedExerciceId ?: defaultId,
                        )
                    }
                    if (defaultId != null) loadBalance()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingInit = false, initError = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun selectExercice(id: Int) {
        _state.update { it.copy(selectedExerciceId = id, balance = null, balanceError = null) }
        loadBalance()
    }

    fun loadBalance() {
        val exerciceId = _state.value.selectedExerciceId ?: return
        if (_state.value.isLoadingBalance) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBalance = true, balanceError = null) }
            when (val r = repository.getBalance(exerciceId)) {
                is NetworkResult.Success -> _state.update { it.copy(balance = r.data, isLoadingBalance = false) }
                is NetworkResult.Error -> _state.update { it.copy(balanceError = r.message, isLoadingBalance = false) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }
    fun setClasseFilter(classe: Int?) = _state.update { it.copy(classeFilter = classe) }
    fun setSoldeFilter(f: BalanceSoldeFilter) = _state.update { it.copy(soldeFilter = f) }
    fun toggleShowZero() = _state.update { it.copy(showZeroMovements = !it.showZeroMovements) }
}
