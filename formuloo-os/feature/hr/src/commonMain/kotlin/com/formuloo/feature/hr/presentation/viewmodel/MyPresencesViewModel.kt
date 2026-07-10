package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PresenceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PresenceSummary(
    val present: Int = 0,
    val absent: Int = 0,
    val retard: Int = 0,
    val conge: Int = 0,
    val ferie: Int = 0,
    val heuresTravaillees: Double = 0.0,
    val heuresSupplementaires: Double = 0.0,
)

class MyPresencesViewModel(private val repository: HrRepository) : ViewModel() {

    // Both targets are JVM (Android + Desktop) — java.util.Calendar is available in commonMain.
    private val now = java.util.Calendar.getInstance()

    private val _mois = MutableStateFlow(now.get(java.util.Calendar.MONTH) + 1)
    val mois: StateFlow<Int> = _mois.asStateFlow()

    private val _annee = MutableStateFlow(now.get(java.util.Calendar.YEAR))
    val annee: StateFlow<Int> = _annee.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<Presence>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Presence>>> = _state.asStateFlow()

    private val _summary = MutableStateFlow(PresenceSummary())
    val summary: StateFlow<PresenceSummary> = _summary.asStateFlow()

    init { load() }

    fun prevMonth() {
        val m = _mois.value
        val a = _annee.value
        if (m == 1) { _mois.value = 12; _annee.value = a - 1 } else _mois.value = m - 1
        load()
    }

    fun nextMonth() {
        val m = _mois.value
        val a = _annee.value
        if (m == 12) { _mois.value = 1; _annee.value = a + 1 } else _mois.value = m + 1
        load()
    }

    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repository.getMyPresences(mois = _mois.value, annee = _annee.value)) {
                is NetworkResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.date }
                    _summary.value = computeSummary(sorted)
                    _state.value = if (sorted.isEmpty()) UiState.Empty else UiState.Success(sorted)
                }
                is NetworkResult.Error -> _state.value = UiState.Error(result.message, result.code)
                else -> Unit
            }
        }
    }

    private fun computeSummary(list: List<Presence>): PresenceSummary = PresenceSummary(
        present = list.count { it.statut == PresenceStatus.PRESENT },
        absent = list.count { it.statut == PresenceStatus.ABSENT },
        retard = list.count { it.statut == PresenceStatus.RETARD },
        conge = list.count { it.statut == PresenceStatus.CONGE },
        ferie = list.count { it.statut == PresenceStatus.FERIE },
        heuresTravaillees = list.sumOf { it.heuresTravaillees?.toDoubleOrNull() ?: 0.0 },
        heuresSupplementaires = list.sumOf { it.heuresSupplementaires.toDoubleOrNull() ?: 0.0 },
    )
}
