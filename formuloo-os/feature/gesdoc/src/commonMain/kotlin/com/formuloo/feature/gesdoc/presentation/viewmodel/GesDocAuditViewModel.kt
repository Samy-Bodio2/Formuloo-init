package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.AuditLogEntry
import com.formuloo.feature.gesdoc.util.CsvOpener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuditCategory(val label: String, val apiValue: String?) {
    ALL("Tout", null),
    SYSTEM("Système", "system"),
    USER("Utilisateurs", "user"),
    ALERTS("Alertes", "alerts"),
}

data class GesDocAuditState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isExporting: Boolean = false,
    val entries: List<AuditLogEntry> = emptyList(),
    val selectedCategory: AuditCategory = AuditCategory.ALL,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
    val exportError: String? = null,
)

class GesDocAuditViewModel(
    private val repo: GesDocRepository,
    private val csvOpener: CsvOpener,
) : ViewModel() {

    private val _state = MutableStateFlow(GesDocAuditState())
    val state: StateFlow<GesDocAuditState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun selectCategory(category: AuditCategory) {
        if (category == _state.value.selectedCategory) return
        _state.update { it.copy(selectedCategory = category) }
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val category = _state.value.selectedCategory.apiValue
            when (val r = repo.getAuditLog(category = category, cursor = null)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        isLoading = false,
                        entries = r.data.entries,
                        nextCursor = r.data.nextCursor,
                        hasMore = r.data.nextCursor != null,
                    )
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val category = current.selectedCategory.apiValue
            when (val r = repo.getAuditLog(category = category, cursor = current.nextCursor)) {
                is NetworkResult.Success -> _state.update {
                    it.copy(
                        isLoadingMore = false,
                        entries = it.entries + r.data.entries,
                        nextCursor = r.data.nextCursor,
                        hasMore = r.data.nextCursor != null,
                    )
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingMore = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun exportLog() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportError = null) }
            val category = _state.value.selectedCategory.apiValue
            when (val r = repo.exportAuditLog(category = category)) {
                is NetworkResult.Success -> {
                    try {
                        csvOpener.openCsv(r.data, "journal_audit.csv")
                    } catch (e: Exception) {
                        _state.update { it.copy(exportError = "Impossible d'ouvrir le fichier exporté.") }
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(exportError = r.message) }
                is NetworkResult.Loading -> Unit
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun clearExportError() {
        _state.update { it.copy(exportError = null) }
    }
}
