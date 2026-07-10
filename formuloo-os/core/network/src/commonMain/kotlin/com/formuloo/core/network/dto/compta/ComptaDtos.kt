package com.formuloo.core.network.dto.compta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// PaginatedResponse<T> est réutilisé depuis com.formuloo.core.network.dto.hr (même module
// core:network, pas de cycle inter-module) — voir ComptaApiService.kt.

// ── FACTURES CLIENTS (ventes) ───────────────────────────────────────────────
// Conforme à comptabilite/serializers/facture.py — endpoint /api/v1/compta/factures/

@Serializable
data class LigneFactureDto(
    val id: Int,
    val description: String,
    val quantite: Double,
    @SerialName("prix_unitaire") val prixUnitaire: Double,
    @SerialName("montant_total") val montantTotal: Double,
)

@Serializable
data class LigneFactureCreateDto(
    val description: String,
    val quantite: Double = 1.0,
    @SerialName("prix_unitaire") val prixUnitaire: Double,
)

@Serializable
data class FactureDto(
    val id: Int,
    @SerialName("type_document") val typeDocument: String,
    @SerialName("facture_origine") val factureOrigine: Int? = null,
    val numero: String,
    @SerialName("client_nom") val clientNom: String,
    @SerialName("client_email") val clientEmail: String? = null,
    val lignes: List<LigneFactureDto> = emptyList(),
    @SerialName("montant_ht") val montantHt: Double,
    @SerialName("tva_taux") val tvaTaux: Double,
    val tva: Double,
    @SerialName("montant_ttc") val montantTtc: Double,
    val devise: String = "XAF",
    val statut: String,
    @SerialName("date_emission") val dateEmission: String? = null,
    @SerialName("date_echeance") val dateEcheance: String,
    @SerialName("ecriture_id") val ecritureId: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class FactureCreateDto(
    @SerialName("client_nom") val clientNom: String,
    @SerialName("client_email") val clientEmail: String? = null,
    val lignes: List<LigneFactureCreateDto>,
    val devise: String = "XAF",
    @SerialName("tva_taux") val tvaTaux: Double = 0.0,
    @SerialName("date_echeance") val dateEcheance: String,
)

// ── FACTURES FOURNISSEURS (achats) ──────────────────────────────────────────
// Conforme à comptabilite/serializers/facture_fournisseur.py — endpoint /api/v1/compta/achats/

@Serializable
data class LigneFactureFournisseurDto(
    val id: Int,
    val description: String,
    @SerialName("compte_charge") val compteCharge: Int? = null,
    val quantite: Double,
    @SerialName("prix_unitaire") val prixUnitaire: Double,
    @SerialName("montant_total") val montantTotal: Double,
)

@Serializable
data class LigneFactureFournisseurCreateDto(
    val description: String,
    @SerialName("compte_charge_id") val compteChargeId: Int? = null,
    val quantite: Double = 1.0,
    @SerialName("prix_unitaire") val prixUnitaire: Double,
)

@Serializable
data class FactureFournisseurDto(
    val id: Int,
    @SerialName("type_document") val typeDocument: String,
    @SerialName("facture_origine") val factureOrigine: Int? = null,
    @SerialName("numero_interne") val numeroInterne: String,
    @SerialName("numero_fournisseur") val numeroFournisseur: String? = null,
    @SerialName("fournisseur_nom") val fournisseurNom: String,
    @SerialName("fournisseur_email") val fournisseurEmail: String? = null,
    val devise: String = "XAF",
    val statut: String,
    @SerialName("date_reception") val dateReception: String? = null,
    @SerialName("date_facture") val dateFacture: String,
    @SerialName("date_echeance") val dateEcheance: String,
    @SerialName("tva_taux") val tvaTaux: Double,
    @SerialName("montant_ht") val montantHt: Double,
    val tva: Double,
    @SerialName("montant_ttc") val montantTtc: Double,
    val ecriture: Int? = null,
    @SerialName("created_at") val createdAt: String,
    val lignes: List<LigneFactureFournisseurDto> = emptyList(),
)

@Serializable
data class FactureFournisseurCreateDto(
    @SerialName("fournisseur_nom") val fournisseurNom: String,
    @SerialName("fournisseur_email") val fournisseurEmail: String? = null,
    @SerialName("numero_fournisseur") val numeroFournisseur: String? = null,
    val devise: String = "XAF",
    @SerialName("date_facture") val dateFacture: String,
    @SerialName("date_echeance") val dateEcheance: String,
    @SerialName("tva_taux") val tvaTaux: Double = 0.0,
    val lignes: List<LigneFactureFournisseurCreateDto>,
)

// ── PAIEMENTS CLIENTS ────────────────────────────────────────────────────────
// Conforme à comptabilite/serializers/paiement.py — endpoint /api/v1/compta/paiements/

@Serializable
data class PaiementDto(
    val id: Int,
    @SerialName("facture_id") val factureId: Int,
    val montant: Double,
    val devise: String = "XAF",
    @SerialName("mode_paiement") val modePaiement: String,
    @SerialName("date_paiement") val datePaiement: String,
    val reference: String? = null,
    @SerialName("ecriture_id") val ecritureId: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class PaiementCreateDto(
    @SerialName("facture_id") val factureId: Int,
    val montant: Double,
    @SerialName("mode_paiement") val modePaiement: String,
    @SerialName("date_paiement") val datePaiement: String,
    val reference: String? = null,
)

// ── PAIEMENTS FOURNISSEURS ───────────────────────────────────────────────────
// Conforme à comptabilite/serializers/paiement_fournisseur.py — endpoint /api/v1/compta/paiements-fournisseurs/

@Serializable
data class PaiementFournisseurDto(
    val id: Int,
    @SerialName("facture_fournisseur") val factureFournisseur: Int,
    @SerialName("facture_numero") val factureNumero: String? = null,
    @SerialName("fournisseur_nom") val fournisseurNom: String? = null,
    val montant: Double,
    val devise: String = "XAF",
    @SerialName("mode_paiement") val modePaiement: String,
    @SerialName("date_paiement") val datePaiement: String,
    val reference: String? = null,
    val ecriture: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

/** Corps de POST /achats/{id}/payer/ — pas de facture_fournisseur_id, déduit de l'URL. */
@Serializable
data class PaiementFournisseurCreateDto(
    val montant: Double,
    @SerialName("mode_paiement") val modePaiement: String,
    @SerialName("date_paiement") val datePaiement: String,
    val reference: String? = null,
)

// ── INITIALISATION SYSCOHADA ─────────────────────────────────────────────────
// Conforme à comptabilite/views/initialiser.py — endpoint POST /api/v1/compta/initialiser/

@Serializable
data class InitialiserStatsDto(
    @SerialName("comptes_crees") val comptesCrees: Int,
    @SerialName("comptes_existants") val comptesExistants: Int,
    @SerialName("journaux_crees") val journauxCrees: Int,
    @SerialName("exercice_cree") val exerciceCree: Boolean,
)

@Serializable
data class InitialiserResponseDto(
    val message: String,
    val annee: Int,
    val stats: InitialiserStatsDto,
)

// ── PLAN COMPTABLE ───────────────────────────────────────────────────────────
// Conforme à comptabilite/serializers/compte.py — endpoint /api/v1/compta/comptes/

@Serializable
data class CompteDto(
    val id: Int,
    val numero: String,
    val libelle: String,
    val classe: Int,
    @SerialName("type_compte") val typeCompte: String,
    @SerialName("is_systeme") val isSysteme: Boolean = false,
    @SerialName("is_actif") val isActif: Boolean = true,
    @SerialName("created_at") val createdAt: String,
)

// ── BALANCE DES COMPTES ───────────────────────────────────────────────────────
// Conforme à comptabilite/views/etats.py BalanceView — endpoint /api/v1/compta/balance/

@Serializable
data class BalanceLigneDto(
    @SerialName("compte_numero") val compteNumero: String,
    @SerialName("compte_libelle") val compteLibelle: String,
    val classe: Int,
    @SerialName("total_debit") val totalDebit: String,
    @SerialName("total_credit") val totalCredit: String,
    @SerialName("solde_debiteur") val soldeDebiteur: String,
    @SerialName("solde_crediteur") val soldeCrediteur: String,
)

@Serializable
data class BalanceDto(
    @SerialName("exercice_id") val exerciceId: Int,
    val devise: String = "XAF",
    @SerialName("total_debit") val totalDebit: String,
    @SerialName("total_credit") val totalCredit: String,
    val lignes: List<BalanceLigneDto> = emptyList(),
)

// ── GRAND LIVRE ───────────────────────────────────────────────────────────────
// Conforme à comptabilite/views/etats.py GrandLivreView — endpoint /api/v1/compta/grand-livre/

@Serializable
data class GrandLivreLigneDto(
    @SerialName("date_ecriture") val dateEcriture: String,
    val libelle: String,
    @SerialName("journal_code") val journalCode: String,
    val debit: String,
    val credit: String,
    @SerialName("solde_cumule") val soldeCumule: String,
)

@Serializable
data class GrandLivreCompteDto(
    @SerialName("exercice_id") val exerciceId: Int,
    @SerialName("compte_numero") val compteNumero: String,
    @SerialName("compte_libelle") val compteLibelle: String,
    @SerialName("total_debit") val totalDebit: String,
    @SerialName("total_credit") val totalCredit: String,
    @SerialName("solde_final") val soldeFinal: String,
    val lignes: List<GrandLivreLigneDto> = emptyList(),
)

// ── BILAN ─────────────────────────────────────────────────────────────────────
// Conforme à comptabilite/views/etats.py BilanView — endpoint /api/v1/compta/bilan/

@Serializable
data class BilanActifDto(
    val immobilisations: String,
    @SerialName("actif_circulant") val actifCirculant: String,
    @SerialName("tresorerie_actif") val tresorerieActif: String,
    @SerialName("total_actif") val totalActif: String,
)

@Serializable
data class BilanPassifDto(
    @SerialName("capitaux_propres") val capitauxPropres: String,
    val dettes: String,
    @SerialName("tresorerie_passif") val tresoreriePassif: String,
    @SerialName("total_passif") val totalPassif: String,
)

@Serializable
data class BilanDto(
    @SerialName("exercice_id") val exerciceId: Int,
    val devise: String = "XAF",
    val actif: BilanActifDto,
    val passif: BilanPassifDto,
    val equilibre: Boolean,
)

// ── COMPTE DE RÉSULTAT ────────────────────────────────────────────────────────
// Conforme à comptabilite/views/etats.py CompteResultatView — /api/v1/compta/compte-resultat/

@Serializable
data class CompteResultatProduitsDto(
    @SerialName("chiffre_affaires") val chiffreAffaires: String,
    @SerialName("autres_produits") val autresProduits: String,
    @SerialName("total_produits") val totalProduits: String,
)

@Serializable
data class CompteResultatChargesDto(
    @SerialName("charges_exploitation") val chargesExploitation: String,
    @SerialName("autres_charges") val autresCharges: String,
    @SerialName("total_charges") val totalCharges: String,
)

@Serializable
data class CompteResultatDto(
    @SerialName("exercice_id") val exerciceId: Int,
    val devise: String = "XAF",
    val produits: CompteResultatProduitsDto,
    val charges: CompteResultatChargesDto,
    @SerialName("resultat_net") val resultatNet: String,
)

// ── EXERCICES ─────────────────────────────────────────────────────────────────
// Conforme à comptabilite/serializers/exercice.py — endpoint /api/v1/compta/exercices/

@Serializable
data class ExerciceDto(
    val id: Int,
    val annee: Int,
    @SerialName("date_debut") val dateDebut: String,
    @SerialName("date_fin") val dateFin: String,
    val statut: String,
    @SerialName("date_cloture") val dateCloture: String? = null,
    @SerialName("resultat_net") val resultatNet: String? = null,
    @SerialName("nb_ecritures") val nbEcritures: Int = 0,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ExerciceCreateDto(
    val annee: Int,
    @SerialName("date_debut") val dateDebut: String,
    @SerialName("date_fin") val dateFin: String,
)

@Serializable
data class ExerciceSuivantDto(val id: Int, val annee: Int, val cree: Boolean)

@Serializable
data class ExerciceCloturerResponseDto(
    val exercice: ExerciceDto,
    @SerialName("resultat_net") val resultatNet: String,
    @SerialName("type_resultat") val typeResultat: String,
    @SerialName("exercice_suivant") val exerciceSuivant: ExerciceSuivantDto,
)

// ── JOURNAUX ──────────────────────────────────────────────────────────────────
// Conforme à comptabilite/serializers/journal.py — endpoint /api/v1/compta/journaux/

@Serializable
data class JournalDto(
    val id: Int,
    val code: String,
    val libelle: String,
    val type: String,
    @SerialName("nb_ecritures") val nbEcritures: Int = 0,
    @SerialName("compte_contrepartie_numero") val compteContrepartieNumero: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class JournalCreateDto(val code: String, val libelle: String, val type: String)

// ── ÉCRITURES ─────────────────────────────────────────────────────────────────
// Conforme à comptabilite/serializers/ecriture.py — endpoint /api/v1/compta/ecritures/

@Serializable
data class LigneEcritureDto(
    val id: Int,
    @SerialName("compte_id") val compteId: Int,
    @SerialName("compte_numero") val compteNumero: String,
    @SerialName("compte_libelle") val compteLibelle: String,
    val libelle: String = "",
    val debit: String,
    val credit: String,
)

@Serializable
data class EcritureDto(
    val id: Int,
    @SerialName("journal_id") val journalId: Int,
    @SerialName("journal_code") val journalCode: String,
    @SerialName("exercice_id") val exerciceId: Int,
    @SerialName("date_ecriture") val dateEcriture: String,
    val libelle: String,
    @SerialName("reference_piece") val referencePiece: String = "",
    val statut: String,
    @SerialName("total_debit") val totalDebit: String,
    @SerialName("total_credit") val totalCredit: String,
    val lignes: List<LigneEcritureDto> = emptyList(),
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class LigneEcritureCreateDto(
    @SerialName("compte_id") val compteId: Int,
    val libelle: String = "",
    val debit: Double,
    val credit: Double,
)

@Serializable
data class EcritureCreateDto(
    @SerialName("journal_id") val journalId: Int,
    @SerialName("exercice_id") val exerciceId: Int,
    @SerialName("date_ecriture") val dateEcriture: String,
    val libelle: String,
    @SerialName("reference_piece") val referencePiece: String = "",
    val lignes: List<LigneEcritureCreateDto>,
)

// ── IMMOBILISATIONS & AMORTISSEMENTS ─────────────────────────────────────────
// Conforme à comptabilite/views/immobilisation.py

@Serializable
data class ImmobilisationDto(
    val id: Int,
    val code: String,
    val designation: String,
    val categorie: String,
    @SerialName("numero_compte") val numeroCompte: String,
    val fournisseur: String = "",
    @SerialName("reference_facture") val referenceFacture: String = "",
    @SerialName("valeur_origine") val valeurOrigine: String,
    @SerialName("valeur_residuelle") val valeurResiduelle: String = "0.00",
    val devise: String = "XAF",
    val methode: String,
    @SerialName("duree_vie") val dureeVie: Int,
    @SerialName("date_mise_en_service") val dateMiseEnService: String,
    @SerialName("cumul_amortissements") val cumulAmortissements: String = "0.00",
    @SerialName("valeur_nette_comptable") val valeurNetteComptable: String,
    @SerialName("taux_lineaire") val tauxLineaire: String,
    val statut: String,
    @SerialName("date_cession") val dateCession: String? = null,
    @SerialName("valeur_nette_cession") val valeurNetteCession: String? = null,
    val exercice: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ImmobilisationCreateDto(
    val code: String,
    val designation: String,
    val categorie: String,
    @SerialName("numero_compte") val numeroCompte: String,
    val fournisseur: String = "",
    @SerialName("reference_facture") val referenceFacture: String = "",
    @SerialName("valeur_origine") val valeurOrigine: Double,
    @SerialName("valeur_residuelle") val valeurResiduelle: Double = 0.0,
    val devise: String = "XAF",
    val methode: String = "LINEAIRE",
    @SerialName("duree_vie") val dureeVie: Int = 5,
    @SerialName("date_mise_en_service") val dateMiseEnService: String,
    val exercice: Int? = null,
)

@Serializable
data class ImmobilisationCederDto(
    @SerialName("valeur_nette_cession") val valeurNetteCession: Double = 0.0,
)

@Serializable
data class DotationAmortissementDto(
    val id: Int,
    val immobilisation: Int,
    @SerialName("immobilisation_code") val immobilisationCode: String,
    @SerialName("immobilisation_designation") val immobilisationDesignation: String,
    val exercice: Int,
    val annee: Int,
    val montant: String,
    val ecriture: Int? = null,
    @SerialName("date_comptabilisation") val dateComptabilisation: String,
)

@Serializable
data class PlanAmortissementLigneDto(
    val annee: Int,
    @SerialName("annuite_prevue") val annuitePrevue: String,
    @SerialName("cumul_prevu") val cumulPrevu: String,
    @SerialName("vnc_fin") val vncFin: String,
    val passe: Boolean,
    @SerialName("montant_reel") val montantReel: String? = null,
)

@Serializable
data class PlanAmortissementDto(
    val immobilisation: ImmobilisationDto,
    val plan: List<PlanAmortissementLigneDto>,
)

@Serializable
data class AmortirResponseDto(
    val dotation: DotationAmortissementDto,
    @SerialName("valeur_nette_comptable") val valeurNetteComptable: String,
    @SerialName("ecriture_id") val ecritureId: Int? = null,
)

// ── DÉCLARATION TVA ───────────────────────────────────────────────────────────
// Conforme à comptabilite/views/declarations.py DeclarationTVAView
// GET /api/v1/compta/declarations/tva/?date_debut=YYYY-MM-DD&date_fin=YYYY-MM-DD

@Serializable
data class TVAPeriodeDto(
    @SerialName("date_debut") val dateDebut: String,
    @SerialName("date_fin") val dateFin: String,
)

@Serializable
data class TVADeductibleDto(
    @SerialName("sur_achats") val surAchats: String,
    @SerialName("sur_immobilisations") val surImmobilisations: String,
    @SerialName("sur_services") val surServices: String,
    val total: String,
)

@Serializable
data class DeclarationTVADto(
    val periode: TVAPeriodeDto,
    val devise: String = "XAF",
    @SerialName("tva_collectee") val tvaCollectee: String,
    @SerialName("tva_deductible") val tvaDeductible: TVADeductibleDto,
    val solde: String,
    val resultat: String,           // "TVA_A_PAYER" | "CREDIT_TVA"
    @SerialName("montant_a_payer") val montantAPayer: String,
    @SerialName("credit_reporte") val creditReporte: String,
)

// ── DASHBOARD STATS ──────────────────────────────────────────────────────────
// Conforme à comptabilite/views/stats.py — endpoint /api/v1/compta/stats/
// Les montants sont sérialisés en String côté backend (str(Decimal)), pas en nombre.

@Serializable
data class ExerciceCourantDto(
    val id: Int? = null,
    val annee: Int? = null,
)

@Serializable
data class ImpayeesEnRetardDto(
    val nb: Int,
    val montant: String,
)

@Serializable
data class ImpayeesDto(
    val nb: Int,
    val montant: String,
    @SerialName("en_retard") val enRetard: ImpayeesEnRetardDto,
)

@Serializable
data class VentesStatsDto(
    @SerialName("ca_mois") val caMois: String,
    @SerialName("nb_factures_mois") val nbFacturesMois: Int,
    val impayees: ImpayeesDto,
)

@Serializable
data class EnAttentePaiementDto(
    val nb: Int,
    val montant: String,
    @SerialName("en_retard") val enRetard: Int,
)

@Serializable
data class AchatsStatsDto(
    @SerialName("en_attente_paiement") val enAttentePaiement: EnAttentePaiementDto,
)

@Serializable
data class StatsDto(
    val date: String,
    val devise: String = "XAF",
    @SerialName("exercice_courant") val exerciceCourant: ExerciceCourantDto,
    val ventes: VentesStatsDto,
    val achats: AchatsStatsDto,
    @SerialName("charges_mois") val chargesMois: String,
    @SerialName("resultat_previsionnel_exercice") val resultatPrevisionnelExercice: String,
    @SerialName("solde_tresorerie") val soldeTresorerie: String,
)
