package com.formuloo.feature.admin.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.admin.data.mapper.MODULE_OPTIONS
import com.formuloo.feature.admin.data.repository.AdminRepository
import com.formuloo.feature.admin.domain.model.ModuleOption
import com.formuloo.feature.admin.domain.model.RoleOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InviteUserViewModel(private val repository: AdminRepository) : ViewModel() {

    private val _roles = MutableStateFlow<List<RoleOption>>(emptyList())
    val roles: StateFlow<List<RoleOption>> = _roles.asStateFlow()

    val moduleOptions: List<ModuleOption> = MODULE_OPTIONS

    private val _selectedRoleCode = MutableStateFlow<String?>(null)
    val selectedRoleCode: StateFlow<String?> = _selectedRoleCode.asStateFlow()

    private val _selectedModuleKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedModuleKeys: StateFlow<Set<String>> = _selectedModuleKeys.asStateFlow()

    private val _inviteState = MutableStateFlow<UiState<Unit>?>(null)
    val inviteState: StateFlow<UiState<Unit>?> = _inviteState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = repository.getRoles()) {
                is NetworkResult.Success -> _roles.value = result.data
                else -> Unit
            }
        }
    }

    fun selectRole(code: String) {
        _selectedRoleCode.value = code
        val role = _roles.value.firstOrNull { it.code == code }
        _selectedModuleKeys.value = role?.moduleKeys?.toSet() ?: emptySet()
    }

    fun toggleModule(key: String) {
        val current = _selectedModuleKeys.value
        _selectedModuleKeys.value = if (key in current) current - key else current + key
    }

    fun reset() {
        _selectedRoleCode.value = null
        _selectedModuleKeys.value = emptySet()
        _inviteState.value = null
    }

    /**
     * Le backend ne connaît que des rôles (pas de permissions par module accordées
     * indépendamment) — chaque module coché en plus du rôle principal ajoute le rôle
     * système qui couvre ce module à la liste envoyée à /invite/.
     */
    fun submit(firstName: String, lastName: String, email: String) {
        val primaryRole = _selectedRoleCode.value
        if (primaryRole == null) {
            _inviteState.value = UiState.Error("Sélectionnez un rôle.")
            return
        }
        val extraRoleCodes = _selectedModuleKeys.value
            .mapNotNull { moduleKey -> _roles.value.firstOrNull { moduleKey in it.moduleKeys }?.code }
        val roleCodes = (listOf(primaryRole) + extraRoleCodes).distinct()

        viewModelScope.launch {
            _inviteState.value = UiState.Loading
            _inviteState.value = when (
                val result = repository.inviteUser(firstName, lastName, email, roleCodes)
            ) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
