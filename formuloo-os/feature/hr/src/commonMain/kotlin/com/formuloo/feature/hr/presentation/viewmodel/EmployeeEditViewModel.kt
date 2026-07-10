package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Employee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployeeEditUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    // Référentiels
    val departments: List<DepartementDto> = emptyList(),
    val postes: List<PosteBriefDto> = emptyList(),
    val employees: List<Employee> = emptyList(),
    // Champs en lecture seule (non modifiables via PATCH)
    val employeeNumber: String = "",
    val emailPro: String = "",
    val hireDate: String = "",
    val gender: String = "",
    val typeEmploye: String = "",
    // Section 1 — Identité
    val firstName: String = "",
    val lastName: String = "",
    val situationFamiliale: String = "",
    val nombreEnfants: String = "",
    // Section 2 — Coordonnées
    val phone: String = "",
    val phonePerso: String = "",
    val emailPerso: String = "",
    val address: String = "",
    val ville: String = "",
    // Section 3 — Poste & rémunération
    val selectedDepartmentId: String = "",
    val selectedDepartmentName: String = "",
    val selectedPosteId: String = "",
    val selectedPosteName: String = "",
    val selectedManagerId: String = "",
    val selectedManagerName: String = "",
    val salaireBase: String = "",
    // Validation
    val errors: Map<String, String> = emptyMap(),
)

class EmployeeEditViewModel(
    private val hrRepository: HrRepository,
    private val employeeId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeEditUiState())
    val state: StateFlow<EmployeeEditUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            when (val r = hrRepository.getEmployee(employeeId)) {
                is NetworkResult.Success -> prefillFromEmployee(r.data)
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, errorMessage = r.message) }
                else -> {}
            }
        }
        viewModelScope.launch {
            when (val r = hrRepository.getDepartements()) {
                is NetworkResult.Success -> _state.update { it.copy(departments = r.data) }
                else -> {}
            }
        }
        viewModelScope.launch {
            when (val r = hrRepository.getPostes()) {
                is NetworkResult.Success -> _state.update { it.copy(postes = r.data) }
                else -> {}
            }
        }
        viewModelScope.launch {
            hrRepository.getEmployees().collect { result ->
                if (result is NetworkResult.Success) {
                    _state.update { it.copy(employees = result.data, isLoading = false) }
                }
            }
        }
    }

    private fun prefillFromEmployee(e: Employee) {
        _state.update {
            it.copy(
                isLoading = false,
                // Lecture seule
                employeeNumber = e.employeeNumber,
                emailPro = e.email,
                hireDate = e.hireDate,
                gender = e.gender.name,
                typeEmploye = e.employeeType.name,
                // Identité
                firstName = e.firstName,
                lastName = e.lastName,
                situationFamiliale = e.situationFamiliale ?: "",
                nombreEnfants = if (e.nombreEnfants > 0) e.nombreEnfants.toString() else "",
                // Coordonnées
                phone = e.phone,
                phonePerso = e.phonePerso ?: "",
                emailPerso = e.emailPerso ?: "",
                address = e.address ?: "",
                ville = e.ville ?: "",
                // Poste
                selectedDepartmentId = e.departmentId ?: "",
                selectedDepartmentName = e.department ?: "",
                selectedPosteId = e.positionId ?: "",
                selectedPosteName = e.position ?: "",
                selectedManagerId = e.managerId ?: "",
                selectedManagerName = e.managerName ?: "",
                // Rémunération
                salaireBase = e.salaireBase?.let { s -> s.toInt().toString() } ?: "",
            )
        }
    }

    // ── Section 1 ─────────────────────────────────────────────────────────
    fun onFirstNameChange(v: String) { _state.update { it.copy(firstName = v, errors = it.errors - "firstName") } }
    fun onLastNameChange(v: String) { _state.update { it.copy(lastName = v, errors = it.errors - "lastName") } }
    fun onSituationFamilialeChange(v: String) { _state.update { it.copy(situationFamiliale = v) } }
    fun onNombreEnfantsChange(v: String) { _state.update { it.copy(nombreEnfants = v.filter { c -> c.isDigit() }) } }

    // ── Section 2 ─────────────────────────────────────────────────────────
    fun onPhoneChange(v: String) { _state.update { it.copy(phone = v, errors = it.errors - "phone") } }
    fun onPhonePersoChange(v: String) { _state.update { it.copy(phonePerso = v) } }
    fun onEmailPersoChange(v: String) { _state.update { it.copy(emailPerso = v) } }
    fun onAddressChange(v: String) { _state.update { it.copy(address = v) } }
    fun onVilleChange(v: String) { _state.update { it.copy(ville = v) } }

    // ── Section 3 ─────────────────────────────────────────────────────────
    fun onDepartmentSelect(id: String, name: String) {
        _state.update { it.copy(selectedDepartmentId = id, selectedDepartmentName = name) }
    }
    fun onPosteSelect(id: String, name: String) {
        _state.update { it.copy(selectedPosteId = id, selectedPosteName = name) }
    }
    fun onManagerSelect(id: String, name: String) {
        _state.update { it.copy(selectedManagerId = id, selectedManagerName = name) }
    }
    fun onSalaireBaseChange(v: String) { _state.update { it.copy(salaireBase = v.filter { c -> c.isDigit() }) } }

    // ── Soumission ────────────────────────────────────────────────────────
    fun submitForm() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = hrRepository.updateEmployee(
                id = employeeId,
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                phone = s.phone.trim().takeIf { it.isNotBlank() },
                phonePerso = s.phonePerso.trim().takeIf { it.isNotBlank() },
                emailPerso = s.emailPerso.trim().takeIf { it.isNotBlank() },
                address = s.address.trim().takeIf { it.isNotBlank() },
                ville = s.ville.trim().takeIf { it.isNotBlank() },
                situationFamiliale = s.situationFamiliale.takeIf { it.isNotBlank() },
                nombreEnfants = s.nombreEnfants.toIntOrNull(),
                departmentId = s.selectedDepartmentId.takeIf { it.isNotBlank() },
                positionId = s.selectedPosteId.takeIf { it.isNotBlank() },
                managerId = s.selectedManagerId.takeIf { it.isNotBlank() },
                salaireBase = s.salaireBase.toDoubleOrNull(),
            )
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(isSubmitting = false, isSuccess = true) }
                is NetworkResult.Error -> {
                    val fieldErrors = result.fieldErrors.associate { it.field to it.message }
                    _state.update { it.copy(isSubmitting = false, errorMessage = result.message, errors = it.errors + fieldErrors) }
                }
                else -> _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        if (s.firstName.isBlank()) errors["firstName"] = "Obligatoire."
        if (s.lastName.isBlank()) errors["lastName"] = "Obligatoire."
        if (s.phone.isBlank()) errors["phone"] = "Obligatoire."
        _state.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }
}
