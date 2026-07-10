package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.AuthApiService
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Employee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployeeCreateUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val createdEmployeeId: String? = null,
    val errorMessage: String? = null,
    // Référentiels chargés depuis l'API
    val departments: List<DepartementDto> = emptyList(),
    val postes: List<PosteBriefDto> = emptyList(),
    val employees: List<Employee> = emptyList(),
    // Section 1 — Compte utilisateur
    val linkedUserEmail: String = "",
    val linkedUserId: String? = null,
    val linkedUserName: String? = null,
    val isVerifyingUser: Boolean = false,
    val userVerifyError: String? = null,
    // Section 2 — Identité
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "M",
    // Section 3 — Coordonnées
    val emailPro: String = "",
    val phone: String = "",
    // Section 4 — Poste & contrat
    val ville: String = "Douala",
    val selectedDepartmentId: String = "",
    val selectedDepartmentName: String = "",
    val selectedPosteId: String = "",
    val selectedPosteName: String = "",
    val selectedManagerId: String = "",
    val selectedManagerName: String = "— Aucun —",
    val contractType: String = "permanent",
    val hireDate: String = "",
    val salaireBase: String = "",
    // Validation
    val errors: Map<String, String> = emptyMap(),
)

class EmployeeCreateViewModel(
    private val hrRepository: HrRepository,
    private val authApiService: AuthApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(EmployeeCreateUiState())
    val state: StateFlow<EmployeeCreateUiState> = _state.asStateFlow()

    init {
        loadFormData()
    }

    private fun loadFormData() {
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
                _state.update { s ->
                    when (result) {
                        is NetworkResult.Success -> s.copy(employees = result.data, isLoading = false)
                        else -> s.copy(isLoading = false)
                    }
                }
            }
        }
    }

    // ── Section 1 — Compte utilisateur ───────────────────────────────────

    fun onLinkedUserEmailChange(email: String) {
        _state.update { it.copy(linkedUserEmail = email, linkedUserId = null, linkedUserName = null, userVerifyError = null) }
    }

    fun verifyUserAccount() {
        val email = _state.value.linkedUserEmail.trim()
        if (email.isBlank()) {
            _state.update { it.copy(userVerifyError = "Saisissez un email à vérifier.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isVerifyingUser = true, userVerifyError = null) }
            when (val result = authApiService.searchUserByEmail(email)) {
                is NetworkResult.Success -> {
                    val user = result.data.results.firstOrNull { it.email.equals(email, ignoreCase = true) }
                    if (user != null) {
                        val name = "${user.firstName} ${user.lastName}".trim().ifEmpty { user.email }
                        _state.update { it.copy(linkedUserId = user.id, linkedUserName = name, isVerifyingUser = false) }
                    } else {
                        _state.update { it.copy(linkedUserId = null, linkedUserName = null, userVerifyError = "Aucun compte trouvé pour cet email.", isVerifyingUser = false) }
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(userVerifyError = result.message ?: "Impossible de vérifier le compte.", isVerifyingUser = false) }
                else -> _state.update { it.copy(isVerifyingUser = false) }
            }
        }
    }

    // ── Section 2 — Identité ─────────────────────────────────────────────

    fun onFirstNameChange(v: String) { _state.update { it.copy(firstName = v, errors = it.errors - "firstName") } }
    fun onLastNameChange(v: String) { _state.update { it.copy(lastName = v, errors = it.errors - "lastName") } }
    fun onGenderChange(v: String) { _state.update { it.copy(gender = v) } }

    // ── Section 3 — Coordonnées ───────────────────────────────────────────

    fun onEmailProChange(v: String) { _state.update { it.copy(emailPro = v, errors = it.errors - "emailPro") } }
    fun onPhoneChange(v: String) { _state.update { it.copy(phone = v, errors = it.errors - "phone") } }

    // ── Section 4 — Poste & contrat ───────────────────────────────────────

    fun onVilleChange(v: String) { _state.update { it.copy(ville = v) } }

    fun onDepartmentSelect(id: String, name: String) {
        _state.update { it.copy(selectedDepartmentId = id, selectedDepartmentName = name) }
    }

    fun onPosteSelect(id: String, name: String) {
        _state.update { it.copy(selectedPosteId = id, selectedPosteName = name) }
    }

    fun onManagerSelect(id: String, name: String) {
        _state.update { it.copy(selectedManagerId = id, selectedManagerName = name.ifEmpty { "— Aucun —" }) }
    }

    fun onContractTypeChange(v: String) { _state.update { it.copy(contractType = v) } }

    fun onHireDateChange(v: String) { _state.update { it.copy(hireDate = v, errors = it.errors - "hireDate") } }

    fun onSalaireBaseChange(v: String) { _state.update { it.copy(salaireBase = v) } }

    // ── Soumission ────────────────────────────────────────────────────────

    fun submitForm() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = hrRepository.createEmployee(
                firstName = s.firstName.trim(),
                lastName = s.lastName.trim(),
                gender = s.gender,
                email = s.emailPro.trim(),
                phone = s.phone.trim(),
                hireDate = s.hireDate.trim(),
                status = "active",
                typeEmploye = s.contractType,
                departmentId = s.selectedDepartmentId.takeIf { it.isNotBlank() },
                positionId = s.selectedPosteId.takeIf { it.isNotBlank() },
                managerId = s.selectedManagerId.takeIf { it.isNotBlank() },
                userId = s.linkedUserId,
                salaireBase = s.salaireBase.toDoubleOrNull(),
                ville = s.ville.trim().takeIf { it.isNotBlank() },
            )
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(isSubmitting = false, isSuccess = true, createdEmployeeId = result.data.id) }
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
        if (s.emailPro.isBlank() || !s.emailPro.contains("@")) errors["emailPro"] = "E-mail invalide."
        if (s.phone.isBlank()) errors["phone"] = "Obligatoire."
        if (s.hireDate.isBlank()) errors["hireDate"] = "Obligatoire."
        _state.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }
}
