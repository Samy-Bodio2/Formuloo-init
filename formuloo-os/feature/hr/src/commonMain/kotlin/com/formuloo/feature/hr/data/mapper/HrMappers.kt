package com.formuloo.feature.hr.data.mapper

import com.formuloo.core.database.ContractEntity
import com.formuloo.core.database.EmployeeEntity
import com.formuloo.core.database.LeaveRequestEntity
import com.formuloo.core.database.PayslipEntity
import com.formuloo.core.network.dto.hr.CongeDto
import com.formuloo.core.network.dto.hr.ContratDto
import com.formuloo.core.network.dto.hr.DepartementTreeDto
import com.formuloo.core.network.dto.hr.EmployeDto
import com.formuloo.core.network.dto.hr.DemandeDocumentDto
import com.formuloo.core.network.dto.hr.PaieDto
import com.formuloo.core.network.dto.hr.PresenceDto
import com.formuloo.core.network.dto.hr.SoldeCongesDto
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.ContractType
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.domain.model.EmployeeType
import com.formuloo.feature.hr.domain.model.Gender
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.LeaveStatus
import com.formuloo.feature.hr.domain.model.LeaveTypeCode
import com.formuloo.feature.hr.domain.model.OrgNode
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayslipStatus
import com.formuloo.feature.hr.domain.model.AdminLeaveBalance
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.DocumentRequestStatus
import com.formuloo.feature.hr.domain.model.DocumentType
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PresenceStatus
import com.formuloo.feature.hr.domain.model.StatsRH
import com.formuloo.core.network.dto.hr.StatsRHDto
import kotlinx.serialization.json.JsonNull

// ── DTO → Domain ───────────────────────────────────────────────────────────

fun EmployeDto.toDomain(): Employee = Employee(
    id = id,
    employeeNumber = employeeNumber,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    phonePerso = phonePerso,
    emailPerso = emailPerso,
    photoUrl = photoUrl,
    department = department?.nom,
    departmentId = department?.id,
    position = position?.titre,
    positionId = position?.id,
    managerName = manager?.let { "${it.firstName} ${it.lastName}" },
    managerId = manager?.id,
    hireDate = hireDate,
    status = status.toEmployeeStatus(),
    employeeType = typeEmploye.toEmployeeType(),
    gender = gender.toGender(),
    nationality = nationality,
    address = address,
    ville = ville,
    numeroCnps = numeroCnps,
    situationFamiliale = situationFamiliale,
    nombreEnfants = nombreEnfants,
    salaireBase = salaireBase,
)

fun ContratDto.toDomain(): Contract = Contract(
    id = id,
    numero = numero,
    employeeId = employee.id,
    employeeName = "${employee.firstName} ${employee.lastName}",
    type = type.toContractType(),
    startDate = startDate,
    endDate = endDate,
    grossSalary = grossSalary.toDouble(),
    currency = currency,
    workHoursPerWeek = workHoursPerWeek.toDouble().toInt(),
    trialPeriodDays = trialPeriod,
    isActive = isActive,
    documentUrl = documentUrl,
    signedAt = signedAt,
)

fun CongeDto.toDomain(): LeaveRequest = LeaveRequest(
    id = id,
    employeeId = employee.id,
    employeeName = "${employee.firstName} ${employee.lastName}",
    employeeInitials = "${employee.firstName.firstOrNull() ?: ""}${employee.lastName.firstOrNull() ?: ""}".uppercase(),
    leaveTypeCode = type.code,
    leaveTypeLabel = type.libelle,
    startDate = startDate,
    endDate = endDate,
    days = days,
    reason = reason,
    status = status.toLeaveStatus(),
    approvedByName = approvedBy?.let { "${it.firstName} ${it.lastName}" },
    approvedAt = approvedAt,
)

fun SoldeCongesDto.toDomain(): LeaveBalance = LeaveBalance(
    typeConge = typeConge,
    annee = annee,
    joursAcquis = joursAcquis,
    joursPris = joursPris,
    joursRestants = joursRestants,
)

fun PaieDto.toDomain(): Payslip = Payslip(
    id = id,
    period = formatPeriodFr(mois, annee),
    mois = mois,
    annee = annee,
    employeeId = employee.id,
    employeeName = "${employee.firstName} ${employee.lastName}".trim(),
    gross = gross,
    primeTransport = bonuses.primeTransport,
    primeLogement = bonuses.primeLogement,
    primeRendement = bonuses.primeRendement,
    autresPrimes = bonuses.autres,
    cotisationCnps = deductions.cotisationCnps,
    impotIrpp = deductions.impotIrpp,
    creditLogement = deductions.creditLogement,
    autresDeductions = deductions.autres,
    netSalary = netSalary,
    currency = currency,
    status = statut.toPayslipStatus(),
    paidAt = paidAt,
    pdfUrl = pdfUrl,
)

fun DepartementTreeDto.toDomain(depth: Int = 0): OrgNode = OrgNode(
    id = id,
    nom = nom,
    code = code,
    nbEmployes = nbEmployes,
    children = sousDepartements.map { it.toDomain(depth + 1) },
    depth = depth,
)

// ── DTO → Entity ───────────────────────────────────────────────────────────

fun EmployeDto.toEntity(): EmployeeEntity = EmployeeEntity(
    id = id,
    employee_number = employeeNumber,
    first_name = firstName,
    last_name = lastName,
    email = email,
    phone = phone,
    photo_url = photoUrl,
    department_name = department?.nom,
    position_title = position?.titre,
    hire_date = hireDate,
    status = status,
    employee_type = typeEmploye,
    gender = gender,
    nationality = nationality,
    address = address,
    numero_cnps = numeroCnps,
    situation_familiale = situationFamiliale,
    nombre_enfants = nombreEnfants.toLong(),
    manager_name = manager?.let { "${it.firstName} ${it.lastName}" },
    updated_at = updatedAt,
)

fun ContratDto.toEntity(): ContractEntity = ContractEntity(
    id = id,
    numero = numero,
    employee_id = employee.id,
    employee_name = "${employee.firstName} ${employee.lastName}",
    type = type,
    start_date = startDate,
    end_date = endDate,
    gross_salary = grossSalary.toDouble(),
    currency = currency,
    work_hours_per_week = workHoursPerWeek.toDouble().toLong(),
    trial_period_days = trialPeriod?.toLong(),
    is_active = if (isActive) 1L else 0L,
    document_url = documentUrl,
    signed_at = signedAt,
    updated_at = updatedAt,
)

// ── Entity → Domain ────────────────────────────────────────────────────────

fun EmployeeEntity.toDomain(): Employee = Employee(
    id = id,
    employeeNumber = employee_number,
    firstName = first_name,
    lastName = last_name,
    email = email,
    phone = phone,
    photoUrl = photo_url,
    department = department_name,
    position = position_title,
    managerName = manager_name,
    hireDate = hire_date,
    status = status.toEmployeeStatus(),
    employeeType = employee_type.toEmployeeType(),
    gender = gender.toGender(),
    nationality = nationality,
    address = address,
    numeroCnps = numero_cnps,
    situationFamiliale = situation_familiale,
    nombreEnfants = nombre_enfants.toInt(),
)

fun ContractEntity.toDomain(): Contract = Contract(
    id = id,
    numero = numero,
    employeeId = employee_id,
    employeeName = employee_name,
    type = type.toContractType(),
    startDate = start_date,
    endDate = end_date,
    grossSalary = gross_salary,
    currency = currency,
    workHoursPerWeek = work_hours_per_week.toInt(),
    trialPeriodDays = trial_period_days?.toInt(),
    isActive = is_active == 1L,
    documentUrl = document_url,
    signedAt = signed_at,
)

fun LeaveRequestEntity.toDomain(): LeaveRequest = LeaveRequest(
    id = id,
    employeeId = employee_id,
    employeeName = employee_name,
    employeeInitials = employee_initials,
    leaveTypeCode = type_code,
    leaveTypeLabel = type_libelle,
    startDate = start_date,
    endDate = end_date,
    days = days.toInt(),
    reason = reason,
    status = status.toLeaveStatus(),
    approvedByName = approved_by_name,
    approvedAt = approved_at,
    isPendingSync = is_pending_sync == 1L,
    localId = if (is_pending_sync == 1L) id else null,
)

fun PayslipEntity.toDomain(): Payslip = Payslip(
    id = id,
    period = period,
    mois = mois.toInt(),
    annee = annee.toInt(),
    gross = gross,
    primeTransport = prime_transport,
    primeLogement = prime_logement,
    primeRendement = prime_rendement,
    autresPrimes = autres_primes,
    cotisationCnps = cotisation_cnps,
    impotIrpp = impot_irpp,
    creditLogement = credit_logement,
    autresDeductions = autres_deductions,
    netSalary = net_salary,
    currency = currency,
    status = status.toPayslipStatus(),
    paidAt = paid_at,
    pdfUrl = pdf_url,
)

// ── DTO → Entity (congés / paie) ────────────────────────────────────────

fun CongeDto.toEntity(): LeaveRequestEntity = LeaveRequestEntity(
    id = id,
    employee_id = employee.id,
    employee_name = "${employee.firstName} ${employee.lastName}",
    employee_initials = "${employee.firstName.firstOrNull() ?: ""}${employee.lastName.firstOrNull() ?: ""}".uppercase(),
    type_code = type.code,
    type_libelle = type.libelle,
    start_date = startDate,
    end_date = endDate,
    days = days.toLong(),
    reason = reason,
    status = status,
    approved_by_name = approvedBy?.let { "${it.firstName} ${it.lastName}" },
    approved_at = approvedAt,
    is_pending_sync = 0L,
    sync_attempts = 0L,
    created_at = createdAt,
)

/** [employeeId] est une clé technique locale ("me") — le contrat API filtre déjà par utilisateur courant. */
fun PaieDto.toEntity(employeeId: String): PayslipEntity = PayslipEntity(
    id = id,
    employee_id = employeeId,
    period = formatPeriodFr(mois, annee),
    mois = mois.toLong(),
    annee = annee.toLong(),
    gross = gross,
    prime_transport = bonuses.primeTransport,
    prime_logement = bonuses.primeLogement,
    prime_rendement = bonuses.primeRendement,
    autres_primes = bonuses.autres,
    cotisation_cnps = deductions.cotisationCnps,
    impot_irpp = deductions.impotIrpp,
    credit_logement = deductions.creditLogement,
    autres_deductions = deductions.autres,
    net_salary = netSalary,
    currency = currency,
    status = statut,
    paid_at = paidAt,
    pdf_url = pdfUrl,
    // Pas de Clock partagé multiplateforme disponible dans le projet actuellement —
    // colonne informative non utilisée par une politique d'éviction pour le moment.
    cached_at = "",
)

// ── String → Enum helpers ──────────────────────────────────────────────────

private fun String.toEmployeeStatus(): EmployeeStatus = when (this) {
    "active" -> EmployeeStatus.ACTIVE
    "inactive" -> EmployeeStatus.INACTIVE
    "on_leave" -> EmployeeStatus.ON_LEAVE
    "terminated" -> EmployeeStatus.TERMINATED
    "suspended" -> EmployeeStatus.SUSPENDED
    else -> EmployeeStatus.INACTIVE
}

private fun String.toEmployeeType(): EmployeeType = when (this) {
    "permanent" -> EmployeeType.PERMANENT
    "contractuel" -> EmployeeType.CONTRACTUEL
    "stagiaire" -> EmployeeType.STAGIAIRE
    "consultant" -> EmployeeType.CONSULTANT
    else -> EmployeeType.PERMANENT
}

private fun String.toContractType(): ContractType = when (this) {
    "CDI" -> ContractType.CDI
    "CDD" -> ContractType.CDD
    "Interim" -> ContractType.INTERIM
    "Stage" -> ContractType.STAGE
    "Freelance" -> ContractType.FREELANCE
    else -> ContractType.CDI
}

private fun String.toLeaveStatus(): LeaveStatus = when (this) {
    "pending" -> LeaveStatus.PENDING
    "approved" -> LeaveStatus.APPROVED
    "rejected" -> LeaveStatus.REJECTED
    "annule" -> LeaveStatus.ANNULE
    else -> LeaveStatus.PENDING
}

private fun String.toGender(): Gender = when (this) {
    "M" -> Gender.M
    "F" -> Gender.F
    else -> Gender.OTHER
}

private fun String.toPayslipStatus(): PayslipStatus = when (this) {
    "brouillon" -> PayslipStatus.BROUILLON
    "valide" -> PayslipStatus.VALIDE
    "paye" -> PayslipStatus.PAYE
    else -> PayslipStatus.BROUILLON
}

private val MONTHS_FR = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

private fun formatPeriodFr(mois: Int, annee: Int): String {
    val name = MONTHS_FR.getOrNull(mois - 1) ?: mois.toString()
    return "$name $annee"
}

// ── LeaveTypeCode ↔ code API (string) ───────────────────────────────────
// Le contrat hr.yaml encode les types de congé en snake_case français
// ("sans_solde", "annuel", ...) — non alignés sur les noms d'enum Kotlin.

fun LeaveTypeCode.toApiValue(): String = when (this) {
    LeaveTypeCode.ANNUEL -> "annuel"
    LeaveTypeCode.MALADIE -> "maladie"
    LeaveTypeCode.MATERNITE -> "maternite"
    LeaveTypeCode.PATERNITE -> "paternite"
    LeaveTypeCode.SANS_SOLDE -> "sans_solde"
    LeaveTypeCode.EXCEPTIONNEL -> "exceptionnel"
    LeaveTypeCode.RECUPERATION -> "recuperation"
    LeaveTypeCode.FORMATION -> "formation"
    LeaveTypeCode.DECES -> "deces"
}

fun LeaveTypeCode.toLibelleFr(): String = when (this) {
    LeaveTypeCode.ANNUEL -> "Congé annuel"
    LeaveTypeCode.MALADIE -> "Congé maladie"
    LeaveTypeCode.MATERNITE -> "Congé maternité"
    LeaveTypeCode.PATERNITE -> "Congé paternité"
    LeaveTypeCode.SANS_SOLDE -> "Congé sans solde"
    LeaveTypeCode.EXCEPTIONNEL -> "Congé exceptionnel"
    LeaveTypeCode.RECUPERATION -> "Récupération"
    LeaveTypeCode.FORMATION -> "Formation"
    LeaveTypeCode.DECES -> "Congé décès"
}

fun String.toLeaveTypeCode(): LeaveTypeCode = when (this) {
    "annuel" -> LeaveTypeCode.ANNUEL
    "maladie" -> LeaveTypeCode.MALADIE
    "maternite" -> LeaveTypeCode.MATERNITE
    "paternite" -> LeaveTypeCode.PATERNITE
    "sans_solde" -> LeaveTypeCode.SANS_SOLDE
    "exceptionnel" -> LeaveTypeCode.EXCEPTIONNEL
    "recuperation" -> LeaveTypeCode.RECUPERATION
    "formation" -> LeaveTypeCode.FORMATION
    "deces" -> LeaveTypeCode.DECES
    else -> LeaveTypeCode.ANNUEL
}

// ── Présences ──────────────────────────────────────────────────────────────

private fun String.toPresenceStatus(): PresenceStatus = when (this) {
    "absent" -> PresenceStatus.ABSENT
    "retard" -> PresenceStatus.RETARD
    "conge" -> PresenceStatus.CONGE
    "ferie" -> PresenceStatus.FERIE
    else -> PresenceStatus.PRESENT
}

fun PresenceDto.toDomain(): Presence = Presence(
    id = id,
    employeeId = employee.id,
    employeeName = "${employee.firstName} ${employee.lastName}".trim(),
    employeeInitials = "${employee.firstName.firstOrNull() ?: ""}${employee.lastName.firstOrNull() ?: ""}".uppercase(),
    date = date,
    heureArrivee = heureArrivee,
    heureDepart = heureDepart,
    heuresTravaillees = heuresTravaillees,
    heuresSupplementaires = heuresSupplementaires,
    statut = statut.toPresenceStatus(),
    commentaire = commentaire,
    createdAt = createdAt,
)

// ── Demandes de documents ──────────────────────────────────────────────────

private fun String.toDocumentType(): DocumentType = when (this) {
    "attestation_salaire" -> DocumentType.ATTESTATION_SALAIRE
    "bulletin_paie_copie" -> DocumentType.BULLETIN_PAIE_COPIE
    else -> DocumentType.ATTESTATION_TRAVAIL
}

private fun String.toDocumentRequestStatus(): DocumentRequestStatus = when (this) {
    "approuvee" -> DocumentRequestStatus.APPROUVEE
    "rejetee" -> DocumentRequestStatus.REJETEE
    "annulee" -> DocumentRequestStatus.ANNULEE
    else -> DocumentRequestStatus.EN_ATTENTE
}

fun DemandeDocumentDto.toDomain(): DocumentRequest = DocumentRequest(
    id = id,
    typeDocument = typeDocument.toDocumentType(),
    typeDocumentLabel = typeDocumentDisplay,
    statut = statut.toDocumentRequestStatus(),
    statutLabel = statutDisplay,
    motifDemande = motifDemande?.ifBlank { null },
    motifRejet = motifRejet?.ifBlank { null },
    hasDocumentData = documentData != null && documentData !is JsonNull,
    traiteeLe = traiteeLe,
    createdAt = createdAt,
    employeeId = employeeId,
    employeeName = employeeNom,
    employeeInitials = employeeNom?.trim()?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.joinToString("") { it.first().uppercaseChar().toString() }
        ?.take(2)
        ?.ifBlank { null },
)

fun SoldeCongesDto.toAdminDomain(): AdminLeaveBalance = AdminLeaveBalance(
    id = id,
    employeeId = employee.id,
    employeeName = "${employee.firstName} ${employee.lastName}".trim(),
    employeeInitials = "${employee.firstName.firstOrNull() ?: ""}${employee.lastName.firstOrNull() ?: ""}".uppercase(),
    typeConge = typeConge,
    annee = annee,
    joursAcquis = joursAcquis,
    joursPris = joursPris,
    joursRestants = joursRestants,
)

fun StatsRHDto.toDomain(): StatsRH = StatsRH(
    periodeReference = periodeReference,
    totalEmployees = effectifs.total,
    activeEmployees = effectifs.actifs,
    inactiveEmployees = effectifs.inactifs,
    onLeaveEmployees = effectifs.enConge,
    byDepartment = effectifs.parDepartement.map { it.departmentNom to it.count },
    congesEnAttente = conges.enAttente,
    congesApprouvesCeMois = conges.approuvesCeMois,
    congesRejetesCeMois = conges.rejetesCeMois,
    joursPresenceCeMois = presences.joursPresenceCeMois,
    employesPresCeMois = presences.employesPresCeMois,
    masseSalarialeNette = paie?.masseSalarialeNette,
    fichesBrouillon = paie?.fichesBrouillon ?: 0,
    fichesValidees = paie?.fichesValidees ?: 0,
    fichesPayees = paie?.fichesPayees ?: 0,
    devise = paie?.devise ?: "XAF",
)
