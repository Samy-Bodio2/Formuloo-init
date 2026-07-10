package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GesDocValidationState(
    val documentId: String,
    val isLoading: Boolean = false,
    val ocrResult: OcrResult? = null,
    val documentNumber: String = "",
    val documentNumberConfidence: Int? = null,
    val date: String = "",
    val dateConfidence: Int? = null,
    val supplier: String = "",
    val supplierConfidence: Int? = null,
    val amountHt: String = "",
    val amountHtConfidence: Int? = null,
    val tvaRate: String = "",
    val tvaRateConfidence: Int? = null,
    val amountTtc: String = "",
    val amountTtcConfidence: Int? = null,
    val currency: String = "XAF",
    val isSaving: Boolean = false,
    val isValidated: Boolean = false,
    val error: String? = null,
)

class GesDocValidationViewModel(
    private val repo: GesDocRepository,
    private val documentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GesDocValidationState(documentId = documentId))
    val state: StateFlow<GesDocValidationState> = _state.asStateFlow()

    init {
        loadOcrResult()
    }

    private fun loadOcrResult() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getOcrResult(documentId)) {
                is NetworkResult.Success -> {
                    val fields = r.data.fields
                    _state.update {
                        it.copy(
                            isLoading = false,
                            ocrResult = r.data,
                            documentNumber = fields["document_number"]?.value.orEmpty(),
                            documentNumberConfidence = fields["document_number"]?.confidence,
                            date = fields["date"]?.value.orEmpty(),
                            dateConfidence = fields["date"]?.confidence,
                            supplier = fields["supplier"]?.value.orEmpty(),
                            supplierConfidence = fields["supplier"]?.confidence,
                            amountHt = fields["amount_ht"]?.value.orEmpty(),
                            amountHtConfidence = fields["amount_ht"]?.confidence,
                            tvaRate = fields["tva_rate"]?.value.orEmpty(),
                            tvaRateConfidence = fields["tva_rate"]?.confidence,
                            amountTtc = fields["amount_ttc"]?.value.orEmpty(),
                            amountTtcConfidence = fields["amount_ttc"]?.confidence,
                            currency = fields["currency"]?.value ?: "XAF",
                        )
                    }
                }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun onDocumentNumberChange(value: String) = _state.update { it.copy(documentNumber = value) }
    fun onDateChange(value: String) = _state.update { it.copy(date = value) }
    fun onSupplierChange(value: String) = _state.update { it.copy(supplier = value) }
    fun onAmountHtChange(value: String) = _state.update { it.copy(amountHt = value) }
    fun onTvaRateChange(value: String) = _state.update { it.copy(tvaRate = value) }
    fun onAmountTtcChange(value: String) = _state.update { it.copy(amountTtc = value) }

    fun validate() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = repo.validateOcr(
                id = documentId,
                documentNumber = current.documentNumber,
                date = normalizeDateForApi(current.date),
                supplier = current.supplier,
                amountHt = current.amountHt.toDoubleOrNull() ?: 0.0,
                tvaRate = current.tvaRate.toDoubleOrNull() ?: 0.0,
                amountTtc = current.amountTtc.toDoubleOrNull() ?: 0.0,
                currency = current.currency,
            )
            when (result) {
                is NetworkResult.Success -> _state.update { it.copy(isSaving = false, isValidated = true) }
                is NetworkResult.Error -> _state.update { it.copy(isSaving = false, error = result.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}

/**
 * L'OCR extrait la date au format DD/MM/YYYY (ou DD-MM-YYYY / DD.MM.YYYY),
 * mais le backend (DRF DateField) n'accepte que le format ISO YYYY-MM-DD.
 */
private val DMY_DATE_REGEX = Regex("""^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$""")
private val ISO_DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

private fun normalizeDateForApi(raw: String): String {
    val trimmed = raw.trim()
    if (ISO_DATE_REGEX.matches(trimmed)) return trimmed
    val match = DMY_DATE_REGEX.matchEntire(trimmed) ?: return trimmed
    val (day, month, year) = match.destructured
    return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
}
