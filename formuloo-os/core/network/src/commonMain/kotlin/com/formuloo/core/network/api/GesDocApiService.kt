package com.formuloo.core.network.api

import com.formuloo.core.common.FieldErrorDetail
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.ApiError
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

class GesDocApiService(private val client: HttpClient) : GesDocRemoteDataSource {

    override suspend fun getDocuments(
        status: String?,
        documentType: String?,
        supplier: String?,
        cursor: String?,
        pageSize: Int,
    ): NetworkResult<DocumentListResponseDto> = safeCall {
        client.get("$BASE/") {
            url {
                if (status != null) parameters.append("status", status)
                if (documentType != null) parameters.append("document_type", documentType)
                if (supplier != null) parameters.append("supplier", supplier)
                if (cursor != null) parameters.append("cursor", cursor)
                parameters.append("page_size", pageSize.toString())
            }
        }
    }

    override suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        fiscalYear: String?,
        notes: String?,
    ): NetworkResult<UploadResponseDto> = safeCall {
        client.post("$BASE/upload/") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("document_type", documentType)
                        if (fiscalYear != null) append("fiscal_year", fiscalYear)
                        if (notes != null) append("notes", notes)
                        append(
                            "file",
                            fileBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                    },
                ),
            )
        }
    }

    override suspend fun getDocumentDetail(id: String): NetworkResult<DocumentDetailDto> = safeCall {
        client.get("$BASE/$id/")
    }

    override suspend fun getOcrStatus(id: String): NetworkResult<OcrStatusDto> = safeCall {
        client.get("$BASE/$id/ocr-status/")
    }

    override suspend fun getOcrResult(id: String): NetworkResult<OcrResultDto> = safeCall {
        client.get("$BASE/$id/ocr-result/")
    }

    override suspend fun validateOcr(id: String, dto: ValidateOCRRequestDto): NetworkResult<ValidateOCRResponseDto> = safeCall {
        client.post("$BASE/$id/validate-ocr/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun getBlockchainStatus(id: String): NetworkResult<BlockchainStatusDto> = safeCall {
        client.get("$BASE/$id/blockchain-status/")
    }

    override suspend fun getBlockchainProof(id: String): NetworkResult<BlockchainProofDto> = safeCall {
        client.get("$BASE/$id/blockchain-proof/")
    }

    override suspend fun getAccountingPrefill(id: String): NetworkResult<AccountingPrefillDto> = safeCall {
        client.get("$BASE/$id/accounting-prefill/")
    }

    override suspend fun linkJournalEntry(id: String, dto: LinkJournalEntryRequestDto): NetworkResult<LinkJournalEntryResponseDto> = safeCall {
        client.post("$BASE/$id/link-journal-entry/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun verifyIntegrity(id: String): NetworkResult<VerifyIntegrityResponseDto> = safeCall {
        client.post("$BASE/$id/verify-integrity/")
    }

    override suspend fun getAuditLog(category: String?, cursor: String?): NetworkResult<AuditLogResponseDto> = safeCall {
        client.get("$BASE/audit-log/") {
            url {
                if (category != null) parameters.append("category", category)
                if (cursor != null) parameters.append("cursor", cursor)
            }
        }
    }

    override suspend fun exportAuditLog(category: String?, format: String): NetworkResult<ByteArray> = safeCall {
        client.get("$BASE/audit-log/") {
            url {
                if (category != null) parameters.append("category", category)
                parameters.append("export", format)
            }
        }
    }

    // ── Helpers privés ─────────────────────────────────────────────────────

    private suspend inline fun <reified T> safeCall(
        crossinline block: suspend () -> HttpResponse,
    ): NetworkResult<T> = try {
        block().toResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    private suspend inline fun <reified T> HttpResponse.toResult(): NetworkResult<T> =
        if (status.isSuccess()) {
            NetworkResult.Success(body<T>(), status.value)
        } else {
            toErrorResult()
        }

    private suspend fun <T> HttpResponse.toErrorResult(): NetworkResult<T> {
        val code = status.value
        val apiError = try { body<ApiError>() } catch (e: Exception) { null }
        return when (code) {
            401 -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Authentification requise.",
                code = code,
            )
            404 -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Document introuvable.",
                code = code,
            )
            else -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Erreur serveur ($code).",
                code = code,
                fieldErrors = apiError?.error?.details?.map {
                    FieldErrorDetail(field = it.field, message = it.message)
                } ?: emptyList(),
            )
        }
    }

    private companion object {
        const val BASE = "/api/v1/documents"
    }
}
