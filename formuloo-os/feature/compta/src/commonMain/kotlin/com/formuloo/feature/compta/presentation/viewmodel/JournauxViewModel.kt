package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.JournalCreateDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournauxUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val journaux: List<JournalDto> = emptyList(),
    val typeFilter: String? = null,
    // Expansion & écritures inline
    val expandedJournalId: Int? = null,
    val journalEcritures: Map<Int, List<EcritureDto>> = emptyMap(),
    val loadingEcrituresForJournal: Int? = null,
    val exerciceId: Int? = null,
    val exerciceAnnee: Int? = null,
    // Dialogue de création
    val showCreateDialog: Boolean = false,
    val createCode: String = "",
    val createLibelle: String = "",
    val createType: String = "OD",
    val isCreating: Boolean = false,
    val createError: String? = null,
) {
    val filteredJournaux: List<JournalDto>
        get() = if (typeFilter == null) journaux else journaux.filter { it.type == typeFilter }
}

class JournauxViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(JournauxUiState())
    val state: StateFlow<JournauxUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }

            val journauxDeferred = async { repository.getJournaux() }
            val statsDeferred = async { repository.getStats() }

            val journauxResult = journauxDeferred.await()
            if (journauxResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, loadError = journauxResult.message) }
                return@launch
            }

            val journaux = (journauxResult as NetworkResult.Success).data
            val stats = (statsDeferred.await() as? NetworkResult.Success)?.data
            val exerciceId = stats?.exerciceId
            val exerciceAnnee = stats?.exerciceAnnee

            _state.update {
                it.copy(
                    isLoading = false,
                    journaux = journaux,
                    exerciceId = exerciceId,
                    exerciceAnnee = exerciceAnnee,
                    // préserver l'expansion si toujours valide
                    expandedJournalId = if (journaux.any { j -> j.id == it.expandedJournalId }) it.expandedJournalId else null,
                )
            }
        }
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    fun setTypeFilter(type: String?) = _state.update { it.copy(typeFilter = type) }

    // ── Expansion inline des écritures ────────────────────────────────────────

    fun toggleJournal(journalId: Int) {
        val s = _state.value
        if (s.expandedJournalId == journalId) {
            _state.update { it.copy(expandedJournalId = null) }
            return
        }
        _state.update { it.copy(expandedJournalId = journalId) }
        // Charger les écritures si pas encore en cache
        if (!s.journalEcritures.containsKey(journalId)) {
            loadEcrituresForJournal(journalId)
        }
    }

    private fun loadEcrituresForJournal(journalId: Int) {
        val exerciceId = _state.value.exerciceId
        viewModelScope.launch {
            _state.update { it.copy(loadingEcrituresForJournal = journalId) }
            val result = repository.getEcritures(journalId = journalId, exerciceId = exerciceId)
            _state.update { s ->
                val updated = s.journalEcritures.toMutableMap()
                if (result is NetworkResult.Success) updated[journalId] = result.data
                s.copy(
                    journalEcritures = updated,
                    loadingEcrituresForJournal = if (s.loadingEcrituresForJournal == journalId) null else s.loadingEcrituresForJournal,
                )
            }
        }
    }

    fun refreshExpandedJournal() {
        val journalId = _state.value.expandedJournalId ?: return
        _state.update { s ->
            val updated = s.journalEcritures.toMutableMap()
            updated.remove(journalId)
            s.copy(journalEcritures = updated)
        }
        loadEcrituresForJournal(journalId)
    }

    // ── Création de journal ───────────────────────────────────────────────────

    fun showCreateDialog() = _state.update {
        it.copy(showCreateDialog = true, createCode = "", createLibelle = "", createType = "OD", createError = null)
    }

    fun dismissCreateDialog() = _state.update { it.copy(showCreateDialog = false, createError = null) }

    fun setCreateCode(v: String) = _state.update { it.copy(createCode = v.uppercase().take(10), createError = null) }
    fun setCreateLibelle(v: String) = _state.update { it.copy(createLibelle = v, createError = null) }
    fun setCreateType(v: String) = _state.update { it.copy(createType = v, createError = null) }

    fun createJournal() {
        val s = _state.value
        if (s.createCode.isBlank()) {
            _state.update { it.copy(createError = "Le code est requis.") }
            return
        }
        if (s.createLibelle.isBlank()) {
            _state.update { it.copy(createError = "Le libellé est requis.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            when (val result = repository.createJournal(JournalCreateDto(s.createCode, s.createLibelle, s.createType))) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isCreating = false, showCreateDialog = false) }
                    load()
                }
                is NetworkResult.Error -> _state.update { it.copy(isCreating = false, createError = result.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
