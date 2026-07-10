package com.formuloo.feature.gesdoc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.domain.model.DocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GesDocUploadState(
    val fileBytes: ByteArray? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val selectedType: DocumentType = DocumentType.INVOICE,
    val currentStep: Int = 1,
    val totalSteps: Int = 4,
    val isUploading: Boolean = false,
    val uploadedDocumentId: String? = null,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = fileBytes != null && !isUploading
    val fileSizeLabel: String get() = fileBytes?.let { formatFileSize(it) } ?: ""

    private fun formatFileSize(bytes: ByteArray): String {
        val kb = bytes.size / 1024.0
        return if (kb < 1024) {
            "%.0f Ko".format(kb)
        } else {
            "%.1f Mo".format(kb / 1024).replace('.', ',')
        }
    }

    // ByteArray equality is reference-based in data class — override for content-based
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GesDocUploadState) return false
        return fileName == other.fileName &&
                mimeType == other.mimeType &&
                selectedType == other.selectedType &&
                currentStep == other.currentStep &&
                isUploading == other.isUploading &&
                uploadedDocumentId == other.uploadedDocumentId &&
                error == other.error
    }

    override fun hashCode(): Int = fileName.hashCode() * 31 + selectedType.hashCode()
}

class GesDocUploadViewModel(
    private val repo: GesDocRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GesDocUploadState())
    val state: StateFlow<GesDocUploadState> = _state.asStateFlow()

    fun setPickedFile(bytes: ByteArray, name: String, mime: String) {
        _state.update { it.copy(fileBytes = bytes, fileName = name, mimeType = mime) }
    }

    fun clearFile() {
        _state.update { it.copy(fileBytes = null, fileName = null, mimeType = null) }
    }

    fun selectDocumentType(type: DocumentType) {
        _state.update { it.copy(selectedType = type) }
    }

    fun submit() {
        val s = _state.value
        val bytes = s.fileBytes ?: return
        val name = s.fileName ?: "document"
        val mime = s.mimeType ?: "application/octet-stream"
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            when (val result = repo.uploadDocument(
                fileBytes = bytes,
                fileName = name,
                mimeType = mime,
                documentType = s.selectedType.name.lowercase(),
            )) {
                is NetworkResult.Success -> _state.update {
                    it.copy(isUploading = false, uploadedDocumentId = result.data)
                }
                is NetworkResult.Error -> _state.update {
                    it.copy(isUploading = false, error = result.message ?: "Erreur lors de l'envoi")
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearUploadedId() = _state.update { it.copy(uploadedDocumentId = null) }
    fun clearError() = _state.update { it.copy(error = null) }
}
