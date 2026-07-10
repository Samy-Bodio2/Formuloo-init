package com.formuloo.feature.gesdoc.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.gesdoc.domain.model.AccountingPrefill
import com.formuloo.feature.gesdoc.domain.model.AuditLogPage
import com.formuloo.feature.gesdoc.domain.model.BlockchainProof
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentFullDetail
import com.formuloo.feature.gesdoc.domain.model.DocumentSummary
import com.formuloo.feature.gesdoc.domain.model.GesDocStats
import com.formuloo.feature.gesdoc.domain.model.IntegrityResult
import com.formuloo.feature.gesdoc.domain.model.OcrResult
import com.formuloo.feature.gesdoc.domain.model.OcrStatus

interface GesDocRepository {
    suspend fun getDocuments(
        status: String? = null,
        documentType: String? = null,
        supplier: String? = null,
    ): NetworkResult<Pair<GesDocStats, List<DocumentSummary>>>

    suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        fiscalYear: String? = null,
        notes: String? = null,
    ): NetworkResult<String>

    suspend fun getDocumentDetail(id: String): NetworkResult<DocumentFullDetail>

    suspend fun getOcrStatus(id: String): NetworkResult<OcrStatus>

    suspend fun getOcrResult(id: String): NetworkResult<OcrResult>

    suspend fun validateOcr(
        id: String,
        documentNumber: String,
        date: String,
        supplier: String,
        amountHt: Double,
        tvaRate: Double,
        amountTtc: Double,
        currency: String = "XAF",
    ): NetworkResult<Unit>

    suspend fun getBlockchainStatus(id: String): NetworkResult<BlockchainStatus>

    suspend fun getBlockchainProof(id: String): NetworkResult<BlockchainProof>

    suspend fun getAccountingPrefill(id: String): NetworkResult<AccountingPrefill>

    suspend fun linkJournalEntry(id: String, journalEntryId: Int): NetworkResult<Unit>

    suspend fun verifyIntegrity(id: String): NetworkResult<IntegrityResult>

    suspend fun getAuditLog(category: String? = null, cursor: String? = null): NetworkResult<AuditLogPage>

    suspend fun exportAuditLog(category: String? = null): NetworkResult<ByteArray>
}
