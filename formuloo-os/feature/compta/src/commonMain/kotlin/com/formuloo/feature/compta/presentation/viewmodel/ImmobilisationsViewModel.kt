package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.AmortirResponseDto
import com.formuloo.core.network.dto.compta.ImmobilisationCreateDto
import com.formuloo.core.network.dto.compta.ImmobilisationDto
import com.formuloo.core.network.dto.compta.PlanAmortissementDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// SYSCOHADA: compte d'immobilisation par défaut par catégorie
val COMPTE_PAR_CATEGORIE = mapOf(
    "INCORPORELLE" to "2118",
    "TERRAIN" to "2213",
    "CONSTRUCTION" to "2312",
    "MATERIEL" to "2184",
    "MOBILIER" to "2245",
    "VEHICULE" to "2243",
    "FINANCIERE" to "2611",
)

val METHODE_PAR_CATEGORIE = mapOf(
    "TERRAIN" to "NON_AMORTISSABLE",
    "FINANCIERE" to "NON_AMORTISSABLE",
)

data class ImmobilisationsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val immobilisations: List<ImmobilisationDto> = emptyList(),
    val statutFilter: String? = null,
    val expandedImmoId: Int? = null,
    val plans: Map<Int, PlanAmortissementDto> = emptyMap(),
    val loadingPlanForId: Int? = null,
    val planError: String? = null,
    // Create
    val showCreateDialog: Boolean = false,
    val createCode: String = "",
    val createDesignation: String = "",
    val createCategorie: String = "MATERIEL",
    val createNumeroCompte: String = "2184",
    val createFournisseur: String = "",
    val createValeurOrigine: String = "",
    val createValeurResiduelle: String = "0",
    val createMethode: String = "LINEAIRE",
    val createDureeVie: String = "5",
    val createDateMiseEnService: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
    // Amortir confirm
    val showAmortirDialog: Boolean = false,
    val immoToAmortir: ImmobilisationDto? = null,
    val isAmortissing: Boolean = false,
    val amortirError: String? = null,
    val amortirResult: AmortirResponseDto? = null,
    // Céder confirm
    val showCederDialog: Boolean = false,
    val immoToCeder: ImmobilisationDto? = null,
    val cederValeurNette: String = "0",
    val isCeding: Boolean = false,
    val cederError: String? = null,
) {
    val filteredImmobilisations: List<ImmobilisationDto>
        get() = if (statutFilter == null) immobilisations else immobilisations.filter { it.statut == statutFilter }

    val nbActive: Int get() = immobilisations.count { it.statut == "ACTIVE" }
    val nbAmortie: Int get() = immobilisations.count { it.statut == "AMORTIE" }
    val nbCedee: Int get() = immobilisations.count { it.statut == "CEDEE" }
    val valeurBrute: Double get() = immobilisations.sumOf { it.valeurOrigine.toDoubleOrNull() ?: 0.0 }
    val valeurNetteTotal: Double get() = immobilisations.sumOf { it.valeurNetteComptable.toDoubleOrNull() ?: 0.0 }
    val cumulAmortissementsTotal: Double get() = immobilisations.sumOf { it.cumulAmortissements.toDoubleOrNull() ?: 0.0 }
}

class ImmobilisationsViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(ImmobilisationsUiState())
    val state: StateFlow<ImmobilisationsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repository.getImmobilisations()) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, immobilisations = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setStatutFilter(statut: String?) = _state.update { it.copy(statutFilter = statut) }

    // ── Expand / Plan ─────────────────────────────────────────────────────────

    fun toggleExpand(id: Int) {
        val current = _state.value.expandedImmoId
        if (current == id) {
            _state.update { it.copy(expandedImmoId = null) }
        } else {
            _state.update { it.copy(expandedImmoId = id, planError = null) }
            if (!_state.value.plans.containsKey(id)) {
                loadPlan(id)
            }
        }
    }

    private fun loadPlan(id: Int) {
        viewModelScope.launch {
            _state.update { it.copy(loadingPlanForId = id, planError = null) }
            when (val r = repository.getPlanAmortissement(id)) {
                is NetworkResult.Success -> _state.update { s ->
                    s.copy(plans = s.plans + (id to r.data), loadingPlanForId = null)
                }
                is NetworkResult.Error -> _state.update { it.copy(planError = r.message, loadingPlanForId = null) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    fun showCreateDialog() = _state.update {
        it.copy(showCreateDialog = true, createCode = "", createDesignation = "", createCategorie = "MATERIEL",
            createNumeroCompte = "2184", createFournisseur = "", createValeurOrigine = "",
            createValeurResiduelle = "0", createMethode = "LINEAIRE", createDureeVie = "5",
            createDateMiseEnService = "", createError = null)
    }

    fun dismissCreateDialog() = _state.update { it.copy(showCreateDialog = false, createError = null) }

    fun setCreateCode(v: String) = _state.update { it.copy(createCode = v) }
    fun setCreateDesignation(v: String) = _state.update { it.copy(createDesignation = v) }
    fun setCreateCategorie(v: String) {
        val compte = COMPTE_PAR_CATEGORIE[v] ?: "2184"
        val methode = METHODE_PAR_CATEGORIE[v] ?: "LINEAIRE"
        val duree = if (methode == "NON_AMORTISSABLE") "0" else _state.value.createDureeVie
        _state.update { it.copy(createCategorie = v, createNumeroCompte = compte, createMethode = methode, createDureeVie = duree) }
    }
    fun setCreateNumeroCompte(v: String) = _state.update { it.copy(createNumeroCompte = v) }
    fun setCreateFournisseur(v: String) = _state.update { it.copy(createFournisseur = v) }
    fun setCreateValeurOrigine(v: String) = _state.update { it.copy(createValeurOrigine = v) }
    fun setCreateValeurResiduelle(v: String) = _state.update { it.copy(createValeurResiduelle = v) }
    fun setCreateMethode(v: String) = _state.update { it.copy(createMethode = v) }
    fun setCreateDureeVie(v: String) = _state.update { it.copy(createDureeVie = v) }
    fun setCreateDateMiseEnService(v: String) = _state.update { it.copy(createDateMiseEnService = v) }

    fun createImmobilisation() {
        val s = _state.value
        val valeurOrigine = s.createValeurOrigine.toDoubleOrNull()
        if (s.createCode.isBlank() || s.createDesignation.isBlank() || valeurOrigine == null || valeurOrigine <= 0) {
            _state.update { it.copy(createError = "Code, désignation et valeur d'origine sont requis.") }
            return
        }
        if (s.createDateMiseEnService.isBlank()) {
            _state.update { it.copy(createError = "La date de mise en service est requise.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, createError = null) }
            val dto = ImmobilisationCreateDto(
                code = s.createCode.trim(),
                designation = s.createDesignation.trim(),
                categorie = s.createCategorie,
                numeroCompte = s.createNumeroCompte.trim(),
                fournisseur = s.createFournisseur.trim(),
                valeurOrigine = valeurOrigine,
                valeurResiduelle = s.createValeurResiduelle.toDoubleOrNull() ?: 0.0,
                methode = s.createMethode,
                dureeVie = s.createDureeVie.toIntOrNull() ?: 5,
                dateMiseEnService = s.createDateMiseEnService.trim(),
            )
            when (val r = repository.createImmobilisation(dto)) {
                is NetworkResult.Success -> {
                    _state.update { it.copy(isCreating = false, showCreateDialog = false,
                        immobilisations = listOf(r.data) + it.immobilisations) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isCreating = false, createError = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Amortir ───────────────────────────────────────────────────────────────

    fun requestAmortir(immo: ImmobilisationDto) =
        _state.update { it.copy(showAmortirDialog = true, immoToAmortir = immo, amortirError = null, amortirResult = null) }

    fun dismissAmortirDialog() =
        _state.update { it.copy(showAmortirDialog = false, immoToAmortir = null, amortirError = null) }

    fun dismissAmortirResult() =
        _state.update { it.copy(amortirResult = null) }

    fun confirmAmortir() {
        val immo = _state.value.immoToAmortir ?: return
        viewModelScope.launch {
            _state.update { it.copy(isAmortissing = true, amortirError = null) }
            when (val r = repository.amortirImmobilisation(immo.id)) {
                is NetworkResult.Success -> {
                    // Refresh list + invalidate cached plan
                    val updatedImmo = r.data.dotation.let { _ ->
                        // Update the immobilisation in list with new VNC from response
                        _state.value.immobilisations.map { existing ->
                            if (existing.id == immo.id)
                                existing.copy(
                                    valeurNetteComptable = r.data.valeurNetteComptable,
                                    cumulAmortissements = (
                                        (existing.cumulAmortissements.toDoubleOrNull() ?: 0.0) +
                                        (r.data.dotation.montant.toDoubleOrNull() ?: 0.0)
                                    ).toString(),
                                )
                            else existing
                        }
                    }
                    _state.update { it.copy(
                        isAmortissing = false,
                        showAmortirDialog = false,
                        amortirResult = r.data,
                        immobilisations = updatedImmo,
                        plans = it.plans - immo.id, // invalidate cached plan
                    ) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isAmortissing = false, amortirError = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Céder ─────────────────────────────────────────────────────────────────

    fun requestCeder(immo: ImmobilisationDto) =
        _state.update { it.copy(showCederDialog = true, immoToCeder = immo, cederValeurNette = "0", cederError = null) }

    fun dismissCederDialog() =
        _state.update { it.copy(showCederDialog = false, immoToCeder = null, cederError = null) }

    fun setCederValeurNette(v: String) = _state.update { it.copy(cederValeurNette = v) }

    fun confirmCeder() {
        val immo = _state.value.immoToCeder ?: return
        val valeur = _state.value.cederValeurNette.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            _state.update { it.copy(isCeding = true, cederError = null) }
            when (val r = repository.cederImmobilisation(immo.id, valeur)) {
                is NetworkResult.Success -> {
                    val updated = _state.value.immobilisations.map { if (it.id == immo.id) r.data else it }
                    _state.update { it.copy(isCeding = false, showCederDialog = false, immoToCeder = null,
                        immobilisations = updated, plans = it.plans - immo.id) }
                }
                is NetworkResult.Error -> _state.update { it.copy(isCeding = false, cederError = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
