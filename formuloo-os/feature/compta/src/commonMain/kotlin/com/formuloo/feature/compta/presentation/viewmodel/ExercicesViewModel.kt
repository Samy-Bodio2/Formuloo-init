package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.ExerciceCloturerResponseDto
import com.formuloo.core.network.dto.compta.ExerciceCreateDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExercicesUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val exercices: List<ExerciceDto> = emptyList(),
    // Dialogue de clôture
    val showCloturerDialog: Boolean = false,
    val exerciceACloturer: ExerciceDto? = null,
    val isCloturing: Boolean = false,
    val cloturerError: String? = null,
    val cloturerResult: ExerciceCloturerResponseDto? = null,
    // Dialogue de création
    val showCreateDialog: Boolean = false,
    val createAnnee: String = "",
    val createDateDebut: String = "",
    val createDateFin: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
) {
    val exerciceOuvert: ExerciceDto? get() = exercices.firstOrNull { it.statut == "OUVERT" }
    val exercicesClotures: List<ExerciceDto> get() = exercices.filter { it.statut == "CLOTURE" }
    val peutCreerNouveau: Boolean get() = exerciceOuvert == null
}

class ExercicesViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(ExercicesUiState())
    val state: StateFlow<ExercicesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            when (val result = repository.getExercices()) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isLoading = false, exercices = result.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isLoading = false, loadError = result.message)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Clôture ───────────────────────────────────────────────────────────────

    fun requestCloturer(exercice: ExerciceDto) =
        _state.update { it.copy(showCloturerDialog = true, exerciceACloturer = exercice, cloturerError = null) }

    fun dismissCloturerDialog() =
        _state.update { it.copy(showCloturerDialog = false, exerciceACloturer = null, cloturerError = null) }

    fun confirmCloturer() {
        val exerciceId = _state.value.exerciceACloturer?.id ?: return
        _state.update { it.copy(showCloturerDialog = false, isCloturing = true, cloturerError = null) }
        viewModelScope.launch {
            when (val result = repository.cloturerExercice(exerciceId)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isCloturing = false, cloturerResult = result.data, exerciceACloturer = null) }
                    load()
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isCloturing = false, cloturerError = result.message, showCloturerDialog = true)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun dismissCloturerResult() = _state.update { it.copy(cloturerResult = null) }

    // ── Création ──────────────────────────────────────────────────────────────

    fun showCreateDialog() {
        val nextAnnee = (_state.value.exercices.maxOfOrNull { it.annee } ?: (currentYear() - 1)) + 1
        _state.update {
            it.copy(
                showCreateDialog = true,
                createAnnee = nextAnnee.toString(),
                createDateDebut = "$nextAnnee-01-01",
                createDateFin = "$nextAnnee-12-31",
                createError = null,
            )
        }
    }

    fun dismissCreateDialog() = _state.update { it.copy(showCreateDialog = false, createError = null) }

    fun setCreateAnnee(v: String) {
        val annee = v.toIntOrNull()
        _state.update {
            it.copy(
                createAnnee = v,
                createDateDebut = if (annee != null) "$annee-01-01" else it.createDateDebut,
                createDateFin = if (annee != null) "$annee-12-31" else it.createDateFin,
                createError = null,
            )
        }
    }

    fun setCreateDateDebut(v: String) = _state.update { it.copy(createDateDebut = v, createError = null) }
    fun setCreateDateFin(v: String) = _state.update { it.copy(createDateFin = v, createError = null) }

    fun createExercice() {
        val s = _state.value
        val annee = s.createAnnee.toIntOrNull()
        if (annee == null) {
            _state.update { it.copy(createError = "Année invalide.") }
            return
        }
        if (s.createDateDebut.isBlank() || s.createDateFin.isBlank()) {
            _state.update { it.copy(createError = "Les dates sont requises.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            when (val result = repository.createExercice(
                ExerciceCreateDto(annee, s.createDateDebut, s.createDateFin)
            )) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isCreating = false, showCreateDialog = false) }
                    load()
                }
                is NetworkResult.Error -> _state.update { it.copy(isCreating = false, createError = result.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    private fun currentYear(): Int {
        // KMP-safe: pas de java.util.Date ici — on parse depuis les exercices existants
        return _state.value.exercices.maxOfOrNull { it.annee } ?: 2024
    }
}
