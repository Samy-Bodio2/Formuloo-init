package com.formuloo.core.network.api

import com.formuloo.core.common.FieldErrorDetail
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.ApiError
import com.formuloo.core.network.dto.hr.CongeApproveDto
import com.formuloo.core.network.dto.hr.CongeCreateDto
import com.formuloo.core.network.dto.hr.CongeDto
import com.formuloo.core.network.dto.hr.ContratCreateDto
import com.formuloo.core.network.dto.hr.ContratDto
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.DepartementTreeDto
import com.formuloo.core.network.dto.hr.EmployeCreateDto
import com.formuloo.core.network.dto.hr.EmployeDto
import com.formuloo.core.network.dto.hr.EmployeUpdateDto
import com.formuloo.core.network.dto.hr.MesPresencesResponseDto
import com.formuloo.core.network.dto.hr.PaginatedResponse
import com.formuloo.core.network.dto.hr.DemandeDocumentCreateDto
import com.formuloo.core.network.dto.hr.DemandeDocumentDto
import com.formuloo.core.network.dto.hr.MesDemandesDocumentResponseDto
import com.formuloo.core.network.dto.hr.RejeterDemandeDocumentDto
import com.formuloo.core.network.dto.hr.StatsRHDto
import com.formuloo.core.network.dto.hr.PaieDto
import com.formuloo.core.network.dto.hr.PaiePayerDto
import com.formuloo.core.network.dto.hr.PayrollPeriodeResponseDto
import com.formuloo.core.network.dto.hr.PayrollRunDto
import com.formuloo.core.network.dto.hr.PayrollRunResultDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.core.network.dto.hr.PresenceCreateDto
import com.formuloo.core.network.dto.hr.PresenceDto
import com.formuloo.core.network.dto.hr.SoldeCongesDto
import com.formuloo.core.network.dto.hr.CongeRejectDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Client HTTP pour le service RH — contrat source : docs/api contract/hr.yaml v2.1.0
 * BASE_URL : /api/v1 (configuré dans ApiClient.kt)
 * Tous les endpoints sont préfixés par /hr/
 *
 * Sécurité : ne jamais logger les champs nationalId, numeroCnps — données personnelles sensibles.
 */
class HrApiService(private val client: HttpClient) : HrRemoteDataSource {

    // ── EMPLOYÉS ──────────────────────────────────────────────────────────

    override suspend fun getEmployees(
        page: Int,
        pageSize: Int,
        search: String?,
        statut: String?,
        departementId: String?,
    ): NetworkResult<PaginatedResponse<EmployeDto>> = safeCall {
        client.get("$BASE/employes/") {
            url {
                parameters.append("page", page.toString())
                parameters.append("page_size", pageSize.toString())
                if (search != null) parameters.append("search", search)
                if (statut != null) parameters.append("statut", statut)
                if (departementId != null) parameters.append("departement_id", departementId)
            }
        }
    }

    override suspend fun getEmployee(id: String): NetworkResult<EmployeDto> = safeCall {
        client.get("$BASE/employes/$id/")
    }

    override suspend fun createEmployee(dto: EmployeCreateDto): NetworkResult<EmployeDto> = safeCall {
        client.post("$BASE/employes/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun updateEmployee(id: String, dto: EmployeUpdateDto): NetworkResult<EmployeDto> = safeCall {
        client.patch("$BASE/employes/$id/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    // ── CONTRATS ──────────────────────────────────────────────────────────

    override suspend fun getContrats(
        employeId: String,
        isActive: Boolean?,
    ): NetworkResult<PaginatedResponse<ContratDto>> = safeCall {
        client.get("$BASE/employes/$employeId/contrats/") {
            if (isActive != null) url { parameters.append("is_active", isActive.toString()) }
        }
    }

    suspend fun getContrat(id: String): NetworkResult<ContratDto> = safeCall {
        client.get("$BASE/contrats/$id/")
    }

    override suspend fun createContrat(dto: ContratCreateDto): NetworkResult<ContratDto> = safeCall {
        client.post("$BASE/contrats/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    // ── CONGÉS ────────────────────────────────────────────────────────────

    override suspend fun getLeaves(
        employeId: String?,
        statut: String?,
    ): NetworkResult<PaginatedResponse<CongeDto>> = safeCall {
        client.get("$BASE/leaves/") {
            url {
                if (employeId != null) parameters.append("employe_id", employeId)
                if (statut != null) parameters.append("statut", statut)
            }
        }
    }

    override suspend fun approveLeave(id: String, dto: CongeApproveDto): NetworkResult<CongeDto> = safeCall {
        client.post("$BASE/leaves/$id/approve/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun rejectLeave(id: String, dto: CongeRejectDto): NetworkResult<CongeDto> = safeCall {
        client.post("$BASE/leaves/$id/reject/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun createLeaveRequest(dto: CongeCreateDto): NetworkResult<CongeDto> = safeCall {
        client.post("$BASE/leaves/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun cancelLeaveRequest(id: String): NetworkResult<Unit> = try {
        val response = client.delete("$BASE/leaves/$id/")
        if (response.status.isSuccess()) {
            NetworkResult.Success(Unit, response.status.value)
        } else {
            response.toErrorResult()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    // ── SOLDES DE CONGÉS ──────────────────────────────────────────────────

    override suspend fun getLeaveBalance(
        employeId: String?,
        annee: Int?,
    ): NetworkResult<PaginatedResponse<SoldeCongesDto>> = safeCall {
        client.get("$BASE/soldes-conges/") {
            url {
                if (employeId != null) parameters.append("employe_id", employeId)
                if (annee != null) parameters.append("annee", annee.toString())
            }
        }
    }

    // ── FICHES DE PAIE ────────────────────────────────────────────────────

    override suspend fun getPayslips(
        employeId: String?,
        mois: Int?,
        annee: Int?,
        statut: String?,
    ): NetworkResult<PaginatedResponse<PaieDto>> = safeCall {
        client.get("$BASE/payroll/") {
            url {
                if (employeId != null) parameters.append("employe_id", employeId)
                if (mois != null) parameters.append("mois", mois.toString())
                if (annee != null) parameters.append("annee", annee.toString())
                if (statut != null) parameters.append("statut", statut)
            }
        }
    }

    override suspend fun getPayslip(id: String): NetworkResult<PaieDto> = safeCall {
        client.get("$BASE/payroll/$id/")
    }

    override suspend fun validatePayslip(id: String): NetworkResult<PaieDto> = safeCall {
        client.post("$BASE/payroll/$id/valider/")
    }

    override suspend fun payPayslip(id: String, dto: PaiePayerDto): NetworkResult<PaieDto> = safeCall {
        client.post("$BASE/payroll/$id/payer/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun downloadPayslipPdf(id: String): NetworkResult<ByteArray> = safeCall {
        client.get("$BASE/payroll/$id/pdf/")
    }

    // ── DÉPARTEMENTS / ORGANIGRAMME ───────────────────────────────────────

    override suspend fun getDepartements(): NetworkResult<PaginatedResponse<DepartementDto>> = safeCall {
        client.get("$BASE/departements/")
    }

    override suspend fun getPostes(): NetworkResult<PaginatedResponse<PosteBriefDto>> = safeCall {
        client.get("$BASE/postes/")
    }

    suspend fun getDepartementsTree(): NetworkResult<List<DepartementTreeDto>> = safeCall {
        client.get("$BASE/departements/tree/")
    }

    override suspend fun getOrganizationTree(): NetworkResult<List<DepartementTreeDto>> = getDepartementsTree()

    // ── PRÉSENCES ─────────────────────────────────────────────────────────

    override suspend fun getPresences(
        employeId: String?,
        statut: String?,
        dateDebut: String?,
        dateFin: String?,
        page: Int,
    ): NetworkResult<PaginatedResponse<PresenceDto>> = safeCall {
        client.get("$BASE/presences/") {
            url {
                parameters.append("page", page.toString())
                if (employeId != null) parameters.append("employe_id", employeId)
                if (statut != null) parameters.append("statut", statut)
                if (dateDebut != null) parameters.append("date_debut", dateDebut)
                if (dateFin != null) parameters.append("date_fin", dateFin)
            }
        }
    }

    override suspend fun createPresence(dto: PresenceCreateDto): NetworkResult<PresenceDto> = safeCall {
        client.post("$BASE/presences/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun updatePresence(id: String, dto: PresenceCreateDto): NetworkResult<PresenceDto> = safeCall {
        client.patch("$BASE/presences/$id/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun archivePresence(id: String): NetworkResult<Unit> = try {
        val response = client.delete("$BASE/presences/$id/")
        if (response.status.isSuccess()) NetworkResult.Success(Unit)
        else response.toErrorResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    override suspend fun getMyPresences(mois: Int?, annee: Int?): NetworkResult<MesPresencesResponseDto> = safeCall {
        client.get("$BASE/me/presences/") {
            url {
                if (mois != null) parameters.append("mois", mois.toString())
                if (annee != null) parameters.append("annee", annee.toString())
            }
        }
    }

    override suspend fun archiveEmployee(id: String): NetworkResult<Unit> = try {
        val response = client.delete("$BASE/employes/$id/")
        if (response.status.isSuccess()) NetworkResult.Success(Unit)
        else response.toErrorResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    override suspend fun runPayroll(dto: PayrollRunDto): NetworkResult<PayrollRunResultDto> = safeCall {
        client.post("$BASE/payroll/run/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun getPayrollByPeriode(periode: String): NetworkResult<PayrollPeriodeResponseDto> = safeCall {
        client.get("$BASE/payroll/periode/$periode/")
    }

    override suspend fun getMesDemandesDocument(statut: String?): NetworkResult<MesDemandesDocumentResponseDto> = safeCall {
        client.get("$BASE/me/demandes-document/") {
            url { if (statut != null) parameters.append("statut", statut) }
        }
    }

    override suspend fun createDemandeDocument(dto: DemandeDocumentCreateDto): NetworkResult<DemandeDocumentDto> = safeCall {
        client.post("$BASE/me/demandes-document/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun getMeDemandeDocument(id: String): NetworkResult<DemandeDocumentDto> = safeCall {
        client.get("$BASE/me/demandes-document/$id/")
    }

    override suspend fun cancelDemandeDocument(id: String): NetworkResult<Unit> = try {
        val response = client.delete("$BASE/me/demandes-document/$id/")
        if (response.status.isSuccess()) NetworkResult.Success(Unit)
        else response.toErrorResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    // ── DEMANDES DOCUMENTS (RH) ───────────────────────────────────────────

    override suspend fun getDemandesDocumentRH(
        statut: String?,
        typeDocument: String?,
    ): NetworkResult<MesDemandesDocumentResponseDto> = safeCall {
        client.get("$BASE/demandes-document/") {
            url {
                if (statut != null) parameters.append("statut", statut)
                if (typeDocument != null) parameters.append("type_document", typeDocument)
            }
        }
    }

    override suspend fun approuverDemandeDocument(id: String): NetworkResult<DemandeDocumentDto> = safeCall {
        client.post("$BASE/demandes-document/$id/approuver/") {
            contentType(ContentType.Application.Json)
        }
    }

    override suspend fun rejeterDemandeDocument(
        id: String,
        dto: RejeterDemandeDocumentDto,
    ): NetworkResult<DemandeDocumentDto> = safeCall {
        client.post("$BASE/demandes-document/$id/rejeter/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    // ── STATS RH ─────────────────────────────────────────────────────────

    override suspend fun getStatsRH(): NetworkResult<StatsRHDto> = safeCall {
        client.get("$BASE/stats/")
    }

    // ── HELPERS PRIVÉS ────────────────────────────────────────────────────

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
                message = apiError?.error?.message ?: "Ressource introuvable.",
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
        const val BASE = "/api/v1/hr"
    }
}
