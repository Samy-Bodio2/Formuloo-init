package com.formuloo.feature.hr.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.sync.NetworkObserver
import com.formuloo.core.database.LeaveRequestEntity
import com.formuloo.core.network.api.HrRemoteDataSource
import com.formuloo.core.network.dto.hr.CongeApproveDto
import com.formuloo.core.network.dto.hr.CongeCreateDto
import com.formuloo.core.network.dto.hr.CongeRejectDto
import com.formuloo.core.network.dto.hr.ContratCreateDto
import com.formuloo.core.network.dto.hr.DepartementDto
import com.formuloo.core.network.dto.hr.EmployeCreateDto
import com.formuloo.core.network.dto.hr.EmployeUpdateDto
import com.formuloo.core.network.dto.hr.PaiePayerDto
import com.formuloo.core.network.dto.hr.PosteBriefDto
import com.formuloo.core.network.dto.hr.PresenceCreateDto
import com.formuloo.feature.hr.data.mapper.toDomain
import com.formuloo.feature.hr.data.mapper.toAdminDomain
import com.formuloo.feature.hr.data.mapper.toEntity
import com.formuloo.core.network.dto.hr.DemandeDocumentCreateDto
import com.formuloo.core.network.dto.hr.PayrollRunDto
import com.formuloo.core.network.dto.hr.RejeterDemandeDocumentDto
import com.formuloo.feature.hr.domain.model.AdminLeaveBalance
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PayrollRunResult
import com.formuloo.feature.hr.domain.model.StatsRH
import com.formuloo.feature.hr.data.source.local.HrLocalDataSource
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.OrgNode
import com.formuloo.feature.hr.domain.model.Payslip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class HrRepositoryImpl(
    private val remote: HrRemoteDataSource,
    private val local: HrLocalDataSource,
    private val networkObserver: NetworkObserver,
) : HrRepository {

    companion object {
        // Identité de l'employé courant non encore reliée à AuthRepository/UserProfile —
        // clé technique stable côté cache local (un seul utilisateur connecté par appareil).
        private const val ME = "me"
        private const val LOCAL_ID_PREFIX = "local-"
    }

    // ── Employés ──────────────────────────────────────────────────────────

    override fun getEmployees(
        search: String?,
        status: String?,
    ): Flow<NetworkResult<List<Employee>>> = flow {
        emit(NetworkResult.Loading)

        val cached = local.getCachedEmployees(search, status)
        if (cached.isNotEmpty()) {
            emit(NetworkResult.Success(cached.map { it.toDomain() }))
        }

        val result = remote.getEmployees(search = search, statut = status)
        when (result) {
            is NetworkResult.Success -> {
                local.replaceAllEmployees(result.data.results.map { it.toEntity() })
                val fresh = local.getCachedEmployees(null, null)
                emit(NetworkResult.Success(fresh.map { it.toDomain() }))
            }
            is NetworkResult.Error -> if (cached.isEmpty()) emit(result)
            else -> {}
        }
    }

    override suspend fun getEmployee(id: String): NetworkResult<Employee> {
        val cached = local.getCachedEmployee(id)
        val result = remote.getEmployee(id)
        if (result is NetworkResult.Success) {
            local.saveEmployee(result.data.toEntity())
            return NetworkResult.Success(result.data.toDomain())
        }
        if (cached != null) return NetworkResult.Success(cached.toDomain())
        return result as NetworkResult.Error
    }

    override suspend fun createEmployee(
        firstName: String, lastName: String, gender: String, email: String,
        phone: String, hireDate: String, status: String, typeEmploye: String,
        birthDate: String?, nationality: String?, nationalId: String?,
        situationFamiliale: String?, nombreEnfants: Int, numeroCnps: String?,
        address: String?, departmentId: String?, positionId: String?,
        managerId: String?, photoUrl: String?,
        userId: String?, salaireBase: Double?, ville: String?,
    ): NetworkResult<Employee> {
        val result = remote.createEmployee(
            EmployeCreateDto(
                firstName = firstName, lastName = lastName, gender = gender,
                email = email, phone = phone, hireDate = hireDate, status = status,
                typeEmploye = typeEmploye, birthDate = birthDate, nationality = nationality,
                nationalId = nationalId, situationFamiliale = situationFamiliale,
                nombreEnfants = nombreEnfants, numeroCnps = numeroCnps, address = address,
                departmentId = departmentId, positionId = positionId, managerId = managerId,
                photoUrl = photoUrl, userId = userId, salaireBase = salaireBase, ville = ville,
            )
        )
        if (result is NetworkResult.Success) {
            local.saveEmployee(result.data.toEntity())
            return NetworkResult.Success(result.data.toDomain())
        }
        return result as NetworkResult.Error
    }

    override suspend fun updateEmployee(
        id: String, firstName: String?, lastName: String?,
        phone: String?, phonePerso: String?, emailPerso: String?,
        address: String?, ville: String?, situationFamiliale: String?,
        nombreEnfants: Int?, departmentId: String?, positionId: String?,
        managerId: String?, status: String?, salaireBase: Double?,
        modePaiement: String?, numeroCompte: String?, banque: String?,
    ): NetworkResult<Employee> {
        val result = remote.updateEmployee(
            id = id,
            dto = EmployeUpdateDto(
                firstName = firstName, lastName = lastName,
                phone = phone, phonePerso = phonePerso, emailPerso = emailPerso,
                address = address, ville = ville,
                situationFamiliale = situationFamiliale, nombreEnfants = nombreEnfants,
                departmentId = departmentId, positionId = positionId, managerId = managerId,
                status = status, salaireBase = salaireBase,
                modePaiement = modePaiement, numeroCompte = numeroCompte, banque = banque,
            ),
        )
        if (result is NetworkResult.Success) {
            local.saveEmployee(result.data.toEntity())
            return NetworkResult.Success(result.data.toDomain())
        }
        return result as NetworkResult.Error
    }

    // ── Contrats ──────────────────────────────────────────────────────────

    override fun getContracts(employeeId: String): Flow<NetworkResult<List<Contract>>> = flow {
        emit(NetworkResult.Loading)

        val cached = local.getCachedContracts(employeeId)
        if (cached.isNotEmpty()) {
            emit(NetworkResult.Success(cached.map { it.toDomain() }))
        }

        val result = remote.getContrats(employeId = employeeId)
        when (result) {
            is NetworkResult.Success -> {
                local.replaceContractsByEmployee(employeeId, result.data.results.map { it.toEntity() })
                val fresh = local.getCachedContracts(employeeId)
                emit(NetworkResult.Success(fresh.map { it.toDomain() }))
            }
            is NetworkResult.Error -> if (cached.isEmpty()) emit(result)
            else -> {}
        }
    }

    override suspend fun createContract(
        employeId: String, type: String, startDate: String, endDate: String?,
        grossSalary: Double, currency: String, workHoursPerWeek: Int,
        trialPeriod: Int?, documentUrl: String?, signedAt: String?,
    ): NetworkResult<Contract> {
        val result = remote.createContrat(
            ContratCreateDto(
                employeId = employeId, type = type, startDate = startDate,
                endDate = endDate, grossSalary = grossSalary, currency = currency,
                workHoursPerWeek = workHoursPerWeek, trialPeriod = trialPeriod,
                documentUrl = documentUrl, signedAt = signedAt,
            )
        )
        if (result is NetworkResult.Success) {
            local.saveContract(result.data.toEntity())
            return NetworkResult.Success(result.data.toDomain())
        }
        return result as NetworkResult.Error
    }

    // ── Congés (pas de cache local) ───────────────────────────────────────

    override suspend fun getPendingLeaves(): NetworkResult<List<LeaveRequest>> {
        val result = remote.getLeaves(statut = "pending")
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun approveLeave(id: String, commentaire: String?): NetworkResult<LeaveRequest> {
        val result = remote.approveLeave(id, CongeApproveDto(commentaire = commentaire))
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun rejectLeave(id: String, reason: String): NetworkResult<LeaveRequest> {
        val result = remote.rejectLeave(id, CongeRejectDto(reason = reason))
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Congés — FOR16A26-988 (offline-first + sync) ────────────────────────

    override fun getMyLeaves(): Flow<NetworkResult<List<LeaveRequest>>> = flow {
        emit(NetworkResult.Loading)

        val cached = local.getCachedLeaves()
        if (cached.isNotEmpty()) {
            emit(NetworkResult.Success(cached.map { it.toDomain() }))
        }

        val result = remote.getLeaves()
        when (result) {
            is NetworkResult.Success -> {
                local.replaceSyncedLeaves(result.data.results.map { it.toEntity() })
                val fresh = local.getCachedLeaves()
                emit(NetworkResult.Success(fresh.map { it.toDomain() }))
            }
            is NetworkResult.Error -> if (cached.isEmpty()) emit(result)
            else -> {}
        }
    }

    override suspend fun getLeaveBalance(employeeId: String?, annee: Int?): NetworkResult<List<LeaveBalance>> {
        val result = remote.getLeaveBalance(employeId = employeeId, annee = annee)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun requestLeave(
        typeCode: String,
        startDate: String,
        endDate: String,
        reason: String?,
    ): NetworkResult<LeaveRequest> {
        if (!networkObserver.isOnline.value) {
            val entity = LeaveRequestEntity(
                id = "$LOCAL_ID_PREFIX${Random.nextLong().toString(16)}",
                employee_id = ME,
                employee_name = "Vous",
                employee_initials = "",
                type_code = typeCode,
                type_libelle = typeCode,
                start_date = startDate,
                end_date = endDate,
                days = 0L,
                reason = reason,
                status = "pending",
                approved_by_name = null,
                approved_at = null,
                is_pending_sync = 1L,
                sync_attempts = 0L,
                created_at = startDate,
            )
            local.saveLeave(entity)
            return NetworkResult.Success(entity.toDomain())
        }

        val result = remote.createLeaveRequest(
            CongeCreateDto(typeConge = typeCode, startDate = startDate, endDate = endDate, reason = reason)
        )
        if (result is NetworkResult.Success) {
            local.saveLeave(result.data.toEntity())
            return NetworkResult.Success(result.data.toDomain())
        }
        return result as NetworkResult.Error
    }

    override suspend fun cancelLeave(id: String): NetworkResult<Unit> {
        if (id.startsWith(LOCAL_ID_PREFIX)) {
            local.deleteLeave(id)
            return NetworkResult.Success(Unit)
        }
        val result = remote.cancelLeaveRequest(id)
        if (result is NetworkResult.Success) local.deleteLeave(id)
        return result
    }

    override fun getTeamPendingLeaves(): Flow<NetworkResult<List<LeaveRequest>>> = flow {
        emit(NetworkResult.Loading)
        val result = remote.getLeaves(statut = "pending")
        emit(
            when (result) {
                is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
                is NetworkResult.Error -> result
                else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
            }
        )
    }

    override suspend fun approveLeave(id: String): NetworkResult<LeaveRequest> = approveLeave(id, null)

    // ── Paie ──────────────────────────────────────────────────────────────

    override fun getMyPayslips(annee: Int?): Flow<NetworkResult<List<Payslip>>> = flow {
        emit(NetworkResult.Loading)

        val cached = local.getCachedPayslips(ME, annee)
        if (cached.isNotEmpty()) {
            emit(NetworkResult.Success(cached.map { it.toDomain() }))
        }

        val result = remote.getPayslips(annee = annee)
        when (result) {
            is NetworkResult.Success -> {
                val entities = result.data.results.map { it.toEntity(ME) }
                local.savePayslips(entities)
                emit(NetworkResult.Success(entities.map { it.toDomain() }))
            }
            is NetworkResult.Error -> if (cached.isEmpty()) emit(result)
            else -> {}
        }
    }

    override suspend fun getPayslipDetail(id: String): NetworkResult<Payslip> {
        val result = remote.getPayslip(id)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun validatePayslip(id: String): NetworkResult<Payslip> {
        val result = remote.validatePayslip(id)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun payPayslip(id: String, modePaiement: String): NetworkResult<Payslip> {
        val result = remote.payPayslip(id, PaiePayerDto(modePaiement = modePaiement))
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun downloadPayslipPdf(id: String): NetworkResult<ByteArray> =
        remote.downloadPayslipPdf(id)

    override suspend fun getPayslipsForEmployee(employeeId: String): NetworkResult<List<Payslip>> {
        val result = remote.getPayslips(employeId = employeeId)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Organigramme ─────────────────────────────────────────────────────

    override suspend fun getOrganizationTree(): NetworkResult<List<OrgNode>> {
        val result = remote.getOrganizationTree()
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Référentiels ─────────────────────────────────────────────────────

    override suspend fun getDepartements(): NetworkResult<List<DepartementDto>> {
        val result = remote.getDepartements()
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results)
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun getPostes(): NetworkResult<List<PosteBriefDto>> {
        val result = remote.getPostes()
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results)
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Présences ─────────────────────────────────────────────────────────

    override suspend fun getPresences(
        employeId: String?,
        statut: String?,
        dateDebut: String?,
        dateFin: String?,
    ): NetworkResult<List<Presence>> {
        val result = remote.getPresences(
            employeId = employeId,
            statut = statut,
            dateDebut = dateDebut,
            dateFin = dateFin,
        )
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun createPresence(
        employeId: String,
        date: String,
        heureArrivee: String?,
        heureDepart: String?,
        heuresSupplementaires: Double?,
        statut: String,
        commentaire: String?,
    ): NetworkResult<Presence> {
        val result = remote.createPresence(
            PresenceCreateDto(
                employeId = employeId,
                date = date,
                heureArrivee = heureArrivee?.takeIf { it.isNotBlank() },
                heureDepart = heureDepart?.takeIf { it.isNotBlank() },
                heuresSupplementaires = heuresSupplementaires,
                statut = statut,
                commentaire = commentaire?.takeIf { it.isNotBlank() },
            )
        )
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun updatePresence(
        id: String,
        heureArrivee: String?,
        heureDepart: String?,
        statut: String?,
        commentaire: String?,
    ): NetworkResult<Presence> {
        val dto = PresenceCreateDto(
            employeId = "",
            date = "",
            heureArrivee = heureArrivee?.takeIf { it.isNotBlank() },
            heureDepart = heureDepart?.takeIf { it.isNotBlank() },
            statut = statut ?: "present",
            commentaire = commentaire?.takeIf { it.isNotBlank() },
        )
        val result = remote.updatePresence(id, dto)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun archivePresence(id: String): NetworkResult<Unit> =
        remote.archivePresence(id)

    override suspend fun getMyPresences(mois: Int?, annee: Int?): NetworkResult<List<Presence>> {
        val result = remote.getMyPresences(mois = mois, annee = annee)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun archiveEmployee(id: String): NetworkResult<Unit> =
        remote.archiveEmployee(id)

    override suspend fun runPayroll(mois: Int, annee: Int): NetworkResult<PayrollRunResult> {
        val result = remote.runPayroll(PayrollRunDto(mois = mois, annee = annee))
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(
                PayrollRunResult(
                    message = result.data.message,
                    nbEmployes = result.data.nbEmployes,
                    nbCrees = result.data.nbCrees,
                    nbIgnores = result.data.nbIgnores,
                ),
                result.code,
            )
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun getPayrollByPeriode(mois: Int, annee: Int): NetworkResult<List<Payslip>> {
        val periode = "%04d-%02d".format(annee, mois)
        val result = remote.getPayrollByPeriode(periode)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun getMesDemandesDocument(statut: String?): NetworkResult<List<DocumentRequest>> {
        val result = remote.getMesDemandesDocument(statut)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun createDemandeDocument(typeDocument: String, motifDemande: String?): NetworkResult<DocumentRequest> {
        val result = remote.createDemandeDocument(
            DemandeDocumentCreateDto(
                typeDocument = typeDocument,
                motifDemande = motifDemande?.ifBlank { null },
            )
        )
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun cancelDemandeDocument(id: String): NetworkResult<Unit> =
        remote.cancelDemandeDocument(id)

    // ── Demandes de documents (RH) ─────────────────────────────────────────

    override suspend fun getDemandesDocumentRH(statut: String?): NetworkResult<List<DocumentRequest>> {
        val result = remote.getDemandesDocumentRH(statut = statut)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun approuverDemandeDocument(id: String): NetworkResult<DocumentRequest> {
        val result = remote.approuverDemandeDocument(id)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    override suspend fun rejeterDemandeDocument(id: String, motifRejet: String): NetworkResult<DocumentRequest> {
        val result = remote.rejeterDemandeDocument(id, RejeterDemandeDocumentDto(motifRejet = motifRejet))
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Soldes de congés (admin) ───────────────────────────────────────────

    override suspend fun getAdminLeaveBalances(annee: Int?): NetworkResult<List<AdminLeaveBalance>> {
        val result = remote.getLeaveBalance(employeId = null, annee = annee)
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.results.map { it.toAdminDomain() })
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }

    // ── Stats RH ──────────────────────────────────────────────────────────

    override suspend fun getStatsRH(): NetworkResult<StatsRH> {
        val result = remote.getStatsRH()
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            else -> NetworkResult.Error("Erreur inattendue. Veuillez réessayer.")
        }
    }
}
