package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExtractionStepState { DONE, IN_PROGRESS, PENDING }

data class ExtractionStepItem(
    val label: String,
    val state: ExtractionStepState,
)

data class GesDocExtractionState(
    val documentId: String,
    val currentStatus: DocumentStatus = DocumentStatus.PENDING_OCR,
    val steps: List<ExtractionStepItem> = DocumentStatus.PENDING_OCR.toExtractionSteps(),
    val isComplete: Boolean = false,
    val error: String? = null,
)

private val TERMINAL_STATUSES = setOf(
    DocumentStatus.EXTRACTED,
    DocumentStatus.VALIDATED,
    DocumentStatus.PENDING_CHAIN,
    DocumentStatus.CERTIFIED,
    DocumentStatus.TAMPERED,
)

private fun DocumentStatus.toExtractionSteps(): List<ExtractionStepItem> {
    val step1 = when (this) {
        DocumentStatus.PENDING_OCR, DocumentStatus.PREPROCESSING -> ExtractionStepState.IN_PROGRESS
        else -> ExtractionStepState.DONE
    }
    val step2 = when (this) {
        DocumentStatus.PENDING_OCR, DocumentStatus.PREPROCESSING -> ExtractionStepState.PENDING
        DocumentStatus.EXTRACTING -> ExtractionStepState.IN_PROGRESS
        else -> ExtractionStepState.DONE
    }
    val step3 = when (this) {
        DocumentStatus.PENDING_OCR, DocumentStatus.PREPROCESSING, DocumentStatus.EXTRACTING -> ExtractionStepState.PENDING
        DocumentStatus.ANALYZING -> ExtractionStepState.IN_PROGRESS
        else -> ExtractionStepState.DONE
    }
    return listOf(
        ExtractionStepItem("Préparation de l'image", step1),
        ExtractionStepItem("Extraction du texte", step2),
        ExtractionStepItem("Analyse de secours", step3),
    )
}

class GesDocExtractionViewModel(
    private val repo: GesDocRepository,
    private val documentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GesDocExtractionState(documentId = documentId))
    val state: StateFlow<GesDocExtractionState> = _state.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                delay(2000L)
                when (val result = repo.getOcrStatus(documentId)) {
                    is NetworkResult.Success -> {
                        val status = result.data.status
                        val isComplete = status in TERMINAL_STATUSES
                        val isFailed = status == DocumentStatus.FAILED
                        _state.update {
                            it.copy(
                                currentStatus = status,
                                steps = status.toExtractionSteps(),
                                isComplete = isComplete,
                                error = when {
                                    isFailed -> "L'extraction OCR a échoué. Vérifiez la lisibilité du document."
                                    else -> result.data.error
                                },
                            )
                        }
                        if (isComplete || isFailed) break
                    }
                    is NetworkResult.Error -> Unit
                    is NetworkResult.Loading -> Unit
                }
            }
        }
    }
}
