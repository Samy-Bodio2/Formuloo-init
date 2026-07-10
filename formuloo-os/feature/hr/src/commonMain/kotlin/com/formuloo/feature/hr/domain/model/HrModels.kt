package com.formuloo.feature.hr.domain.model

enum class EmployeeStatus { ACTIVE, INACTIVE, ON_LEAVE, TERMINATED, SUSPENDED }
enum class EmployeeType { PERMANENT, CONTRACTUEL, STAGIAIRE, CONSULTANT }
enum class ContractType { CDI, CDD, INTERIM, STAGE, FREELANCE }
enum class LeaveStatus { PENDING, APPROVED, REJECTED, ANNULE }
enum class Gender { M, F, OTHER }

// ── Ticket FOR16A26-988 — congés / paie / organigramme ──────────────────
// LeaveStatus (PENDING/APPROVED/REJECTED/ANNULE) couvre déjà les 4 statuts
// attendus pour les demandes de congé — pas de doublon "LeaveRequestStatus".

enum class LeaveTypeCode { ANNUEL, MALADIE, MATERNITE, PATERNITE, SANS_SOLDE, EXCEPTIONNEL, RECUPERATION, FORMATION, DECES }
enum class PayslipStatus { BROUILLON, VALIDE, PAYE }
enum class DocumentRequestStatus { EN_ATTENTE, APPROUVEE, REJETEE, ANNULEE }
enum class DocumentType { ATTESTATION_TRAVAIL, ATTESTATION_SALAIRE, BULLETIN_PAIE_COPIE }

data class Employee(
    val id: String,
    val employeeNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val phonePerso: String? = null,
    val emailPerso: String? = null,
    val photoUrl: String?,
    val department: String?,
    val departmentId: String? = null,
    val position: String?,
    val positionId: String? = null,
    val managerName: String?,
    val managerId: String? = null,
    val hireDate: String,
    val status: EmployeeStatus,
    val employeeType: EmployeeType,
    val gender: Gender,
    val nationality: String?,
    val address: String?,
    val ville: String? = null,
    val numeroCnps: String?,
    val situationFamiliale: String?,
    val nombreEnfants: Int,
    val salaireBase: Double? = null,
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String get() = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
}

data class Contract(
    val id: String,
    val numero: String,
    val employeeId: String,
    val employeeName: String,
    val type: ContractType,
    val startDate: String,
    val endDate: String?,
    val grossSalary: Double,
    val currency: String,
    val workHoursPerWeek: Int,
    val trialPeriodDays: Int?,
    val isActive: Boolean,
    val documentUrl: String?,
    val signedAt: String?,
)

data class LeaveRequest(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val employeeInitials: String,
    val leaveTypeCode: String,
    val leaveTypeLabel: String,
    val startDate: String,
    val endDate: String,
    val days: Int,
    val reason: String?,
    val status: LeaveStatus,
    val approvedByName: String? = null,
    val approvedAt: String? = null,
    // Champs locaux uniquement — pour la sync offline (FOR16A26-988)
    val isPendingSync: Boolean = false,
    val localId: String? = null,
)

data class LeaveBalance(
    val typeConge: String,
    val annee: Int,
    val joursAcquis: Double,
    val joursPris: Double,
    val joursRestants: Double,
)

data class Payslip(
    val id: String,
    val period: String,
    val mois: Int,
    val annee: Int,
    val employeeId: String = "",
    val employeeName: String = "",
    val gross: Double,
    val primeTransport: Double,
    val primeLogement: Double,
    val primeRendement: Double,
    val autresPrimes: Double,
    val cotisationCnps: Double,
    val impotIrpp: Double,
    val creditLogement: Double,
    val autresDeductions: Double,
    val netSalary: Double,
    val currency: String,
    val status: PayslipStatus,
    val paidAt: String?,
    val pdfUrl: String?,
)

data class PayrollRunResult(
    val message: String,
    val nbEmployes: Int,
    val nbCrees: Int,
    val nbIgnores: Int,
)

data class DocumentRequest(
    val id: String,
    val typeDocument: DocumentType,
    val typeDocumentLabel: String,
    val statut: DocumentRequestStatus,
    val statutLabel: String,
    val motifDemande: String?,
    val motifRejet: String?,
    val hasDocumentData: Boolean,
    val traiteeLe: String?,
    val createdAt: String,
    val employeeId: String? = null,
    val employeeName: String? = null,
    val employeeInitials: String? = null,
)

data class AdminLeaveBalance(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val employeeInitials: String,
    val typeConge: String,
    val annee: Int,
    val joursAcquis: Double,
    val joursPris: Double,
    val joursRestants: Double,
)

data class StatsRH(
    val periodeReference: String,
    val totalEmployees: Int,
    val activeEmployees: Int,
    val inactiveEmployees: Int,
    val onLeaveEmployees: Int,
    val byDepartment: List<Pair<String, Int>>,
    val congesEnAttente: Int,
    val congesApprouvesCeMois: Int,
    val congesRejetesCeMois: Int,
    val joursPresenceCeMois: Int,
    val employesPresCeMois: Int,
    val masseSalarialeNette: Double?,
    val fichesBrouillon: Int,
    val fichesValidees: Int,
    val fichesPayees: Int,
    val devise: String,
)

data class OrgNode(
    val id: String,
    val nom: String,
    val code: String,
    val nbEmployes: Int,
    val children: List<OrgNode>,
    val depth: Int = 0,
)

enum class PresenceStatus { PRESENT, ABSENT, RETARD, CONGE, FERIE }

data class Presence(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val employeeInitials: String,
    val date: String,
    val heureArrivee: String?,
    val heureDepart: String?,
    val heuresTravaillees: String?,
    val heuresSupplementaires: String,
    val statut: PresenceStatus,
    val commentaire: String?,
    val createdAt: String,
)
