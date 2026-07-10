package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GesDocCertificationState(
    val documentId: String,
    val isLoading: Boolean = false,
    val blockchainStatus: BlockchainStatus? = null,
    val explorerUrl: String? = null,
    val error: String? = null,
)

class GesDocCertificationViewModel(
    private val repo: GesDocRepository,
    private val documentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(GesDocCertificationState(documentId = documentId))
    val state: StateFlow<GesDocCertificationState> = _state.asStateFlow()

    init {
        loadCertification()
    }

    private fun loadCertification() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = repo.getBlockchainStatus(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(isLoading = false, blockchainStatus = r.data) }
                is NetworkResult.Error -> _state.update { it.copy(isLoading = false, error = r.message) }
                is NetworkResult.Loading -> Unit
            }
        }
        viewModelScope.launch {
            when (val r = repo.getBlockchainProof(documentId)) {
                is NetworkResult.Success -> _state.update { it.copy(explorerUrl = r.data.explorerUrl) }
                is NetworkResult.Error -> Unit
                is NetworkResult.Loading -> Unit
            }
        }
    }
}
