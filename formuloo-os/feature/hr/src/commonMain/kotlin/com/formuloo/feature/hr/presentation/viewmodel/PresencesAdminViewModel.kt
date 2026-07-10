package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.Presence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PresenceCreateForm(
    val employeeId: String = "",
    val employeeName: String = "",
    val date: String = "",
    val heureArrivee: String = "",
    val heureDepart: String = "",
    val statut: String = "present",
    val commentaire: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PresencesAdminViewModel(private val repository: HrRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Presence>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Presence>>> = _state.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _statutFilter = MutableStateFlow<String?>(null)
    val statutFilter: StateFlow<String?> = _statutFilter.asStateFlow()

    private val _dateDebut = MutableStateFlow("")
    val dateDebut: StateFlow<String> = _dateDebut.asStateFlow()

    private val _dateFin = MutableStateFlow("")
    val dateFin: StateFlow<String> = _dateFin.asStateFlow()

    private val _createForm = MutableStateFlow<PresenceCreateForm?>(null)
    val createForm: StateFlow<PresenceCreateForm?> = _createForm.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        loadEmployees()
        load()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            repository.getEmployees().collect { result ->
                if (result is NetworkResult.Success) _employees.value = result.data
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repository.getPresences(
                statut = _statutFilter.value,
                dateDebut = _dateDebut.value.takeIf { it.isNotBlank() },
                dateFin = _dateFin.value.takeIf { it.isNotBlank() },
            )) {
                is NetworkResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.date }
                    _state.value = if (sorted.isEmpty()) UiState.Empty else UiState.Success(sorted)
                }
                is NetworkResult.Error -> _state.value = UiState.Error(result.message, result.code)
                else -> Unit
            }
        }
    }

    fun setStatutFilter(statut: String?) {
        _statutFilter.value = statut
        load()
    }

    fun setDateDebut(v: String) { _dateDebut.value = v }
    fun setDateFin(v: String) { _dateFin.value = v }
    fun applyDateFilter() = load()

    fun openCreateForm(today: String = "") {
        _createForm.value = PresenceCreateForm(date = today)
    }

    fun closeCreateForm() { _createForm.value = null }
    fun clearActionError() { _actionError.value = null }

    fun updateForm(update: (PresenceCreateForm) -> PresenceCreateForm) {
        _createForm.value = _createForm.value?.let(update)
    }

    fun submitCreate() {
        val form = _createForm.value ?: return
        if (form.employeeId.isBlank()) {
            _createForm.value = form.copy(error = "Sélectionnez un employé.")
            return
        }
        if (form.date.isBlank()) {
            _createForm.value = form.copy(error = "La date est obligatoire.")
            return
        }
        viewModelScope.launch {
            _createForm.value = form.copy(isLoading = true, error = null)
            when (val result = repository.createPresence(
                employeId = form.employeeId,
                date = form.date,
                heureArrivee = form.heureArrivee.takeIf { it.isNotBlank() },
                heureDepart = form.heureDepart.takeIf { it.isNotBlank() },
                statut = form.statut,
                commentaire = form.commentaire.takeIf { it.isNotBlank() },
            )) {
                is NetworkResult.Success -> {
                    _createForm.value = null
                    load()
                }
                is NetworkResult.Error -> {
                    _createForm.value = form.copy(isLoading = false, error = result.message)
                }
                else -> _createForm.value = form.copy(isLoading = false)
            }
        }
    }

    fun archivePresence(id: String) {
        viewModelScope.launch {
            when (val result = repository.archivePresence(id)) {
                is NetworkResult.Success -> load()
                is NetworkResult.Error -> _actionError.value = result.message
                else -> Unit
            }
        }
    }
}
