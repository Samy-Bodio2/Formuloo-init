package com.formuloo.feature.hr.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.OrgNode
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.AdminLeaveBalance
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PayrollRunResult
import com.formuloo.feature.hr.domain.model.StatsRH
import kotlinx.coroutines.flow.Flow

interface HrRepository {

    // ── Employés (offline-first via Flow) ──────────────────────────────────
    fun getEmployees(
        search: String? = null,
        status: String? = null,
    ): Flow<NetworkResult<List<Employee>>>

    suspend fun getEmployee(id: String): NetworkResult<Employee>

    suspend fun updateEmployee(
        id: String,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,
        phonePerso: String? = null,
        emailPerso: String? = null,
        address: String? = null,
        ville: String? = null,
        situationFamiliale: String? = null,
        nombreEnfants: Int? = null,
        departmentId: String? = null,
        positionId: String? = null,
        managerId: String? = null,
        status: String? = null,
        salaireBase: Double? = null,
        modePaiement: String? = null,
        numeroCompte: String? = null,
        banque: String? = null,
    ): NetworkResult<Employee>

    suspend fun createEmployee(
        firstName: String,
        lastName: String,
        gender: String,
        email: String,
        phone: String,
        hireDate: String,
        status: String = "active",
        typeEmploye: String = "permanent",
        birthDate: String? = null,
        nationality: String? = null,
        nationalId: String? = null,
        situationFamiliale: String? = null,
        nombreEnfants: Int = 0,
        numeroCnps: String? = null,
        address: String? = null,
        departmentId: String? = null,
        positionId: String? = null,
        managerId: String? = null,
        photoUrl: String? = null,
        userId: String? = null,
        salaireBase: Double? = null,
        ville: String? = null,
    ): NetworkResult<Employee>

    // ── Contrats (offline-first via Flow) ──────────────────────────────────
    fun getContracts(employeeId: String): Flow<NetworkResult<List<Contract>>>

    suspend fun createContract(
        employeId: String,
        type: String,
        startDate: String,
        endDate: String? = null,
        grossSalary: Double,
        currency: String = "XAF",
        workHoursPerWeek: Int = 40,
        trialPeriod: Int? = null,
        documentUrl: String? = null,
        signedAt: String? = null,
    ): NetworkResult<Contract>

    // ── Congés (toujours depuis l'API, pas de cache local) ─────────────────
    suspend fun getPendingLeaves(): NetworkResult<List<LeaveRequest>>

    suspend fun approveLeave(id: String, commentaire: String?): NetworkResult<LeaveRequest>

    suspend fun rejectLeave(id: String, reason: String): NetworkResult<LeaveRequest>

    // ── Congés — FOR16A26-988 (offline-first + sync) ────────────────────────

    /** Congés de l'employé courant — fusionne le cache serveur et la file d'attente offline. */
    fun getMyLeaves(): Flow<NetworkResult<List<LeaveRequest>>>

    suspend fun getLeaveBalance(employeeId: String? = null, annee: Int? = null): NetworkResult<List<LeaveBalance>>

    /**
     * Si hors-ligne : insertion locale optimiste (is_pending_sync=1, succès immédiat —
     * synchronisée plus tard par [com.formuloo.feature.hr.data.sync.LeaveSyncManager]).
     * Si en ligne : envoi direct à l'API.
     */
    suspend fun requestLeave(typeCode: String, startDate: String, endDate: String, reason: String?): NetworkResult<LeaveRequest>

    suspend fun cancelLeave(id: String): NetworkResult<Unit>

    fun getTeamPendingLeaves(): Flow<NetworkResult<List<LeaveRequest>>>

    suspend fun approveLeave(id: String): NetworkResult<LeaveRequest>

    // ── Paie (lecture seule) ─────────────────────────────────────────────

    fun getMyPayslips(annee: Int? = null): Flow<NetworkResult<List<Payslip>>>

    suspend fun getPayslipDetail(id: String): NetworkResult<Payslip>

    suspend fun validatePayslip(id: String): NetworkResult<Payslip>

    suspend fun payPayslip(id: String, modePaiement: String): NetworkResult<Payslip>

    suspend fun downloadPayslipPdf(id: String): NetworkResult<ByteArray>

    /** Bulletins d'un employé spécifique — appel direct API, pas de cache local. */
    suspend fun getPayslipsForEmployee(employeeId: String): NetworkResult<List<Payslip>>

    // ── Organigramme ─────────────────────────────────────────────────────

    suspend fun getOrganizationTree(): NetworkResult<List<OrgNode>>

    // ── Référentiels (départements, postes) ───────────────────────────────

    suspend fun getDepartements(): NetworkResult<List<DepartementDto>>

    suspend fun getPostes(): NetworkResult<List<PosteBriefDto>>

    // ── Présences ────────────────────────────────────────────────────────

    suspend fun getPresences(
        employeId: String? = null,
        statut: String? = null,
        dateDebut: String? = null,
        dateFin: String? = null,
    ): NetworkResult<List<Presence>>

    suspend fun createPresence(
        employeId: String,
        date: String,
        heureArrivee: String? = null,
        heureDepart: String? = null,
        heuresSupplementaires: Double? = null,
        statut: String = "present",
        commentaire: String? = null,
    ): NetworkResult<Presence>

    suspend fun updatePresence(
        id: String,
        heureArrivee: String? = null,
        heureDepart: String? = null,
        statut: String? = null,
        commentaire: String? = null,
    ): NetworkResult<Presence>

    suspend fun archivePresence(id: String): NetworkResult<Unit>

    suspend fun archiveEmployee(id: String): NetworkResult<Unit>

    // ── Génération de paie en masse ───────────────────────────────────────

    suspend fun runPayroll(mois: Int, annee: Int): NetworkResult<PayrollRunResult>

    suspend fun getPayrollByPeriode(mois: Int, annee: Int): NetworkResult<List<Payslip>>

    // ── Demandes de documents (self-service) ──────────────────────────────

    suspend fun getMesDemandesDocument(statut: String? = null): NetworkResult<List<DocumentRequest>>

    suspend fun createDemandeDocument(typeDocument: String, motifDemande: String?): NetworkResult<DocumentRequest>

    suspend fun cancelDemandeDocument(id: String): NetworkResult<Unit>

    // ── Demandes de documents (RH) ────────────────────────────────────────

    suspend fun getDemandesDocumentRH(statut: String? = null): NetworkResult<List<DocumentRequest>>

    suspend fun approuverDemandeDocument(id: String): NetworkResult<DocumentRequest>

    suspend fun rejeterDemandeDocument(id: String, motifRejet: String): NetworkResult<DocumentRequest>

    // ── Soldes de congés (admin) ──────────────────────────────────────────

    suspend fun getAdminLeaveBalances(annee: Int? = null): NetworkResult<List<AdminLeaveBalance>>

    // ── Stats RH ─────────────────────────────────────────────────────────

    suspend fun getStatsRH(): NetworkResult<StatsRH>

    suspend fun getMyPresences(mois: Int? = null, annee: Int? = null): NetworkResult<List<Presence>>
}
