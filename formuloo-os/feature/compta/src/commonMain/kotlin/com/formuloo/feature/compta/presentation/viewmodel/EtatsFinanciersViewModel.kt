package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.BilanDto
import com.formuloo.core.network.dto.compta.CompteResultatDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.CompteItemUi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EtatType { BALANCE, BILAN, RESULTAT, GRAND_LIVRE }

data class EtatsFinanciersUiState(
    val isLoadingInit: Boolean = true,
    val initError: String? = null,
    val exercices: List<ExerciceDto> = emptyList(),
    val selectedExerciceId: Int? = null,
    val selectedEtat: EtatType = EtatType.BALANCE,
    // Balance
    val balance: com.formuloo.core.network.dto.compta.BalanceDto? = null,
    val isLoadingBalance: Boolean = false,
    val balanceError: String? = null,
    // Bilan
    val bilan: BilanDto? = null,
    val isLoadingBilan: Boolean = false,
    val bilanError: String? = null,
    // Compte de résultat
    val compteResultat: CompteResultatDto? = null,
    val isLoadingResultat: Boolean = false,
    val resultatError: String? = null,
    // Grand Livre
    val glCompteNumero: String = "",
    val glCompte: GrandLivreCompteDto? = null,
    val isLoadingGL: Boolean = false,
    val glError: String? = null,
    val flatComptes: List<CompteItemUi> = emptyList(),
) {
    val selectedExercice: ExerciceDto? get() = exercices.firstOrNull { it.id == selectedExerciceId }
}

class EtatsFinanciersViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(EtatsFinanciersUiState())
    val state: StateFlow<EtatsFinanciersUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInit = true, initError = null) }

            val exercicesDeferred = async { repository.getExercices() }
            val comptesDeferred = async { repository.getAllComptes() }

            val exercicesResult = exercicesDeferred.await()
            if (exercicesResult is NetworkResult.Error) {
                _state.update { it.copy(isLoadingInit = false, initError = exercicesResult.message) }
                return@launch
            }

            val exercices = (exercicesResult as NetworkResult.Success).data
            val defaultId = exercices.firstOrNull { it.statut == "OUVERT" }?.id
                ?: exercices.firstOrNull()?.id

            val flatComptes = when (val r = comptesDeferred.await()) {
                is NetworkResult.Success -> r.data.map { c ->
                    CompteItemUi(
                        id = c.id, numero = c.numero, libelle = c.libelle,
                        soldeDebiteur = 0.0, soldeCrediteur = 0.0, isActif = c.isActif,
                    )
                }
                else -> emptyList()
            }

            _state.update {
                it.copy(
                    isLoadingInit = false,
                    exercices = exercices,
                    selectedExerciceId = it.selectedExerciceId ?: defaultId,
                    flatComptes = flatComptes,
                )
            }

            if (defaultId != null) loadCurrentEtat()
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun selectEtat(type: EtatType) {
        _state.update { it.copy(selectedEtat = type) }
        loadCurrentEtat()
    }

    fun selectExercice(id: Int) {
        _state.update { it.copy(selectedExerciceId = id, balance = null, bilan = null, compteResultat = null, glCompte = null) }
        loadCurrentEtat()
    }

    private fun loadCurrentEtat() {
        when (_state.value.selectedEtat) {
            EtatType.BALANCE -> loadBalance()
            EtatType.BILAN -> loadBilan()
            EtatType.RESULTAT -> loadResultat()
            EtatType.GRAND_LIVRE -> Unit // lazy: user triggers via compte search
        }
    }

    // ── Balance ───────────────────────────────────────────────────────────────

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

    // ── Bilan ─────────────────────────────────────────────────────────────────

    fun loadBilan() {
        val exerciceId = _state.value.selectedExerciceId ?: return
        if (_state.value.isLoadingBilan) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBilan = true, bilanError = null) }
            when (val r = repository.getBilan(exerciceId)) {
                is NetworkResult.Success -> _state.update { it.copy(bilan = r.data, isLoadingBilan = false) }
                is NetworkResult.Error -> _state.update { it.copy(bilanError = r.message, isLoadingBilan = false) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Compte de résultat ────────────────────────────────────────────────────

    fun loadResultat() {
        val exerciceId = _state.value.selectedExerciceId ?: return
        if (_state.value.isLoadingResultat) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingResultat = true, resultatError = null) }
            when (val r = repository.getCompteResultat(exerciceId)) {
                is NetworkResult.Success -> _state.update { it.copy(compteResultat = r.data, isLoadingResultat = false) }
                is NetworkResult.Error -> _state.update { it.copy(resultatError = r.message, isLoadingResultat = false) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // ── Grand Livre ───────────────────────────────────────────────────────────

    fun setGLCompteNumero(numero: String) {
        _state.update { it.copy(glCompteNumero = numero, glCompte = null, glError = null) }
        if (numero.length >= 3) {
            val compte = _state.value.flatComptes.find { it.numero == numero && it.isActif }
            if (compte != null) loadGrandLivre(compte.id)
        }
    }

    private fun loadGrandLivre(compteId: Int) {
        val exerciceId = _state.value.selectedExerciceId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingGL = true, glError = null, glCompte = null) }
            when (val r = repository.getGrandLivre(exerciceId, compteId)) {
                is NetworkResult.Success -> _state.update { it.copy(glCompte = r.data, isLoadingGL = false) }
                is NetworkResult.Error -> _state.update { it.copy(glError = r.message, isLoadingGL = false) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
