package com.formuloo.core.network.api

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.gesdoc.AccountingPrefillDto
import com.formuloo.core.network.dto.gesdoc.AuditLogResponseDto
import com.formuloo.core.network.dto.gesdoc.BlockchainProofDto
import com.formuloo.core.network.dto.gesdoc.BlockchainStatusDto
import com.formuloo.core.network.dto.gesdoc.DocumentDetailDto
import com.formuloo.core.network.dto.gesdoc.DocumentListResponseDto
import com.formuloo.core.network.dto.gesdoc.LinkJournalEntryRequestDto
import com.formuloo.core.network.dto.gesdoc.LinkJournalEntryResponseDto
import com.formuloo.core.network.dto.gesdoc.OcrResultDto
import com.formuloo.core.network.dto.gesdoc.OcrStatusDto
import com.formuloo.core.network.dto.gesdoc.UploadResponseDto
import com.formuloo.core.network.dto.gesdoc.ValidateOCRRequestDto
import com.formuloo.core.network.dto.gesdoc.ValidateOCRResponseDto
import com.formuloo.core.network.dto.gesdoc.VerifyIntegrityResponseDto

interface GesDocRemoteDataSource {
    suspend fun getDocuments(
        status: String? = null,
        documentType: String? = null,
        supplier: String? = null,
        cursor: String? = null,
        pageSize: Int = 20,
    ): NetworkResult<DocumentListResponseDto>

    suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        fiscalYear: String? = null,
        notes: String? = null,
    ): NetworkResult<UploadResponseDto>

    suspend fun getDocumentDetail(id: String): NetworkResult<DocumentDetailDto>

    suspend fun getOcrStatus(id: String): NetworkResult<OcrStatusDto>
    suspend fun getOcrResult(id: String): NetworkResult<OcrResultDto>
    suspend fun validateOcr(id: String, dto: ValidateOCRRequestDto): NetworkResult<ValidateOCRResponseDto>

    suspend fun getBlockchainStatus(id: String): NetworkResult<BlockchainStatusDto>
    suspend fun getBlockchainProof(id: String): NetworkResult<BlockchainProofDto>

    suspend fun getAccountingPrefill(id: String): NetworkResult<AccountingPrefillDto>
    suspend fun linkJournalEntry(id: String, dto: LinkJournalEntryRequestDto): NetworkResult<LinkJournalEntryResponseDto>

    suspend fun verifyIntegrity(id: String): NetworkResult<VerifyIntegrityResponseDto>

    suspend fun getAuditLog(category: String? = null, cursor: String? = null): NetworkResult<AuditLogResponseDto>

    suspend fun exportAuditLog(category: String? = null, format: String = "csv"): NetworkResult<ByteArray>
}
