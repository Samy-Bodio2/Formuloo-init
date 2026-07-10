package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.DocumentRequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DemandesDocumentRHViewModel(private val repository: HrRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<DocumentRequest>>>(UiState.Loading)
    val state: StateFlow<UiState<List<DocumentRequest>>> = _state.asStateFlow()

    private val _statutFilter = MutableStateFlow<DocumentRequestStatus?>(DocumentRequestStatus.EN_ATTENTE)
    val statutFilter: StateFlow<DocumentRequestStatus?> = _statutFilter.asStateFlow()

    private val _processingId = MutableStateFlow<String?>(null)
    val processingId: StateFlow<String?> = _processingId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _rejectTargetId = MutableStateFlow<String?>(null)
    val rejectTargetId: StateFlow<String?> = _rejectTargetId.asStateFlow()

    private val _rejectMotif = MutableStateFlow("")
    val rejectMotif: StateFlow<String> = _rejectMotif.asStateFlow()

    private val _rejectLoading = MutableStateFlow(false)
    val rejectLoading: StateFlow<Boolean> = _rejectLoading.asStateFlow()

    init { load() }

    fun setStatutFilter(statut: DocumentRequestStatus?) {
        _statutFilter.value = statut
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val apiStatut = when (_statutFilter.value) {
                DocumentRequestStatus.EN_ATTENTE -> "en_attente"
                DocumentRequestStatus.APPROUVEE -> "approuvee"
                DocumentRequestStatus.REJETEE -> "rejetee"
                DocumentRequestStatus.ANNULEE -> "annulee"
                null -> null
            }
            _state.value = when (val r = repository.getDemandesDocumentRH(statut = apiStatut)) {
                is NetworkResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                else -> UiState.Loading
            }
        }
    }

    fun approve(id: String) {
        if (_processingId.value != null) return
        viewModelScope.launch {
            _processingId.value = id
            _actionError.value = null
            when (val r = repository.approuverDemandeDocument(id)) {
                is NetworkResult.Success -> load()
                is NetworkResult.Error -> _actionError.value = r.message
                else -> Unit
            }
            _processingId.value = null
        }
    }

    fun openRejectDialog(id: String) {
        _rejectTargetId.value = id
        _rejectMotif.value = ""
    }

    fun updateRejectMotif(motif: String) {
        _rejectMotif.value = motif
    }

    fun submitReject() {
        val id = _rejectTargetId.value ?: return
        val motif = _rejectMotif.value.trim()
        if (motif.length < 5) return
        if (_rejectLoading.value) return
        viewModelScope.launch {
            _rejectLoading.value = true
            _actionError.value = null
            when (val r = repository.rejeterDemandeDocument(id, motif)) {
                is NetworkResult.Success -> {
                    _rejectTargetId.value = null
                    _rejectMotif.value = ""
                    load()
                }
                is NetworkResult.Error -> _actionError.value = r.message
                else -> Unit
            }
            _rejectLoading.value = false
        }
    }

    fun closeRejectDialog() {
        _rejectTargetId.value = null
        _rejectMotif.value = ""
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
