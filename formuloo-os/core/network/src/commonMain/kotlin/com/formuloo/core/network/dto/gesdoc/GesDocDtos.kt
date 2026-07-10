package com.formuloo.core.network.dto.gesdoc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentListResponseDto(
    val count: Int,
    @SerialName("next_cursor") val nextCursor: String? = null,
    val stats: DocumentStatsDto? = null,
    val results: List<DocumentSummaryDto>,
)

@Serializable
data class DocumentSummaryDto(
    val id: String,
    val number: String? = null,
    val supplier: String? = null,
    @SerialName("amount_ttc") val amountTtc: Double? = null,
    val currency: String = "XAF",
    val status: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("certified_at") val certifiedAt: String? = null,
)

@Serializable
data class DocumentStatsDto(
    @SerialName("total_archived") val totalArchived: Int = 0,
    @SerialName("certified_on_chain") val certifiedOnChain: Int = 0,
    @SerialName("in_processing") val inProcessing: Int = 0,
    @SerialName("integrity_alerts") val integrityAlerts: Int = 0,
)

@Serializable
data class UploadResponseDto(
    val id: String,
    val status: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("file_url") val fileUrl: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class OcrStatusDto(
    val id: String,
    val status: String,
    val progress: Map<String, String>? = null,
    val error: String? = null,
)

@Serializable
data class OcrFieldDto(
    val value: String? = null,
    val confidence: Int? = null,
)

@Serializable
data class OcrResultDto(
    val id: String,
    val status: String? = null,
    @SerialName("ocr_engine") val ocrEngine: String? = null,
    val confidence: Int? = null,
    @SerialName("raw_text") val rawText: String? = null,
    val fields: Map<String, OcrFieldDto>? = null,
)

@Serializable
data class ValidateOCRRequestDto(
    @SerialName("document_number") val documentNumber: String,
    val date: String,
    val supplier: String,
    @SerialName("amount_ht") val amountHt: Double,
    @SerialName("tva_rate") val tvaRate: Double,
    @SerialName("amount_ttc") val amountTtc: Double,
    val currency: String = "XAF",
    val corrections: List<Map<String, String>> = emptyList(),
)

@Serializable
data class ValidateOCRResponseDto(
    val id: String,
    val status: String,
    @SerialName("validated_by") val validatedBy: String? = null,
    @SerialName("validated_at") val validatedAt: String? = null,
    @SerialName("corrections_count") val correctionsCount: Int = 0,
)

@Serializable
data class BlockchainStatusDto(
    val id: String,
    val status: String,
    @SerialName("hash_sha256") val hashSha256: String? = null,
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("block_number") val blockNumber: Int? = null,
    val blockchain: String? = null,
    @SerialName("anchored_at") val anchoredAt: String? = null,
)

@Serializable
data class BlockchainProofDto(
    @SerialName("document_id") val documentId: String,
    @SerialName("tx_hash") val txHash: String? = null,
    @SerialName("block_number") val blockNumber: Int? = null,
    val blockchain: String? = null,
    @SerialName("anchored_at") val anchoredAt: String? = null,
    @SerialName("explorer_url") val explorerUrl: String? = null,
)

@Serializable
data class AccountingLineDto(
    val compte: String,
    val libelle: String,
    val debit: Double,
    val credit: Double,
)

@Serializable
data class AccountingPrefillDto(
    @SerialName("source_document_id") val sourceDocumentId: String,
    @SerialName("suggested_journal_entry") val suggestedJournalEntry: List<AccountingLineDto>,
    val confidence: String? = null,
)

@Serializable
data class LinkJournalEntryRequestDto(
    @SerialName("journal_entry_id") val journalEntryId: Int,
)

@Serializable
data class LinkJournalEntryResponseDto(
    @SerialName("document_id") val documentId: String,
    @SerialName("journal_entry_id") val journalEntryId: Int,
    @SerialName("linked_at") val linkedAt: String,
)

@Serializable
data class VerifyIntegrityResponseDto(
    @SerialName("document_id") val documentId: String,
    @SerialName("integrity_ok") val integrityOk: Boolean,
    @SerialName("computed_hash") val computedHash: String? = null,
    @SerialName("stored_hash") val storedHash: String? = null,
)

@Serializable
data class DocumentDetailDto(
    val id: String,
    @SerialName("document_type") val documentType: String? = null,
    @SerialName("original_filename") val originalFilename: String? = null,
    val status: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("document_number") val documentNumber: String? = null,
    val supplier: String? = null,
    @SerialName("invoice_date") val invoiceDate: String? = null,
    @SerialName("amount_ht") val amountHt: Double? = null,
    @SerialName("tva_rate") val tvaRate: Double? = null,
    @SerialName("amount_ttc") val amountTtc: Double? = null,
    val currency: String? = null,
    @SerialName("hash_sha256") val hashSha256: String? = null,
    @SerialName("certified_pdf_url") val certifiedPdfUrl: String? = null,
)

@Serializable
data class AuditLogEntryDto(
    val id: String,
    val action: String,
    val label: String,
    val detail: String = "",
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("actor_type") val actorType: String,
    val timestamp: String,
)

@Serializable
data class AuditLogResponseDto(
    @SerialName("next_cursor") val nextCursor: String? = null,
    val results: List<AuditLogEntryDto> = emptyList(),
)
