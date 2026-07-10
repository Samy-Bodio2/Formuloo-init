package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.EcritureCreateDto
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.core.network.dto.compta.LigneEcritureCreateDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.CompteItemUi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LigneForm(
    val localId: Int,
    val compteNumero: String = "",
    val compteId: Int? = null,
    val compteLibelle: String = "",
    val libelle: String = "",
    val debit: String = "",
    val credit: String = "",
    val compteError: String? = null,
) {
    val hasDebit: Boolean get() = debit.toDoubleOrNull()?.let { it > 0 } ?: false
    val hasCredit: Boolean get() = credit.toDoubleOrNull()?.let { it > 0 } ?: false
}

data class SaisieUiState(
    // Chargement initial
    val isLoading: Boolean = true,
    val loadError: String? = null,
    // Données de référence
    val journaux: List<JournalDto> = emptyList(),
    val flatComptes: List<CompteItemUi> = emptyList(),
    val exerciceId: Int? = null,
    // Formulaire
    val selectedJournalId: Int? = null,
    val date: String = "",
    val libelle: String = "",
    val referencePiece: String = "",
    val lignes: List<LigneForm> = listOf(LigneForm(0), LigneForm(1)),
    // Totaux calculés
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isEquilibree: Boolean = false,
    // Soumission
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    // État brouillon sauvegardé
    val brouillonId: Int? = null,
    val showValidationDialog: Boolean = false,
    val isValidating: Boolean = false,
    val isDeleting: Boolean = false,
    // Succès final (écriture validée)
    val submitSuccess: Boolean = false,
    val successId: Int? = null,
    // Liste des écritures récentes
    val ecritures: List<EcritureDto> = emptyList(),
    val isLoadingEcritures: Boolean = false,
)

private fun SaisieUiState.withTotals(): SaisieUiState {
    val d = lignes.sumOf { it.debit.toDoubleOrNull() ?: 0.0 }
    val c = lignes.sumOf { it.credit.toDoubleOrNull() ?: 0.0 }
    return copy(totalDebit = d, totalCredit = c, isEquilibree = d > 0 && d == c)
}

class SaisieViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(SaisieUiState())
    val state: StateFlow<SaisieUiState> = _state.asStateFlow()

    private var nextLocalId = 2

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }

            val journauxDeferred = async { repository.getJournaux() }
            val comptesDeferred = async { repository.getAllComptes() }
            val statsDeferred = async { repository.getStats() }

            val journauxResult = journauxDeferred.await()
            if (journauxResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, loadError = journauxResult.message) }
                return@launch
            }

            val journaux = (journauxResult as NetworkResult.Success).data
            val exerciceId = (statsDeferred.await() as? NetworkResult.Success)?.data?.exerciceId
            val flatComptes = when (val r = comptesDeferred.await()) {
                is NetworkResult.Success -> r.data.map { c ->
                    CompteItemUi(
                        id = c.id,
                        numero = c.numero,
                        libelle = c.libelle,
                        soldeDebiteur = 0.0,
                        soldeCrediteur = 0.0,
                        isActif = c.isActif,
                    )
                }
                else -> emptyList()
            }

            _state.update { s ->
                s.copy(
                    isLoading = false,
                    journaux = journaux,
                    flatComptes = flatComptes,
                    exerciceId = exerciceId,
                    selectedJournalId = s.selectedJournalId ?: journaux.firstOrNull()?.id,
                )
            }

            // Charger les écritures récentes
            if (exerciceId != null) loadEcritures(exerciceId)
        }
    }

    private fun loadEcritures(exerciceId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingEcritures = true) }
            when (val r = repository.getEcritures(exerciceId = exerciceId)) {
                is NetworkResult.Success -> _state.update { it.copy(ecritures = r.data, isLoadingEcritures = false) }
                else -> _state.update { it.copy(isLoadingEcritures = false) }
            }
        }
    }

    fun refreshEcritures() {
        val exerciceId = _state.value.exerciceId ?: return
        loadEcritures(exerciceId)
    }

    // ── Formulaire ─────────────────────────────────────────────────────────────

    fun selectJournal(id: Int) = _state.update { it.copy(selectedJournalId = id, submitError = null) }

    fun setDate(date: String) = _state.update { it.copy(date = date, submitError = null) }

    fun setLibelle(libelle: String) = _state.update { it.copy(libelle = libelle, submitError = null) }

    fun setReferencePiece(ref: String) = _state.update { it.copy(referencePiece = ref, submitError = null) }

    fun updateLigneNumero(localId: Int, numero: String) {
        _state.update { s ->
            val compte = if (numero.length >= 3) s.flatComptes.find { it.numero == numero && it.isActif } else null
            val updated = s.lignes.map { l ->
                if (l.localId != localId) l
                else l.copy(
                    compteNumero = numero,
                    compteId = compte?.id,
                    compteLibelle = compte?.libelle ?: "",
                    compteError = if (numero.length >= 3 && compte == null) "Compte introuvable" else null,
                )
            }
            s.copy(lignes = updated)
        }
    }

    fun updateLigneLibelle(localId: Int, libelle: String) {
        _state.update { s ->
            s.copy(lignes = s.lignes.map { if (it.localId == localId) it.copy(libelle = libelle) else it })
        }
    }

    fun updateLigneDebit(localId: Int, value: String) {
        _state.update { s ->
            val updated = s.lignes.map { l ->
                if (l.localId != localId) l else l.copy(debit = value)
            }
            s.copy(lignes = updated).withTotals()
        }
    }

    fun updateLigneCredit(localId: Int, value: String) {
        _state.update { s ->
            val updated = s.lignes.map { l ->
                if (l.localId != localId) l else l.copy(credit = value)
            }
            s.copy(lignes = updated).withTotals()
        }
    }

    fun addLigne() {
        val id = nextLocalId++
        _state.update { s -> s.copy(lignes = s.lignes + LigneForm(localId = id)) }
    }

    fun removeLigne(localId: Int) {
        _state.update { s ->
            if (s.lignes.size <= 2) s
            else s.copy(lignes = s.lignes.filter { it.localId != localId }).withTotals()
        }
    }

    // ── Soumission (brouillon) ─────────────────────────────────────────────────

    private fun validate(): String? {
        val s = _state.value
        return when {
            s.exerciceId == null -> "Aucun exercice comptable ouvert."
            s.selectedJournalId == null -> "Sélectionnez un journal."
            s.date.isBlank() -> "Entrez la date de l'écriture (AAAA-MM-JJ)."
            s.libelle.isBlank() -> "Entrez un libellé pour l'écriture."
            s.lignes.any { it.compteId == null } -> "Toutes les lignes doivent avoir un compte valide."
            s.lignes.any { it.hasDebit && it.hasCredit } -> "Une ligne ne peut pas avoir à la fois un débit et un crédit."
            !s.isEquilibree -> "L'écriture doit être équilibrée (Total Débit = Total Crédit)."
            else -> null
        }
    }

    fun saveAsBrouillon() {
        val error = validate()
        if (error != null) {
            _state.update { it.copy(submitError = error) }
            return
        }
        val s = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, submitError = null) }

            val dto = EcritureCreateDto(
                journalId = s.selectedJournalId!!,
                exerciceId = s.exerciceId!!,
                dateEcriture = s.date,
                libelle = s.libelle,
                referencePiece = s.referencePiece,
                lignes = s.lignes.map {
                    LigneEcritureCreateDto(
                        compteId = it.compteId!!,
                        libelle = it.libelle,
                        debit = it.debit.toDoubleOrNull() ?: 0.0,
                        credit = it.credit.toDoubleOrNull() ?: 0.0,
                    )
                },
            )

            when (val result = repository.createEcriture(dto)) {
                is NetworkResult.Error -> _state.update { it.copy(isSubmitting = false, submitError = result.message) }
                is NetworkResult.Success -> {
                    _state.update { it.copy(isSubmitting = false, brouillonId = result.data.id) }
                    refreshEcritures()
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Validation du brouillon ───────────────────────────────────────────────

    fun requestValidation() = _state.update { it.copy(showValidationDialog = true) }

    fun dismissValidationDialog() = _state.update { it.copy(showValidationDialog = false) }

    fun confirmValidation() {
        val brouillonId = _state.value.brouillonId ?: return
        _state.update { it.copy(showValidationDialog = false, isValidating = true, submitError = null) }

        viewModelScope.launch {
            when (val result = repository.validerEcriture(brouillonId)) {
                is NetworkResult.Error -> _state.update { it.copy(isValidating = false, submitError = result.message) }
                is NetworkResult.Success -> {
                    _state.update { it.copy(isValidating = false, submitSuccess = true, successId = brouillonId, brouillonId = null) }
                    refreshEcritures()
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun deleteBrouillon() {
        val brouillonId = _state.value.brouillonId ?: return
        _state.update { it.copy(isDeleting = true, submitError = null) }

        viewModelScope.launch {
            when (val result = repository.deleteEcriture(brouillonId)) {
                is NetworkResult.Error -> _state.update { it.copy(isDeleting = false, submitError = result.message) }
                is NetworkResult.Success -> {
                    _state.update { it.copy(isDeleting = false, brouillonId = null) }
                    refreshEcritures()
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Reset formulaire ──────────────────────────────────────────────────────

    fun dismissSuccess() {
        nextLocalId = 2
        _state.update { s ->
            SaisieUiState(
                journaux = s.journaux,
                flatComptes = s.flatComptes,
                exerciceId = s.exerciceId,
                selectedJournalId = s.selectedJournalId,
                ecritures = s.ecritures,
                isLoading = false,
            )
        }
    }
}
