package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.AccountingPrefill
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentFullDetail
import com.formuloo.feature.gesdoc.domain.model.IntegrityResult
import com.formuloo.feature.gesdoc.domain.model.OcrResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentDetailState(
    val id: String,
    val isLoadingOcr: Boolean = false,
    val isLoadingBlockchain: Boolean = false,
    val ocrResult: OcrResult? = null,
    val blockchainStatus: BlockchainStatus? = null,
    val documentDetail: DocumentFullDetail? = null,
    val prefill: AccountingPrefill? = null,
    val integrityResult: IntegrityResult? = null,
    val isVerifyingIntegrity: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null,
)

class DocumentDetailViewModel(
    private val repo: GesDocRepository,
    private val documentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentDetailState(id = documentId))
    val state: StateFlow<DocumentDetailState> = _state.asStateFlow()

    init {
        loadOcrResult()
        loadBlockchainStatus()
        loadDocumentDetail()
    }

    fun loadDocumentDetail() {
        viewModelScope.launch {
            when (val r = repo.getDocumentDetail(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(documentDetail = r.data) }
                is NetworkResult.Error -> Unit
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadOcrResult() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingOcr = true, error = null) }
            when (val r = repo.getOcrResult(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoadingOcr = false, ocrResult = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingOcr = false, error = r.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadBlockchainStatus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingBlockchain = true) }
            when (val r = repo.getBlockchainStatus(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoadingBlockchain = false, blockchainStatus = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoadingBlockchain = false) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun loadPrefill() {
        viewModelScope.launch {
            when (val r = repo.getAccountingPrefill(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(prefill = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(error = r.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun verifyIntegrity() {
        viewModelScope.launch {
            _state.update { it.copy(isVerifyingIntegrity = true, error = null) }
            when (val r = repo.verifyIntegrity(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(isVerifyingIntegrity = false, integrityResult = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isVerifyingIntegrity = false, error = r.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun linkJournalEntry(journalEntryId: Int) {
        viewModelScope.launch {
            when (val r = repo.linkJournalEntry(documentId, journalEntryId)) {
                is NetworkResult.Success -> _state.update { it.copy(actionSuccess = "Écriture comptable liée avec succès.") }
                is NetworkResult.Error -> _state.update { it.copy(error = r.message) }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(error = null, actionSuccess = null) }
    }
}
