package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.core.common.sync.NetworkObserver
import com.formuloo.feature.hr.data.mapper.toApiValue
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveTypeCode
import com.formuloo.feature.hr.domain.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaveFormState(
    val type: LeaveTypeCode = LeaveTypeCode.ANNUEL,
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val errors: Map<String, String> = emptyMap(),
)

class LeaveRequestViewModel(
    private val repository: HrRepository,
    private val networkObserver: NetworkObserver,
) : ViewModel() {

    private val _formState = MutableStateFlow(LeaveFormState())
    val formState: StateFlow<LeaveFormState> = _formState.asStateFlow()

    private val _balanceState = MutableStateFlow<UiState<List<LeaveBalance>>>(UiState.Loading)
    val balanceState: StateFlow<UiState<List<LeaveBalance>>> = _balanceState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<String>>(UiState.Empty)
    val submitState: StateFlow<UiState<String>> = _submitState.asStateFlow()

    init {
        loadBalance()
    }

    private fun loadBalance() {
        viewModelScope.launch {
            _balanceState.value = UiState.Loading
            _balanceState.value = when (val result = repository.getLeaveBalance()) {
                is NetworkResult.Success -> if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }

    fun updateType(type: LeaveTypeCode) {
        _formState.value = _formState.value.copy(type = type)
    }

    fun updateStartDate(date: String) {
        _formState.value = _formState.value.copy(startDate = date)
    }

    fun updateEndDate(date: String) {
        _formState.value = _formState.value.copy(endDate = date)
    }

    fun updateReason(reason: String) {
        _formState.value = _formState.value.copy(reason = reason)
    }

    fun calculateDays(): Int {
        val state = _formState.value
        if (state.startDate.isBlank() || state.endDate.isBlank()) return 0
        return DateUtils.businessDaysBetween(state.startDate, state.endDate)
    }

    /** Solde restant pour le type sélectionné, si disponible (null si pas encore chargé). */
    fun remainingBalanceForSelectedType(): Double? {
        val balances = (_balanceState.value as? UiState.Success)?.data ?: return null
        return balances.firstOrNull { it.typeConge == _formState.value.type.toApiValue() }?.joursRestants
    }

    fun submit() {
        val state = _formState.value
        val days = calculateDays()
        val errors = mutableMapOf<String, String>()

        if (state.startDate.isBlank()) errors["startDate"] = "La date de début est requise."
        if (state.endDate.isBlank()) errors["endDate"] = "La date de fin est requise."
        if (state.startDate.isNotBlank() && state.endDate.isNotBlank() && days <= 0) {
            errors["endDate"] = "La date de fin doit être postérieure ou égale à la date de début."
        }
        if (state.type == LeaveTypeCode.ANNUEL) {
            val remaining = remainingBalanceForSelectedType()
            if (remaining != null && days > remaining) {
                errors["balance"] = "Solde insuffisant : il vous reste ${remaining.toInt()} jour(s)."
            }
        }

        if (errors.isNotEmpty()) {
            _formState.value = state.copy(errors = errors)
            return
        }

        _formState.value = state.copy(errors = emptyMap())
        val wasOffline = !networkObserver.isOnline.value

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            val result = repository.requestLeave(
                typeCode = state.type.toApiValue(),
                startDate = state.startDate,
                endDate = state.endDate,
                reason = state.reason.ifBlank { null },
            )
            _submitState.value = when (result) {
                is NetworkResult.Success -> UiState.Success(
                    if (wasOffline) {
                        "Connexion indisponible — la demande sera envoyée automatiquement."
                    } else {
                        "Demande envoyée avec succès."
                    }
                )
                is NetworkResult.Error -> UiState.Error(result.message, result.code)
                else -> UiState.Loading
            }
        }
    }
}
