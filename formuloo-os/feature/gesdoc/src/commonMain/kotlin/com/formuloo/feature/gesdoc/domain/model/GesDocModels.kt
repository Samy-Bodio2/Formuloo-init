package com.formuloo.feature.gesdoc.domain.model

data class GesDocStats(
    val totalArchived: Int = 0,
    val certifiedOnChain: Int = 0,
    val inProcessing: Int = 0,
    val integrityAlerts: Int = 0,
)

data class DocumentSummary(
    val id: String,
    val number: String?,
    val supplier: String?,
    val amountTtc: Double?,
    val currency: String,
    val status: DocumentStatus,
    val documentType: DocumentType,
    val certifiedAt: String?,
)

enum class DocumentStatus(val label: String) {
    PENDING_OCR("En attente OCR"),
    PREPROCESSING("Pré-traitement"),
    EXTRACTING("Extraction"),
    ANALYZING("Analyse"),
    EXTRACTED("Extrait"),
    VALIDATED("Validé"),
    PENDING_CHAIN("En attente blockchain"),
    CERTIFIED("Certifié"),
    TAMPERED("Altéré"),
    FAILED("Échec"),
    UNKNOWN("Inconnu");

    companion object {
        fun from(value: String): DocumentStatus =
            entries.firstOrNull { it.name.lowercase() == value.lowercase().replace("-", "_") } ?: UNKNOWN
    }
}

enum class DocumentType(val label: String) {
    INVOICE("Facture"),
    PURCHASE_ORDER("Bon de commande"),
    RECEIPT("Reçu"),
    UNKNOWN("Inconnu");

    companion object {
        fun from(value: String): DocumentType =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() } ?: UNKNOWN
    }
}

data class OcrResult(
    val id: String,
    val status: DocumentStatus,
    val ocrEngine: String?,
    val confidence: Int?,
    val rawText: String?,
    val fields: Map<String, OcrField>,
)

data class OcrField(
    val value: String?,
    val confidence: Int?,
)

data class BlockchainStatus(
    val id: String,
    val status: DocumentStatus,
    val hashSha256: String?,
    val txHash: String?,
    val blockNumber: Int?,
    val blockchain: String?,
    val anchoredAt: String?,
)

data class BlockchainProof(
    val documentId: String,
    val txHash: String?,
    val blockNumber: Int?,
    val blockchain: String?,
    val anchoredAt: String?,
    val explorerUrl: String?,
)

data class AccountingPrefill(
    val sourceDocumentId: String,
    val lines: List<AccountingLine>,
    val confidence: String?,
)

data class AccountingLine(
    val compte: String,
    val libelle: String,
    val debit: Double,
    val credit: Double,
)

data class IntegrityResult(
    val documentId: String,
    val integrityOk: Boolean,
    val computedHash: String?,
    val storedHash: String?,
)

data class DocumentFullDetail(
    val id: String,
    val documentType: DocumentType,
    val originalFilename: String?,
    val status: DocumentStatus,
    val fileUrl: String?,
    val previewUrl: String?,
    val documentNumber: String?,
    val supplier: String?,
    val invoiceDate: String?,
    val amountHt: Double?,
    val tvaRate: Double?,
    val amountTtc: Double?,
    val currency: String?,
    val hashSha256: String?,
    val certifiedPdfUrl: String?,
) {
    val viewableUrl: String? get() = certifiedPdfUrl ?: fileUrl ?: previewUrl
}

data class OcrStatus(
    val documentId: String,
    val status: DocumentStatus,
    val error: String? = null,
)

enum class AuditActorType { SYSTEM, USER, UNKNOWN }

enum class AuditAction {
    UPLOAD,
    OCR_EXTRACTED,
    OCR_VALIDATED,
    OCR_CORRECTION,
    CERTIFIED,
    JOURNAL_LINKED,
    INTEGRITY_CHECK,
    INTEGRITY_ALERT,
    UNKNOWN;

    companion object {
        fun from(value: String): AuditAction =
            entries.firstOrNull { it.name.lowercase() == value.lowercase() } ?: UNKNOWN
    }
}

data class AuditLogEntry(
    val id: String,
    val action: AuditAction,
    val label: String,
    val detail: String,
    val documentNumber: String?,
    val actorType: AuditActorType,
    val timestamp: String,
)

data class AuditLogPage(
    val entries: List<AuditLogEntry>,
    val nextCursor: String?,
)
