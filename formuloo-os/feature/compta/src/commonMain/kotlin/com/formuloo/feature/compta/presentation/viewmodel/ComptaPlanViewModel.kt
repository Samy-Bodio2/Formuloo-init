package com.formuloo.feature.compta.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.CompteDto
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.domain.model.ClasseGroup
import com.formuloo.feature.compta.domain.model.CompteItemUi
import com.formuloo.feature.compta.domain.model.PlanComptableStats
import com.formuloo.feature.compta.domain.model.SousGroupeGroup
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanComptableUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val isInitialising: Boolean = false,
    val initialiseError: String? = null,
    val stats: PlanComptableStats? = null,
    val allClasses: List<ClasseGroup> = emptyList(),
    val filteredClasses: List<ClasseGroup> = emptyList(),
    val expandedClasses: Set<Int> = emptySet(),
    val expandedSousGroupes: Set<String> = emptySet(),
    val searchQuery: String = "",
)

class ComptaPlanViewModel(private val repository: ComptaRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlanComptableUiState())
    val state: StateFlow<PlanComptableUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val statsResult = repository.getStats()
            if (statsResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, error = statsResult.message) }
                return@launch
            }
            val stats = (statsResult as NetworkResult.Success).data
            val exerciceId = stats.exerciceId

            val comptesDeferred = async { repository.getAllComptes() }
            val balanceDeferred = async {
                if (exerciceId != null) repository.getBalance(exerciceId) else null
            }

            val comptesResult = comptesDeferred.await()
            val balanceResult = balanceDeferred.await()

            if (comptesResult is NetworkResult.Error) {
                _state.update { it.copy(isLoading = false, error = comptesResult.message) }
                return@launch
            }

            val comptes = (comptesResult as NetworkResult.Success).data

            if (comptes.isEmpty()) {
                _state.update { it.copy(isLoading = false, isEmpty = true) }
                return@launch
            }

            // Indexed by compte_numero → Pair(soldeDebiteur, soldeCrediteur)
            val balanceMap: Map<String, Pair<Double, Double>> = if (balanceResult is NetworkResult.Success) {
                balanceResult.data.lignes.associate { ligne ->
                    ligne.compteNumero to Pair(
                        ligne.soldeDebiteur.toDoubleOrNull() ?: 0.0,
                        ligne.soldeCrediteur.toDoubleOrNull() ?: 0.0,
                    )
                }
            } else {
                emptyMap()
            }

            val totalActif = comptes
                .filter { it.typeCompte == "ACTIF" && it.isActif }
                .sumOf { balanceMap[it.numero]?.first ?: 0.0 }

            val planStats = PlanComptableStats(
                nbComptesActifs = comptes.count { it.isActif },
                totalActif = totalActif,
                tresorerieNette = stats.soldeTresorerie,
                dateAt = stats.date,
                exerciceAnnee = stats.exerciceAnnee,
                devise = stats.devise,
            )

            val classes = buildHierarchy(comptes, balanceMap)

            _state.update {
                it.copy(
                    isLoading = false,
                    stats = planStats,
                    allClasses = classes,
                    filteredClasses = applySearch(classes, it.searchQuery),
                )
            }
        }
    }

    fun initialiserPlan() {
        viewModelScope.launch {
            _state.update { it.copy(isInitialising = true, initialiseError = null) }
            when (val result = repository.initialiserPlan()) {
                is NetworkResult.Success -> {
                    // Plan créé — recharger la liste
                    _state.update { it.copy(isInitialising = false, isEmpty = false) }
                    load()
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isInitialising = false, initialiseError = result.message) }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { s ->
            s.copy(
                searchQuery = query,
                filteredClasses = applySearch(s.allClasses, query),
                expandedClasses = if (query.isNotBlank()) s.allClasses.map { it.numero }.toSet() else s.expandedClasses,
                expandedSousGroupes = if (query.isNotBlank()) {
                    s.allClasses.flatMap { cl -> cl.sousGroupes.map { sg -> sg.prefixe } }.toSet()
                } else {
                    s.expandedSousGroupes
                },
            )
        }
    }

    fun toggleClasse(numero: Int) {
        _state.update { s ->
            val expanded = if (s.expandedClasses.contains(numero)) {
                s.expandedClasses - numero
            } else {
                s.expandedClasses + numero
            }
            s.copy(expandedClasses = expanded)
        }
    }

    fun toggleSousGroupe(prefixe: String) {
        _state.update { s ->
            val expanded = if (s.expandedSousGroupes.contains(prefixe)) {
                s.expandedSousGroupes - prefixe
            } else {
                s.expandedSousGroupes + prefixe
            }
            s.copy(expandedSousGroupes = expanded)
        }
    }

    private fun buildHierarchy(
        comptes: List<CompteDto>,
        balanceMap: Map<String, Pair<Double, Double>>,
    ): List<ClasseGroup> {
        val classeNoms = mapOf(
            1 to "Comptes de ressources durables",
            2 to "Comptes d'actif immobilisé",
            3 to "Stocks",
            4 to "Comptes de tiers",
            5 to "Comptes de trésorerie",
            6 to "Comptes de charges",
            7 to "Comptes de produits",
            8 to "Comptes des opérations hors activités ordinaires",
        )

        return comptes
            .groupBy { it.classe }
            .entries
            .sortedBy { it.key }
            .map { (classeNum, comptesInClasse) ->
                val sousGroupes = buildSousGroupes(comptesInClasse, balanceMap)
                val classeTotal = sousGroupes.sumOf { kotlin.math.abs(it.total) }
                ClasseGroup(
                    numero = classeNum,
                    libelle = classeNoms[classeNum] ?: "Classe $classeNum",
                    total = classeTotal,
                    sousGroupes = sousGroupes,
                )
            }
    }

    private fun buildSousGroupes(
        comptes: List<CompteDto>,
        balanceMap: Map<String, Pair<Double, Double>>,
    ): List<SousGroupeGroup> {
        val twoDigitComptes = comptes.filter { it.numero.length == 2 }
        val detailComptes = comptes.filter { it.numero.length >= 3 }

        val prefixSet = (twoDigitComptes.map { it.numero } + detailComptes.map { it.numero.take(2) }).toSet()

        return prefixSet.sorted().map { prefixe ->
            val header = twoDigitComptes.firstOrNull { it.numero == prefixe }
            val children = detailComptes
                .filter { it.numero.startsWith(prefixe) }
                .sortedBy { it.numero }
                .map { c ->
                    val (sd, sc) = balanceMap[c.numero] ?: Pair(0.0, 0.0)
                    CompteItemUi(
                        id = c.id,
                        numero = c.numero,
                        libelle = c.libelle,
                        soldeDebiteur = sd,
                        soldeCrediteur = sc,
                        isActif = c.isActif,
                    )
                }
            // Solde net algébrique du sous-groupe (D - C)
            val total = children.sumOf { it.soldeNet }
            SousGroupeGroup(
                prefixe = prefixe,
                libelle = header?.libelle ?: "Groupe $prefixe",
                total = total,
                comptes = children,
            )
        }
    }

    private fun applySearch(classes: List<ClasseGroup>, query: String): List<ClasseGroup> {
        if (query.isBlank()) return classes
        val q = query.lowercase()
        return classes.mapNotNull { cl ->
            val filteredSg = cl.sousGroupes.mapNotNull { sg ->
                val filteredComptes = sg.comptes.filter { c ->
                    c.numero.contains(q) || c.libelle.lowercase().contains(q)
                }
                val sgMatches = sg.prefixe.contains(q) || sg.libelle.lowercase().contains(q)
                when {
                    sgMatches -> sg
                    filteredComptes.isNotEmpty() -> sg.copy(comptes = filteredComptes)
                    else -> null
                }
            }
            val clMatches = cl.numero.toString().contains(q) || cl.libelle.lowercase().contains(q)
            when {
                clMatches -> cl
                filteredSg.isNotEmpty() -> cl.copy(sousGroupes = filteredSg)
                else -> null
            }
        }
    }
}
