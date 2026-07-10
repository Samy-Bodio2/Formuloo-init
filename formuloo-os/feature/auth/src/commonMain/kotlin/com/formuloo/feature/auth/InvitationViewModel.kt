package com.formuloo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.auth.TokenRepository
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.core.network.dto.auth.AuthApiService
import com.formuloo.core.network.dto.auth.InvitePreviewDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvitationViewModel(
    private val authApiService: AuthApiService,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _previewState = MutableStateFlow<UiState<InvitePreviewDto>>(UiState.Loading)
    val previewState: StateFlow<UiState<InvitePreviewDto>> = _previewState.asStateFlow()

    private val _acceptState = MutableStateFlow<UiState<Unit>?>(null)
    val acceptState: StateFlow<UiState<Unit>?> = _acceptState.asStateFlow()

    fun loadPreview(code: String) {
        viewModelScope.launch {
            _previewState.value = UiState.Loading
            _previewState.value = when (val result = authApiService.getInvitePreview(code)) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    /** Accepte l'invitation puis enchaîne une connexion automatique pour obtenir les JWT. */
    fun accept(code: String, email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _acceptState.value = UiState.Loading
            when (val acceptResult = authApiService.acceptInvitation(code, password, confirmPassword)) {
                is NetworkResult.Error -> {
                    _acceptState.value = UiState.Error(acceptResult.message, acceptResult.code)
                    return@launch
                }
                is NetworkResult.Loading -> return@launch
                is NetworkResult.Success -> Unit
            }

            when (val loginResult = authApiService.login(email, password)) {
                is NetworkResult.Success -> {
                    val tokenResponse = loginResult.data
                    val refreshToken = tokenResponse.refresh
                    if (refreshToken != null) {
                        tokenRepository.saveTokens(tokenResponse.access, refreshToken)
                    }
                    tokenResponse.user?.let { dto ->
                        tokenRepository.saveUserProfile(
                            UserProfile(
                                id = dto.id,
                                email = dto.email,
                                firstName = dto.firstName,
                                lastName = dto.lastName,
                                roles = dto.roles,
                                tenantId = dto.tenantId,
                                isActive = dto.isActive,
                                isVerified = dto.isVerified,
                            ),
                        )
                    }
                    _acceptState.value = UiState.Success(Unit)
                }
                is NetworkResult.Error -> {
                    // Compte activé mais connexion auto échouée — l'utilisateur peut se reconnecter manuellement.
                    _acceptState.value = UiState.Error(loginResult.message, loginResult.code)
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
