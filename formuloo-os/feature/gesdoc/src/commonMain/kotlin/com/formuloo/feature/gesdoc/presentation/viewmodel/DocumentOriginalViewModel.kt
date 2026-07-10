package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.DocumentFullDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentOriginalState(
    val documentId: String,
    val isLoading: Boolean = false,
    val detail: DocumentFullDetail? = null,
    val error: String? = null,
)

class DocumentOriginalViewModel(
    private val repo: GesDocRepository,
    private val documentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentOriginalState(documentId = documentId))
    val state: StateFlow<DocumentOriginalState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getDocumentDetail(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, detail = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
