package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.OrgNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrgChartViewModel(private val repository: HrRepository) : ViewModel() {

    private val _treeState = MutableStateFlow<UiState<List<OrgNode>>>(UiState.Loading)
    val treeState: StateFlow<UiState<List<OrgNode>>> = _treeState.asStateFlow()

    private val _expandedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedNodeIds: StateFlow<Set<String>> = _expandedNodeIds.asStateFlow()

    init {
        loadTree()
    }

    fun loadTree() {
        viewModelScope.launch {
            _treeState.value = UiState.Loading
            _treeState.value = when (val result = repository.getOrganizationTree()) {
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) {
                        UiState.Empty
                    } else {
                        // Racines développées par défaut pour un premier affichage utile.
                        _expandedNodeIds.value = result.data.map { it.id }.toSet()
                        UiState.Success(result.data)
                    }
                }
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    fun toggleNode(id: String) {
        val current = _expandedNodeIds.value
        _expandedNodeIds.value = if (id in current) current - id else current + id
    }
}
