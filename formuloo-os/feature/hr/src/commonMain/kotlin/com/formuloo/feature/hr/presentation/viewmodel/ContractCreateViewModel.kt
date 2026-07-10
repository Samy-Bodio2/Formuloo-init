package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.hr.data.repository.HrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContractCreateUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val contractType: String = "CDI",
    val startDate: String = "",
    val endDate: String = "",
    val grossSalary: String = "",
    val workHoursPerWeek: String = "40",
    val trialPeriod: String = "",
    val documentUrl: String = "",
    val errors: Map<String, String> = emptyMap(),
)

class ContractCreateViewModel(
    private val hrRepository: HrRepository,
    private val employeeId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ContractCreateUiState())
    val state: StateFlow<ContractCreateUiState> = _state.asStateFlow()

    fun onContractTypeChange(type: String) {
        _state.update { it.copy(contractType = type, errors = it.errors - "endDate") }
    }

    fun onStartDateChange(v: String) {
        _state.update { it.copy(startDate = v, errors = it.errors - "startDate") }
    }

    fun onEndDateChange(v: String) {
        _state.update { it.copy(endDate = v, errors = it.errors - "endDate") }
    }

    fun onGrossSalaryChange(v: String) {
        _state.update { it.copy(grossSalary = v.filter { c -> c.isDigit() }, errors = it.errors - "grossSalary") }
    }

    fun onWorkHoursChange(v: String) {
        _state.update { it.copy(workHoursPerWeek = v.filter { c -> c.isDigit() }) }
    }

    fun onTrialPeriodChange(v: String) {
        _state.update { it.copy(trialPeriod = v.filter { c -> c.isDigit() }) }
    }

    fun onDocumentUrlChange(v: String) {
        _state.update { it.copy(documentUrl = v) }
    }

    private val endDateRequired get() = _state.value.contractType in listOf("CDD", "Stage")

    fun submitForm() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = hrRepository.createContract(
                employeId = employeeId,
                type = s.contractType,
                startDate = s.startDate.trim(),
                endDate = s.endDate.trim().takeIf { it.isNotBlank() },
                grossSalary = s.grossSalary.toDoubleOrNull() ?: 0.0,
                currency = "XAF",
                workHoursPerWeek = s.workHoursPerWeek.toIntOrNull() ?: 40,
                trialPeriod = s.trialPeriod.toIntOrNull(),
                documentUrl = s.documentUrl.trim().takeIf { it.isNotBlank() },
                signedAt = null,
            )
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(isSubmitting = false, isSuccess = true) }
                is NetworkResult.Error -> {
                    val fieldErrors = result.fieldErrors.associate { it.field to it.message }
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message,
                            errors = it.errors + fieldErrors,
                        )
                    }
                }
                else -> _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        val errors = mutableMapOf<String, String>()
        if (s.startDate.isBlank()) errors["startDate"] = "Date de début obligatoire."
        if (s.grossSalary.isBlank()) errors["grossSalary"] = "Salaire brut obligatoire."
        else if ((s.grossSalary.toDoubleOrNull() ?: 0.0) < 36270.0) {
            errors["grossSalary"] = "Inférieur au SMIG (36 270 XAF)."
        }
        if (endDateRequired && s.endDate.isBlank()) {
            errors["endDate"] = "Date de fin obligatoire pour un ${s.contractType}."
        }
        _state.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }
}
