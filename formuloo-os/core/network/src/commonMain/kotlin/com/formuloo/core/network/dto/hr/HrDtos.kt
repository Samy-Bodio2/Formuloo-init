package com.formuloo.core.network.dto.hr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── PAGINATION ─────────────────────────────────────────────────────────────

@Serializable
data class PaginatedResponse<T>(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>,
)

// ── DÉPARTEMENT ────────────────────────────────────────────────────────────

@Serializable
data class DepartementBriefDto(
    val id: String,
    val nom: String,
    val code: String,
)

@Serializable
data class DepartementDto(
    val id: String,
    val nom: String,
    val code: String,
    val description: String? = null,
    val parent: DepartementBriefDto? = null,
    val responsable: EmployeBriefDto? = null,
    val budget: Double? = null,
    val devise: String = "XAF",
    @SerialName("nb_employes") val nbEmployes: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class DepartementTreeDto(
    val id: String,
    val nom: String,
    val code: String,
    @SerialName("nb_employes") val nbEmployes: Int = 0,
    @SerialName("sous_departements") val sousDepartements: List<DepartementTreeDto> = emptyList(),
)

// ── POSTE ──────────────────────────────────────────────────────────────────

@Serializable
data class PosteBriefDto(
    val id: String,
    val titre: String,
    val code: String,
    val niveau: String,
)

// ── EMPLOYÉ ────────────────────────────────────────────────────────────────

@Serializable
data class EmployeBriefDto(
    val id: String,
    @SerialName("employee_number") val employeeNumber: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
)

@Serializable
data class EmployeDto(
    val id: String,
    @SerialName("employee_number") val employeeNumber: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("birth_date") val birthDate: String? = null,
    val gender: String,
    val nationality: String? = null,
    @SerialName("national_id") val nationalId: String? = null,
    @SerialName("situation_familiale") val situationFamiliale: String? = null,
    @SerialName("nombre_enfants") val nombreEnfants: Int = 0,
    @SerialName("numero_cnps") val numeroCnps: String? = null,
    val address: String? = null,
    val phone: String,
    @SerialName("phone_perso") val phonePerso: String? = null,
    val email: String,
    @SerialName("email_perso") val emailPerso: String? = null,
    val ville: String? = null,
    val department: DepartementBriefDto? = null,
    val position: PosteBriefDto? = null,
    val manager: EmployeBriefDto? = null,
    @SerialName("hire_date") val hireDate: String,
    val status: String,
    @SerialName("type_employe") val typeEmploye: String,
    @SerialName("salaire_base") val salaireBase: Double? = null,
    @SerialName("mode_paiement") val modePaiement: String? = null,
    @SerialName("numero_compte") val numeroCompte: String? = null,
    val banque: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class EmployeCreateDto(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val gender: String,
    val email: String,
    val phone: String,
    @SerialName("hire_date") val hireDate: String,
    val status: String = "active",
    @SerialName("type_employe") val typeEmploye: String = "permanent",
    @SerialName("birth_date") val birthDate: String? = null,
    val nationality: String? = null,
    @SerialName("national_id") val nationalId: String? = null,
    @SerialName("situation_familiale") val situationFamiliale: String? = null,
    @SerialName("nombre_enfants") val nombreEnfants: Int = 0,
    @SerialName("numero_cnps") val numeroCnps: String? = null,
    val address: String? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("position_id") val positionId: String? = null,
    @SerialName("manager_id") val managerId: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("salaire_base") val salaireBase: Double? = null,
    val ville: String? = null,
)

@Serializable
data class EmployeUpdateDto(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val phone: String? = null,
    @SerialName("phone_perso") val phonePerso: String? = null,
    @SerialName("email_perso") val emailPerso: String? = null,
    val address: String? = null,
    val ville: String? = null,
    @SerialName("situation_familiale") val situationFamiliale: String? = null,
    @SerialName("nombre_enfants") val nombreEnfants: Int? = null,
    @SerialName("department_id") val departmentId: String? = null,
    @SerialName("position_id") val positionId: String? = null,
    @SerialName("manager_id") val managerId: String? = null,
    val status: String? = null,
    @SerialName("salaire_base") val salaireBase: Double? = null,
    @SerialName("mode_paiement") val modePaiement: String? = null,
    @SerialName("numero_compte") val numeroCompte: String? = null,
    val banque: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("cv_url") val cvUrl: String? = null,
)

// ── CONTRAT ────────────────────────────────────────────────────────────────

@Serializable
data class ContratDto(
    val id: String,
    val numero: String,
    val employee: EmployeBriefDto,
    val type: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    // DRF sérialise les DecimalField en chaînes (ex: "3200000.00") — pas en nombre JSON brut.
    @SerialName("gross_salary") val grossSalary: String,
    val currency: String = "XAF",
    @SerialName("work_hours_per_week") val workHoursPerWeek: String = "40",
    @SerialName("trial_period") val trialPeriod: Int? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("document_url") val documentUrl: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ContratCreateDto(
    @SerialName("employe_id") val employeId: String,
    val type: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("gross_salary") val grossSalary: Double,
    val currency: String = "XAF",
    @SerialName("work_hours_per_week") val workHoursPerWeek: Int = 40,
    @SerialName("trial_period") val trialPeriod: Int? = null,
    @SerialName("document_url") val documentUrl: String? = null,
    @SerialName("signed_at") val signedAt: String? = null,
)

// ── CONGÉ ──────────────────────────────────────────────────────────────────

/** Conforme au schéma LeaveType du hr.yaml v2.1.0 */
@Serializable
data class CongeTypeDto(
    val code: String,
    val libelle: String,
)

@Serializable
data class CongeDto(
    val id: String,
    val employee: EmployeBriefDto,
    val type: CongeTypeDto,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val days: Int,
    val reason: String? = null,
    val status: String,
    @SerialName("approved_by") val approvedBy: EmployeBriefDto? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)

/** Le champ s'appelle "commentaire" dans le contrat hr.yaml v2.1.0 */
@Serializable
data class CongeApproveDto(val commentaire: String? = null)

@Serializable
data class CongeRejectDto(val reason: String)

/** Corps de POST /hr/leaves/ — conforme au schéma CongeCreate du hr.yaml v2.1.0 */
@Serializable
data class CongeCreateDto(
    @SerialName("type_conge") val typeConge: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val reason: String? = null,
)

// ── SOLDE DE CONGÉS ──────────────────────────────────────────────────────
// Conforme au schéma SoldeConges du hr.yaml v2.1.0 — endpoint /hr/soldes-conges/

@Serializable
data class SoldeCongesDto(
    val id: String,
    val employee: EmployeBriefDto,
    @SerialName("type_conge") val typeConge: String,
    val annee: Int,
    @SerialName("jours_acquis") val joursAcquis: Double,
    @SerialName("jours_pris") val joursPris: Double,
    @SerialName("jours_restants") val joursRestants: Double,
)

// ── FICHE DE PAIE ────────────────────────────────────────────────────────
// Conforme au schéma Paie du hr.yaml v2.1.0 — endpoint /hr/payroll/

@Serializable
data class PaieBonusesDto(
    @SerialName("prime_transport") val primeTransport: Double = 0.0,
    @SerialName("prime_logement") val primeLogement: Double = 0.0,
    @SerialName("prime_rendement") val primeRendement: Double = 0.0,
    val autres: Double = 0.0,
)

@Serializable
data class PaieDeductionsDto(
    @SerialName("cotisation_cnps") val cotisationCnps: Double = 0.0,
    @SerialName("impot_irpp") val impotIrpp: Double = 0.0,
    @SerialName("credit_logement") val creditLogement: Double = 0.0,
    val autres: Double = 0.0,
)

@Serializable
data class PaieDto(
    val id: String,
    val employee: EmployeBriefDto,
    val period: String,
    val mois: Int,
    val annee: Int,
    val gross: Double,
    val bonuses: PaieBonusesDto,
    val deductions: PaieDeductionsDto,
    @SerialName("net_salary") val netSalary: Double,
    val currency: String = "XAF",
    val statut: String,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("pdf_url") val pdfUrl: String? = null,
)

@Serializable
data class PaiePayerDto(
    @SerialName("mode_paiement") val modePaiement: String,
)

@Serializable
data class PayrollRunDto(
    val mois: Int,
    val annee: Int,
)

@Serializable
data class PayrollRunResultDto(
    val message: String,
    @SerialName("nb_employes") val nbEmployes: Int,
    @SerialName("nb_crees") val nbCrees: Int,
    @SerialName("nb_ignores") val nbIgnores: Int,
)

@Serializable
data class PayrollPeriodeResponseDto(
    val count: Int,
    val periode: String,
    val results: List<PaieDto>,
)

// ── DEMANDES DE DOCUMENTS ─────────────────────────────────────────────────

@Serializable
data class DemandeDocumentDto(
    val id: String,
    @SerialName("type_document") val typeDocument: String,
    @SerialName("type_document_display") val typeDocumentDisplay: String,
    val statut: String,
    @SerialName("statut_display") val statutDisplay: String,
    @SerialName("motif_demande") val motifDemande: String? = null,
    @SerialName("motif_rejet") val motifRejet: String? = null,
    @SerialName("document_data") val documentData: JsonElement? = null,
    @SerialName("traitee_le") val traiteeLe: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("employee_nom") val employeeNom: String? = null,
)

@Serializable
data class RejeterDemandeDocumentDto(
    @SerialName("motif_rejet") val motifRejet: String,
)

@Serializable
data class DemandeDocumentCreateDto(
    @SerialName("type_document") val typeDocument: String,
    @SerialName("motif_demande") val motifDemande: String? = null,
)

@Serializable
data class MesDemandesDocumentResponseDto(
    val count: Int,
    val results: List<DemandeDocumentDto>,
)

// ── PRÉSENCES ─────────────────────────────────────────────────────────────

@Serializable
data class PresenceEmployeeDto(
    val id: String,
    @SerialName("employee_number") val employeeNumber: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
)

@Serializable
data class PresenceDto(
    val id: String,
    val employee: PresenceEmployeeDto,
    val date: String,
    @SerialName("heure_arrivee") val heureArrivee: String? = null,
    @SerialName("heure_depart") val heureDepart: String? = null,
    @SerialName("heures_travaillees") val heuresTravaillees: String? = null,
    @SerialName("heures_supplementaires") val heuresSupplementaires: String = "0.00",
    val statut: String,
    val commentaire: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class PresenceCreateDto(
    @SerialName("employe_id") val employeId: String,
    val date: String,
    @SerialName("heure_arrivee") val heureArrivee: String? = null,
    @SerialName("heure_depart") val heureDepart: String? = null,
    @SerialName("heures_supplementaires") val heuresSupplementaires: Double? = null,
    val statut: String = "present",
    val commentaire: String? = null,
)

/** Wrapper non-standard de GET /me/presences/ — diff. de PaginatedResponse (pas de next/previous). */
@Serializable
data class MesPresencesResponseDto(
    val count: Int,
    val periode: String,
    val results: List<PresenceDto>,
)

// ── STATS RH ─────────────────────────────────────────────────────────────────

@Serializable
data class EffectifParTypeDto(
    @SerialName("type_employe") val typeEmploye: String,
    val count: Int,
)

@Serializable
data class EffectifParDeptDto(
    @SerialName("department__nom") val departmentNom: String,
    val count: Int,
)

@Serializable
data class StatsEffectifsDto(
    val total: Int,
    val actifs: Int,
    val inactifs: Int,
    @SerialName("en_conge") val enConge: Int,
    @SerialName("par_type") val parType: List<EffectifParTypeDto>,
    @SerialName("par_departement") val parDepartement: List<EffectifParDeptDto>,
)

@Serializable
data class StatsCongesDto(
    @SerialName("en_attente") val enAttente: Int,
    @SerialName("approuves_ce_mois") val approuvesCeMois: Int,
    @SerialName("rejetes_ce_mois") val rejetesCeMois: Int,
)

@Serializable
data class StatsPresencesDto(
    @SerialName("jours_presence_ce_mois") val joursPresenceCeMois: Int,
    @SerialName("employes_presents_ce_mois") val employesPresCeMois: Int,
)

@Serializable
data class StatsPaieDto(
    val periode: String,
    @SerialName("fiches_brouillon") val fichesBrouillon: Int,
    @SerialName("fiches_validees") val fichesValidees: Int,
    @SerialName("fiches_payees") val fichesPayees: Int,
    @SerialName("masse_salariale_nette") val masseSalarialeNette: Double,
    val devise: String,
)

@Serializable
data class StatsRHDto(
    @SerialName("periode_reference") val periodeReference: String,
    val effectifs: StatsEffectifsDto,
    val conges: StatsCongesDto,
    val presences: StatsPresencesDto,
    val paie: StatsPaieDto? = null,
)
