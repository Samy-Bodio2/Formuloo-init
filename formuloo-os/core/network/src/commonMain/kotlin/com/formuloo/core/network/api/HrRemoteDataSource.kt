package com.formuloo.core.network.api

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.hr.CongeApproveDto
import com.formuloo.core.network.dto.hr.CongeCreateDto
import com.formuloo.core.network.dto.hr.CongeDto
import com.formuloo.core.network.dto.hr.CongeRejectDto
import com.formuloo.core.network.dto.hr.ContratCreateDto
import com.formuloo.core.network.dto.hr.ContratDto
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.DepartementTreeDto
import com.formuloo.core.network.dto.hr.EmployeCreateDto
import com.formuloo.core.network.dto.hr.EmployeDto
import com.formuloo.core.network.dto.hr.EmployeUpdateDto
import com.formuloo.core.network.dto.hr.DemandeDocumentCreateDto
import com.formuloo.core.network.dto.hr.DemandeDocumentDto
import com.formuloo.core.network.dto.hr.MesDemandesDocumentResponseDto
import com.formuloo.core.network.dto.hr.RejeterDemandeDocumentDto
import com.formuloo.core.network.dto.hr.StatsRHDto
import com.formuloo.core.network.dto.hr.MesPresencesResponseDto
import com.formuloo.core.network.dto.hr.PaginatedResponse
import com.formuloo.core.network.dto.hr.PaieDto
import com.formuloo.core.network.dto.hr.PaiePayerDto
import com.formuloo.core.network.dto.hr.PayrollPeriodeResponseDto
import com.formuloo.core.network.dto.hr.PayrollRunDto
import com.formuloo.core.network.dto.hr.PayrollRunResultDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.core.network.dto.hr.PresenceCreateDto
import com.formuloo.core.network.dto.hr.PresenceDto
import com.formuloo.core.network.dto.hr.SoldeCongesDto

interface HrRemoteDataSource {

    suspend fun getEmployees(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null,
        statut: String? = null,
        departementId: String? = null,
    ): NetworkResult<PaginatedResponse<EmployeDto>>

    suspend fun getEmployee(id: String): NetworkResult<EmployeDto>

    suspend fun createEmployee(dto: EmployeCreateDto): NetworkResult<EmployeDto>

    suspend fun updateEmployee(id: String, dto: EmployeUpdateDto): NetworkResult<EmployeDto>

    suspend fun getContrats(
        employeId: String,
        isActive: Boolean? = null,
    ): NetworkResult<PaginatedResponse<ContratDto>>

    suspend fun createContrat(dto: ContratCreateDto): NetworkResult<ContratDto>

    suspend fun getLeaves(
        employeId: String? = null,
        statut: String? = null,
    ): NetworkResult<PaginatedResponse<CongeDto>>

    suspend fun approveLeave(id: String, dto: CongeApproveDto): NetworkResult<CongeDto>

    suspend fun rejectLeave(id: String, dto: CongeRejectDto): NetworkResult<CongeDto>

    suspend fun createLeaveRequest(dto: CongeCreateDto): NetworkResult<CongeDto>

    /** DELETE /hr/leaves/{id}/ — annulation, autorisée uniquement en statut "pending" côté serveur. */
    suspend fun cancelLeaveRequest(id: String): NetworkResult<Unit>

    suspend fun getLeaveBalance(
        employeId: String? = null,
        annee: Int? = null,
    ): NetworkResult<PaginatedResponse<SoldeCongesDto>>

    suspend fun getPayslips(
        employeId: String? = null,
        mois: Int? = null,
        annee: Int? = null,
        statut: String? = null,
    ): NetworkResult<PaginatedResponse<PaieDto>>

    suspend fun getPayslip(id: String): NetworkResult<PaieDto>

    suspend fun validatePayslip(id: String): NetworkResult<PaieDto>

    suspend fun payPayslip(id: String, dto: PaiePayerDto): NetworkResult<PaieDto>

    suspend fun downloadPayslipPdf(id: String): NetworkResult<ByteArray>

    suspend fun getOrganizationTree(): NetworkResult<List<DepartementTreeDto>>

    suspend fun getDepartements(): NetworkResult<PaginatedResponse<DepartementDto>>

    suspend fun getPostes(): NetworkResult<PaginatedResponse<PosteBriefDto>>

    // ── PRÉSENCES ─────────────────────────────────────────────────────────

    suspend fun getPresences(
        employeId: String? = null,
        statut: String? = null,
        dateDebut: String? = null,
        dateFin: String? = null,
        page: Int = 1,
    ): NetworkResult<PaginatedResponse<PresenceDto>>

    suspend fun createPresence(dto: PresenceCreateDto): NetworkResult<PresenceDto>

    suspend fun updatePresence(id: String, dto: PresenceCreateDto): NetworkResult<PresenceDto>

    suspend fun archivePresence(id: String): NetworkResult<Unit>

    suspend fun getMyPresences(mois: Int? = null, annee: Int? = null): NetworkResult<MesPresencesResponseDto>

    // ── Archivage employé ─────────────────────────────────────────────────

    suspend fun archiveEmployee(id: String): NetworkResult<Unit>

    // ── Génération de paie en masse ───────────────────────────────────────

    suspend fun runPayroll(dto: PayrollRunDto): NetworkResult<PayrollRunResultDto>

    suspend fun getPayrollByPeriode(periode: String): NetworkResult<PayrollPeriodeResponseDto>

    // ── Demandes de documents (self-service) ──────────────────────────────

    suspend fun getMesDemandesDocument(statut: String? = null): NetworkResult<MesDemandesDocumentResponseDto>

    suspend fun createDemandeDocument(dto: DemandeDocumentCreateDto): NetworkResult<DemandeDocumentDto>

    suspend fun getMeDemandeDocument(id: String): NetworkResult<DemandeDocumentDto>

    suspend fun cancelDemandeDocument(id: String): NetworkResult<Unit>

    // ── Demandes de documents (RH) ─────────────────────────────────────────

    suspend fun getDemandesDocumentRH(
        statut: String? = null,
        typeDocument: String? = null,
    ): NetworkResult<MesDemandesDocumentResponseDto>

    suspend fun approuverDemandeDocument(id: String): NetworkResult<DemandeDocumentDto>

    suspend fun rejeterDemandeDocument(id: String, dto: RejeterDemandeDocumentDto): NetworkResult<DemandeDocumentDto>

    // ── Stats RH ──────────────────────────────────────────────────────────

    suspend fun getStatsRH(): NetworkResult<StatsRHDto>
}
