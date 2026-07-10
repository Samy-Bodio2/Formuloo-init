package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class EmployeeListViewModel(private val repository: HrRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Employee>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Employee>>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    // Contrat actif par employé — chargé en parallèle après la liste pour afficher
    // le salaire sur chaque carte (pas d'endpoint liste qui joint déjà le contrat).
    // Acceptable à l'échelle d'une PME (quelques dizaines d'employés affichés).
    private val _activeContractByEmployee = MutableStateFlow<Map<String, Contract>>(emptyMap())
    val activeContractByEmployee: StateFlow<Map<String, Contract>> = _activeContractByEmployee.asStateFlow()

    private var currentPage = 1

    init {
        viewModelScope.launch {
            combine(_searchQuery.debounce(300), _selectedStatus) { query, status ->
                Pair(query.ifBlank { null }, status)
            }.collectLatest { (query, status) ->
                currentPage = 1
                loadEmployees(query, status)
            }
        }
    }

    private fun loadEmployees(search: String?, status: String?) {
        viewModelScope.launch {
            repository.getEmployees(search = search, status = status).collect { result ->
                _uiState.value = when (result) {
                    is NetworkResult.Loading -> UiState.Loading
                    is NetworkResult.Success -> {
                        loadActiveContracts(result.data)
                        if (result.data.isEmpty()) UiState.Empty
                        else UiState.Success(result.data)
                    }
                    is NetworkResult.Error -> UiState.Error(result.message, result.code)
                }
            }
        }
    }

    private fun loadActiveContracts(employees: List<Employee>) {
        viewModelScope.launch {
            val pairs = coroutineScope {
                employees.map { employee ->
                    async {
                        val result = repository.getContracts(employee.id).first { it !is NetworkResult.Loading }
                        val active = (result as? NetworkResult.Success)?.data?.firstOrNull { it.isActive }
                        employee.id to active
                    }
                }.awaitAll()
            }
            _activeContractByEmployee.value = pairs.mapNotNull { (id, contract) -> contract?.let { id to it } }.toMap()
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun filterByStatus(status: String?) {
        _selectedStatus.value = status
    }

    fun refresh() {
        loadEmployees(_searchQuery.value.ifBlank { null }, _selectedStatus.value)
    }

    fun loadMore() {
        currentPage++
        viewModelScope.launch {
            repository.getEmployees(
                search = _searchQuery.value.ifBlank { null },
                status = _selectedStatus.value,
            ).collectLatest { result ->
                if (result is NetworkResult.Success) {
                    val current = (_uiState.value as? UiState.Success)?.data ?: emptyList()
                    _uiState.value = UiState.Success(current + result.data)
                    loadActiveContracts(current + result.data)
                }
            }
        }
    }
}
