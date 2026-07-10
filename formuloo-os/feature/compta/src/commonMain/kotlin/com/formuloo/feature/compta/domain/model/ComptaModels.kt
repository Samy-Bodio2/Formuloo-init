package com.formuloo.feature.compta.domain.model

enum class InvoiceStatus { BROUILLON, EMISE, PARTIELLEMENT_PAYEE, PAYEE, ANNULEE }
enum class PurchaseInvoiceStatus { BROUILLON, RECUE, VALIDEE, PARTIELLEMENT_PAYEE, PAYEE, ANNULEE }
enum class PaymentMode { VIREMENT, CHEQUE, ESPECES, MOBILE_MONEY }

data class InvoiceLine(
    val id: Int,
    val description: String,
    val quantite: Double,
    val prixUnitaire: Double,
    val montantTotal: Double,
)

data class Invoice(
    val id: Int,
    val typeDocument: String = "FACTURE",
    val factureOrigineId: Int? = null,
    val numero: String,
    val clientNom: String,
    val clientEmail: String?,
    val lignes: List<InvoiceLine>,
    val montantHt: Double,
    val tvaTaux: Double,
    val tva: Double,
    val montantTtc: Double,
    val devise: String,
    val statut: InvoiceStatus,
    val dateEmission: String?,
    val dateEcheance: String,
    val createdAt: String,
) {
    val isAvoir: Boolean get() = typeDocument == "AVOIR"
    val canEmetAvoir: Boolean get() = !isAvoir && statut in listOf(InvoiceStatus.EMISE, InvoiceStatus.PARTIELLEMENT_PAYEE)
}

data class PurchaseInvoiceLine(
    val id: Int,
    val description: String,
    val quantite: Double,
    val prixUnitaire: Double,
    val montantTotal: Double,
)

data class PurchaseInvoice(
    val id: Int,
    val typeDocument: String = "FACTURE",
    val factureOrigineId: Int? = null,
    val numeroInterne: String,
    val numeroFournisseur: String?,
    val fournisseurNom: String,
    val fournisseurEmail: String?,
    val lignes: List<PurchaseInvoiceLine>,
    val montantHt: Double,
    val tvaTaux: Double,
    val tva: Double,
    val montantTtc: Double,
    val devise: String,
    val statut: PurchaseInvoiceStatus,
    val dateReception: String?,
    val dateFacture: String,
    val dateEcheance: String,
    val createdAt: String,
) {
    val isAvoir: Boolean get() = typeDocument == "AVOIR"
    val canEmetAvoir: Boolean get() = !isAvoir && statut in listOf(PurchaseInvoiceStatus.VALIDEE, PurchaseInvoiceStatus.PARTIELLEMENT_PAYEE)
}

data class Payment(
    val id: Int,
    val factureId: Int,
    val montant: Double,
    val devise: String,
    val modePaiement: PaymentMode,
    val datePaiement: String,
    val reference: String?,
    val createdAt: String,
)

data class SupplierPayment(
    val id: Int,
    val factureFournisseurId: Int,
    val factureNumero: String?,
    val fournisseurNom: String?,
    val montant: Double,
    val devise: String,
    val modePaiement: PaymentMode,
    val datePaiement: String,
    val reference: String?,
    val createdAt: String,
)

data class ComptaPage<T>(
    val items: List<T>,
    val totalCount: Int,
    val hasNext: Boolean,
)

// ── PLAN COMPTABLE ────────────────────────────────────────────────────────────

data class CompteItemUi(
    val id: Int,
    val numero: String,
    val libelle: String,
    val soldeDebiteur: Double,
    val soldeCrediteur: Double,
    val isActif: Boolean,
) {
    val soldeNet: Double get() = soldeDebiteur - soldeCrediteur
    val isDebiteur: Boolean get() = soldeDebiteur >= soldeCrediteur
    val soldeAbsolu: Double get() = if (isDebiteur) soldeDebiteur else soldeCrediteur
}

data class SousGroupeGroup(
    val prefixe: String,
    val libelle: String,
    val total: Double,
    val comptes: List<CompteItemUi>,
)

data class ClasseGroup(
    val numero: Int,
    val libelle: String,
    val total: Double,
    val sousGroupes: List<SousGroupeGroup>,
)

data class PlanComptableStats(
    val nbComptesActifs: Int,
    val totalActif: Double,
    val tresorerieNette: Double,
    val dateAt: String,
    val exerciceAnnee: Int?,
    val devise: String,
)

data class DashboardStats(
    val date: String,
    val devise: String,
    val exerciceId: Int?,
    val exerciceAnnee: Int?,
    val caMois: Double,
    val nbFacturesMois: Int,
    val nbImpayees: Int,
    val montantImpaye: Double,
    val nbImpayeesEnRetard: Int,
    val montantImpayeEnRetard: Double,
    val nbAchatsEnAttente: Int,
    val montantAchatsEnAttente: Double,
    val nbAchatsEnRetard: Int,
    val chargesMois: Double,
    val resultatPrevisionnelExercice: Double,
    val soldeTresorerie: Double,
)
