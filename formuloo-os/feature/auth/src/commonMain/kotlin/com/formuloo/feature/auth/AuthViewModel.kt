package com.formuloo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.auth.TokenRepository
import com.formuloo.core.auth.domain.model.AuthState
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authApiService: AuthApiService,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Loading)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow(tokenRepository.getUserProfile())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        if (tokenRepository.hasValidSession()) {
            _uiState.value = AuthState.LoggedIn
            refreshProfile()
        } else {
            _uiState.value = AuthState.LoggedOut
        }
    }

    /**
     * Rafraîchit le profil local (rôles/permissions/statut) depuis GET /me/ — le profil
     * persisté localement peut être obsolète si l'utilisateur a été modifié côté serveur
     * pendant que l'app était fermée. Ne bloque pas l'UI : LoggedIn reste affiché pendant
     * le chargement, sauf si le serveur confirme que la session n'est plus valide (401,
     * après que le plugin Ktor a déjà tenté un refresh).
     */
    private fun refreshProfile() {
        viewModelScope.launch {
            when (val result = authApiService.getProfile()) {
                is NetworkResult.Success -> {
                    val dto = result.data
                    val profile = UserProfile(
                        id = dto.id,
                        email = dto.email,
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        roles = dto.roles,
                        tenantId = dto.tenantId,
                        isActive = dto.isActive,
                        isVerified = dto.isVerified,
                    )
                    tokenRepository.saveUserProfile(profile)
                    _currentUser.value = profile
                }
                is NetworkResult.Error -> {
                    if (result.code == 401) {
                        tokenRepository.clear()
                        _currentUser.value = null
                        _uiState.value = AuthState.LoggedOut
                    }
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _fieldErrors.value = emptyMap()
            _uiState.value = AuthState.Loading
            when (val result = authApiService.login(email, password)) {
                is NetworkResult.Success -> {
                    val token = result.data
                    val refreshToken = token.refresh
                    if (refreshToken != null) {
                        tokenRepository.saveTokens(token.access, refreshToken)
                    }
                    token.user?.let { dto ->
                        val profile = UserProfile(
                            id = dto.id,
                            email = dto.email,
                            firstName = dto.firstName,
                            lastName = dto.lastName,
                            roles = dto.roles,
                            tenantId = dto.tenantId,
                            isActive = dto.isActive,
                            isVerified = dto.isVerified,
                        )
                        tokenRepository.saveUserProfile(profile)
                        _currentUser.value = profile
                    }
                    _uiState.value = AuthState.LoggedIn
                }
                is NetworkResult.Error -> {
                    val message = if (result.code == 401) {
                        "Email ou mot de passe incorrect"
                    } else {
                        result.message
                    }
                    _fieldErrors.value = result.fieldErrors.associate { it.field to it.message }
                    _uiState.value = AuthState.Error(message, result.code)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    /**
     * Nettoie l'état local de session immédiatement (pas d'attente réseau) pour que
     * la navigation déclenchée juste après par l'appelant ne retombe jamais sur un
     * état "connecté" obsolète. La révocation du refresh token côté serveur se fait
     * en arrière-plan, sans bloquer la déconnexion locale.
     */
    fun logout() {
        val refreshToken = tokenRepository.getRefreshToken()
        tokenRepository.clear()
        _currentUser.value = null
        _uiState.value = AuthState.LoggedOut
        if (refreshToken != null) {
            viewModelScope.launch {
                authApiService.logout(refreshToken)
            }
        }
    }
}
