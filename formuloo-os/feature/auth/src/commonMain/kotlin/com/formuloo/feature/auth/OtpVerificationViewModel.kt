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

class OtpVerificationViewModel(private val authApiService: AuthApiService) : ViewModel() {

    private val _verifyState = MutableStateFlow<UiState<Unit>?>(null)
    val verifyState: StateFlow<UiState<Unit>?> = _verifyState.asStateFlow()

    private val _resendState = MutableStateFlow<UiState<Unit>?>(null)
    val resendState: StateFlow<UiState<Unit>?> = _resendState.asStateFlow()

    fun verify(code: String) {
        viewModelScope.launch {
            _verifyState.value = UiState.Loading
            _verifyState.value = when (val result = authApiService.verifyEmail(code)) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    fun resend(email: String) {
        viewModelScope.launch {
            _resendState.value = UiState.Loading
            _resendState.value = when (val result = authApiService.resendVerification(email)) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }
}
