package com.formuloo.feature.gesdoc.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.api.GesDocRemoteDataSource
import com.formuloo.core.network.dto.gesdoc.LinkJournalEntryRequestDto
import com.formuloo.core.network.dto.gesdoc.ValidateOCRRequestDto
import com.formuloo.feature.gesdoc.domain.model.AccountingLine
import com.formuloo.feature.gesdoc.domain.model.AccountingPrefill
import com.formuloo.feature.gesdoc.domain.model.AuditAction
import com.formuloo.feature.gesdoc.domain.model.AuditActorType
import com.formuloo.feature.gesdoc.domain.model.AuditLogEntry
import com.formuloo.feature.gesdoc.domain.model.AuditLogPage
import com.formuloo.feature.gesdoc.domain.model.BlockchainProof
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentFullDetail
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentSummary
import com.formuloo.feature.gesdoc.domain.model.DocumentType
import com.formuloo.feature.gesdoc.domain.model.GesDocStats
import com.formuloo.feature.gesdoc.domain.model.IntegrityResult
import com.formuloo.feature.gesdoc.domain.model.OcrField
import com.formuloo.feature.gesdoc.domain.model.OcrResult
import com.formuloo.feature.gesdoc.domain.model.OcrStatus

class GesDocRepositoryImpl(private val remote: GesDocRemoteDataSource) : GesDocRepository {

    override suspend fun getDocuments(
        status: String?,
        documentType: String?,
        supplier: String?,
    ): NetworkResult<Pair<GesDocStats, List<DocumentSummary>>> =
        when (val r = remote.getDocuments(status = status, documentType = documentType, supplier = supplier)) {
            is NetworkResult.Success -> {
                val stats = r.data.stats?.let {
                    GesDocStats(
                        totalArchived = it.totalArchived,
                        certifiedOnChain = it.certifiedOnChain,
                        inProcessing = it.inProcessing,
                        integrityAlerts = it.integrityAlerts,
                    )
                } ?: GesDocStats()
                val docs = r.data.results.map { d ->
                    DocumentSummary(
                        id = d.id,
                        number = d.number,
                        supplier = d.supplier,
                        amountTtc = d.amountTtc,
                        currency = d.currency,
                        status = DocumentStatus.from(d.status),
                        documentType = DocumentType.from(d.documentType),
                        certifiedAt = d.certifiedAt,
                    )
                }
                NetworkResult.Success(Pair(stats, docs), r.code)
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getDocumentDetail(id: String): NetworkResult<DocumentFullDetail> =
        when (val r = remote.getDocumentDetail(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    DocumentFullDetail(
                        id = d.id,
                        documentType = DocumentType.from(d.documentType ?: ""),
                        originalFilename = d.originalFilename,
                        status = DocumentStatus.from(d.status ?: ""),
                        fileUrl = d.fileUrl,
                        previewUrl = d.previewUrl,
                        documentNumber = d.documentNumber,
                        supplier = d.supplier,
                        invoiceDate = d.invoiceDate,
                        amountHt = d.amountHt,
                        tvaRate = d.tvaRate,
                        amountTtc = d.amountTtc,
                        currency = d.currency,
                        hashSha256 = d.hashSha256,
                        certifiedPdfUrl = d.certifiedPdfUrl,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        fiscalYear: String?,
        notes: String?,
    ): NetworkResult<String> =
        when (val r = remote.uploadDocument(fileBytes, fileName, mimeType, documentType, fiscalYear, notes)) {
            is NetworkResult.Success -> NetworkResult.Success(r.data.id, r.code)
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getOcrStatus(id: String): NetworkResult<OcrStatus> =
        when (val r = remote.getOcrStatus(id)) {
            is NetworkResult.Success -> NetworkResult.Success(
                OcrStatus(
                    documentId = r.data.id,
                    status = DocumentStatus.from(r.data.status),
                    error = r.data.error,
                ),
                r.code,
            )
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getOcrResult(id: String): NetworkResult<OcrResult> =
        when (val r = remote.getOcrResult(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    OcrResult(
                        id = d.id,
                        status = d.status?.let { DocumentStatus.from(it) } ?: DocumentStatus.EXTRACTED,
                        ocrEngine = d.ocrEngine,
                        confidence = d.confidence,
                        rawText = d.rawText,
                        fields = d.fields?.mapValues { (_, v) -> OcrField(v.value, v.confidence) } ?: emptyMap(),
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun validateOcr(
        id: String,
        documentNumber: String,
        date: String,
        supplier: String,
        amountHt: Double,
        tvaRate: Double,
        amountTtc: Double,
        currency: String,
    ): NetworkResult<Unit> {
        val dto = ValidateOCRRequestDto(
            documentNumber = documentNumber,
            date = date,
            supplier = supplier,
            amountHt = amountHt,
            tvaRate = tvaRate,
            amountTtc = amountTtc,
            currency = currency,
        )
        return when (val r = remote.validateOcr(id, dto)) {
            is NetworkResult.Success -> NetworkResult.Success(Unit, r.code)
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getBlockchainStatus(id: String): NetworkResult<BlockchainStatus> =
        when (val r = remote.getBlockchainStatus(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    BlockchainStatus(
                        id = d.id,
                        status = DocumentStatus.from(d.status),
                        hashSha256 = d.hashSha256,
                        txHash = d.txHash,
                        blockNumber = d.blockNumber,
                        blockchain = d.blockchain,
                        anchoredAt = d.anchoredAt,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getBlockchainProof(id: String): NetworkResult<BlockchainProof> =
        when (val r = remote.getBlockchainProof(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    BlockchainProof(
                        documentId = d.documentId,
                        txHash = d.txHash,
                        blockNumber = d.blockNumber,
                        blockchain = d.blockchain,
                        anchoredAt = d.anchoredAt,
                        explorerUrl = d.explorerUrl,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getAccountingPrefill(id: String): NetworkResult<AccountingPrefill> =
        when (val r = remote.getAccountingPrefill(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    AccountingPrefill(
                        sourceDocumentId = d.sourceDocumentId,
                        lines = d.suggestedJournalEntry.map { l ->
                            AccountingLine(l.compte, l.libelle, l.debit, l.credit)
                        },
                        confidence = d.confidence,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun linkJournalEntry(id: String, journalEntryId: Int): NetworkResult<Unit> =
        when (val r = remote.linkJournalEntry(id, LinkJournalEntryRequestDto(journalEntryId))) {
            is NetworkResult.Success -> NetworkResult.Success(Unit, r.code)
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun verifyIntegrity(id: String): NetworkResult<IntegrityResult> =
        when (val r = remote.verifyIntegrity(id)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    IntegrityResult(
                        documentId = d.documentId,
                        integrityOk = d.integrityOk,
                        computedHash = d.computedHash,
                        storedHash = d.storedHash,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getAuditLog(category: String?, cursor: String?): NetworkResult<AuditLogPage> =
        when (val r = remote.getAuditLog(category, cursor)) {
            is NetworkResult.Success -> {
                val d = r.data
                NetworkResult.Success(
                    AuditLogPage(
                        entries = d.results.map { entry ->
                            AuditLogEntry(
                                id = entry.id,
                                action = AuditAction.from(entry.action),
                                label = entry.label,
                                detail = entry.detail,
                                documentNumber = entry.documentNumber,
                                actorType = when (entry.actorType.lowercase()) {
                                    "system" -> AuditActorType.SYSTEM
                                    "user" -> AuditActorType.USER
                                    else -> AuditActorType.UNKNOWN
                                },
                                timestamp = entry.timestamp,
                            )
                        },
                        nextCursor = d.nextCursor,
                    ),
                    r.code,
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code, r.fieldErrors)
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun exportAuditLog(category: String?): NetworkResult<ByteArray> =
        remote.exportAuditLog(category)
}
