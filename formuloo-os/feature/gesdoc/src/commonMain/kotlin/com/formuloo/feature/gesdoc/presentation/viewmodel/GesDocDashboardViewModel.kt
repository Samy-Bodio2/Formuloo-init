package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentSummary
import com.formuloo.feature.gesdoc.domain.model.GesDocStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GesDocDashboardState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: GesDocStats = GesDocStats(),
    val documents: List<DocumentSummary> = emptyList(),
    val selectedFilter: Int = 0, // 0=Tous, 1=En cours, 2=Certifiés, 3=Alertes
    val searchQuery: String = "",
)

class GesDocDashboardViewModel(private val repo: GesDocRepository) : ViewModel() {

    private val _state = MutableStateFlow(GesDocDashboardState())
    val state: StateFlow<GesDocDashboardState> = _state.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getDocuments()) {
                is NetworkResult.Success -> {
                    val (stats, docs) = r.data
                    _state.update { it.copy(isLoading = false, stats = stats, documents = docs) }
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isLoading = false, error = r.message) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun selectFilter(index: Int) {
        _state.update { it.copy(selectedFilter = index) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    /**
     * Filtre les documents à partir de l'état observé par le composable
     * (via collectAsStateWithLifecycle), plutôt que de lire `_state.value`
     * directement — sinon ce calcul se fige à la toute première composition
     * (avant même le premier chargement réseau) car rien dans la portée de
     * recomposition qui l'entoure ne lit `state` directement (seuls des
     * `item { }` imbriqués du LazyColumn le font, et ce sont des portées de
     * recomposition distinctes).
     */
    fun filterDocuments(state: GesDocDashboardState): List<DocumentSummary> {
        var list = state.documents
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter {
                it.supplier?.lowercase()?.contains(q) == true ||
                    it.number?.lowercase()?.contains(q) == true
            }
        }
        return when (state.selectedFilter) {
            1 -> list.filter { it.status in IN_PROCESSING }
            2 -> list.filter { it.status == DocumentStatus.CERTIFIED }
            3 -> list.filter { it.status == DocumentStatus.TAMPERED }
            else -> list
        }
    }

    private companion object {
        val IN_PROCESSING = setOf(
            DocumentStatus.PENDING_OCR,
            DocumentStatus.PREPROCESSING,
            DocumentStatus.EXTRACTING,
            DocumentStatus.ANALYZING,
            DocumentStatus.EXTRACTED,
            DocumentStatus.VALIDATED,
            DocumentStatus.PENDING_CHAIN,
        )
    }
}
