package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.DeclarationTVADto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TVAPeriodeMode { MENSUEL, TRIMESTRIEL }

val MOIS_LABELS = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

val MOIS_COURTS = listOf(
    "Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
    "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc",
)

data class DeclarationTVAUiState(
    val isLoadingInit: Boolean = true,
    val initError: String? = null,
    val availableYears: List<Int> = emptyList(),
    val periodeMode: TVAPeriodeMode = TVAPeriodeMode.MENSUEL,
    val selectedYear: Int = 0,
    val selectedMonth: Int = 1,
    val selectedTrimestre: Int = 1,
    val declaration: DeclarationTVADto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val dateDebut: String
        get() = when (periodeMode) {
            TVAPeriodeMode.MENSUEL ->
                "$selectedYear-${selectedMonth.toString().padStart(2, '0')}-01"
            TVAPeriodeMode.TRIMESTRIEL -> {
                val m = (selectedTrimestre - 1) * 3 + 1
                "$selectedYear-${m.toString().padStart(2, '0')}-01"
            }
        }

    val dateFin: String
        get() = when (periodeMode) {
            TVAPeriodeMode.MENSUEL -> {
                val last = daysInMonth(selectedYear, selectedMonth)
                "$selectedYear-${selectedMonth.toString().padStart(2, '0')}-${last.toString().padStart(2, '0')}"
            }
            TVAPeriodeMode.TRIMESTRIEL -> {
                val m = selectedTrimestre * 3
                val last = daysInMonth(selectedYear, m)
                "$selectedYear-${m.toString().padStart(2, '0')}-${last.toString().padStart(2, '0')}"
            }
        }

    val periodeLabel: String
        get() = when (periodeMode) {
            TVAPeriodeMode.MENSUEL -> "${MOIS_LABELS.getOrElse(selectedMonth - 1) { selectedMonth.toString() }} $selectedYear"
            TVAPeriodeMode.TRIMESTRIEL -> "T$selectedTrimestre $selectedYear"
        }

    val trimestreLabel: (Int) -> String
        get() = { t ->
            val m1 = (t - 1) * 3 + 1
            val m3 = t * 3
            "${MOIS_COURTS.getOrElse(m1 - 1) { "" }}–${MOIS_COURTS.getOrElse(m3 - 1) { "" }}"
        }
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}

class DeclarationTVAViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(DeclarationTVAUiState())
    val state: StateFlow<DeclarationTVAUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInit = true, initError = null) }
            when (val r = repository.getExercices()) {
                is NetworkResult.Success -> {
                    val years = r.data.map { it.annee }.distinct().sorted()
                    val defaultYear = r.data.firstOrNull { it.statut == "OUVERT" }?.annee
                        ?: years.lastOrNull()
                        ?: 2025
                    _state.update {
                        it.copy(
                            isLoadingInit = false,
                            availableYears = years,
                            selectedYear = if (it.selectedYear == 0) defaultYear else it.selectedYear,
                        )
                    }
                    loadDeclaration()
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingInit = false, initError = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadDeclaration() {
        val s = _state.value
        if (s.selectedYear == 0) return
        if (s.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, declaration = null) }
            when (val r = repository.getDeclarationTVA(s.dateDebut, s.dateFin)) {
                is NetworkResult.Success -> _state.update { it.copy(declaration = r.data, isLoading = false) }
                is NetworkResult.Error -> _state.update { it.copy(error = r.message, isLoading = false) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setPeriodeMode(mode: TVAPeriodeMode) {
        _state.update { it.copy(periodeMode = mode, declaration = null, error = null) }
        loadDeclaration()
    }

    fun selectYear(year: Int) {
        _state.update { it.copy(selectedYear = year, declaration = null, error = null) }
        loadDeclaration()
    }

    fun selectMonth(month: Int) {
        _state.update { it.copy(selectedMonth = month, declaration = null, error = null) }
        loadDeclaration()
    }

    fun selectTrimestre(t: Int) {
        _state.update { it.copy(selectedTrimestre = t, declaration = null, error = null) }
        loadDeclaration()
    }
}
