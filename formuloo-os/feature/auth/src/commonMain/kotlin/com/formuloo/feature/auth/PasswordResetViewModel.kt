package com.formuloo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.core.network.dto.auth.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PasswordResetViewModel(private val authApiService: AuthApiService) : ViewModel() {

    private val _requestState = MutableStateFlow<UiState<Unit>?>(null)
    val requestState: StateFlow<UiState<Unit>?> = _requestState.asStateFlow()

    private val _confirmState = MutableStateFlow<UiState<Unit>?>(null)
    val confirmState: StateFlow<UiState<Unit>?> = _confirmState.asStateFlow()

    fun requestReset(email: String) {
        viewModelScope.launch {
            _requestState.value = UiState.Loading
            _requestState.value = when (val result = authApiService.forgotPassword(email)) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    fun confirmReset(code: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _confirmState.value = UiState.Loading
            _confirmState.value = when (
                val result = authApiService.resetPassword(code, password, confirmPassword)
            ) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
