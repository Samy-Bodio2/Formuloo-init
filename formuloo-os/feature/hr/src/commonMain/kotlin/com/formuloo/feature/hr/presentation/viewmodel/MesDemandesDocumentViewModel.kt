package com.formuloo.feature.hr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.DocumentRequestStatus
import com.formuloo.feature.hr.domain.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentCreateForm(
    val typeDocument: DocumentType = DocumentType.ATTESTATION_TRAVAIL,
    val motifDemande: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class MesDemandesDocumentViewModel(private val repository: HrRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<DocumentRequest>>>(UiState.Loading)
    val state: StateFlow<UiState<List<DocumentRequest>>> = _state.asStateFlow()

    private val _statutFilter = MutableStateFlow<DocumentRequestStatus?>(null)
    val statutFilter: StateFlow<DocumentRequestStatus?> = _statutFilter.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _createForm = MutableStateFlow(DocumentCreateForm())
    val createForm: StateFlow<DocumentCreateForm> = _createForm.asStateFlow()

    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError: StateFlow<String?> = _cancelError.asStateFlow()

    private val _cancellingId = MutableStateFlow<String?>(null)
    val cancellingId: StateFlow<String?> = _cancellingId.asStateFlow()

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
            _state.value = when (val r = repository.getMesDemandesDocument(apiStatut)) {
                is NetworkResult.Success -> if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
                is NetworkResult.Error -> UiState.Error(r.message, r.code)
                else -> UiState.Loading
            }
        }
    }

    fun openCreateDialog() {
        _createForm.value = DocumentCreateForm()
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun updateCreateType(type: DocumentType) {
        _createForm.value = _createForm.value.copy(typeDocument = type, error = null)
    }

    fun updateCreateMotif(motif: String) {
        _createForm.value = _createForm.value.copy(motifDemande = motif, error = null)
    }

    fun submitCreate() {
        val form = _createForm.value
        if (form.isLoading) return
        viewModelScope.launch {
            _createForm.value = form.copy(isLoading = true, error = null)
            val apiType = when (form.typeDocument) {
                DocumentType.ATTESTATION_TRAVAIL -> "attestation_travail"
                DocumentType.ATTESTATION_SALAIRE -> "attestation_salaire"
                DocumentType.BULLETIN_PAIE_COPIE -> "bulletin_paie_copie"
            }
            when (val r = repository.createDemandeDocument(apiType, form.motifDemande.ifBlank { null })) {
                is NetworkResult.Success -> {
                    _showCreateDialog.value = false
                    _createForm.value = DocumentCreateForm()
                    load()
                }
                is NetworkResult.Error -> _createForm.value = form.copy(isLoading = false, error = r.message)
                else -> _createForm.value = form.copy(isLoading = false)
            }
        }
    }

    fun cancelRequest(id: String) {
        if (_cancellingId.value != null) return
        viewModelScope.launch {
            _cancellingId.value = id
            _cancelError.value = null
            when (val r = repository.cancelDemandeDocument(id)) {
                is NetworkResult.Success -> load()
                is NetworkResult.Error -> _cancelError.value = r.message
                else -> Unit
            }
            _cancellingId.value = null
        }
    }

    fun clearCancelError() {
        _cancelError.value = null
    }
}
