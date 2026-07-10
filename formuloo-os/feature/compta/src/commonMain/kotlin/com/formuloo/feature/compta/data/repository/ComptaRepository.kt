package com.formuloo.feature.compta.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.AmortirResponseDto
import com.formuloo.core.network.dto.compta.BalanceDto
import com.formuloo.core.network.dto.compta.DeclarationTVADto
import com.formuloo.core.network.dto.compta.ImmobilisationCreateDto
import com.formuloo.core.network.dto.compta.ImmobilisationDto
import com.formuloo.core.network.dto.compta.PlanAmortissementDto
import com.formuloo.core.network.dto.compta.CompteDto
import com.formuloo.core.network.dto.compta.BilanDto
import com.formuloo.core.network.dto.compta.CompteResultatDto
import com.formuloo.core.network.dto.compta.EcritureCreateDto
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.ExerciceCloturerResponseDto
import com.formuloo.core.network.dto.compta.ExerciceCreateDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.core.network.dto.compta.JournalCreateDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.feature.compta.domain.model.ComptaPage
import com.formuloo.feature.compta.domain.model.DashboardStats
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.Payment
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import com.formuloo.feature.compta.domain.model.SupplierPayment

/** Online-only (pas de cache local) — chaque appel passe directement par l'API Compta. */
interface ComptaRepository {

    // ── Factures clients ─────────────────────────────────────────────────
    suspend fun getInvoices(
        page: Int = 1,
        statut: String? = null,
        dateDebut: String? = null,
        dateFin: String? = null,
    ): NetworkResult<ComptaPage<Invoice>>

    suspend fun getInvoice(id: Int): NetworkResult<Invoice>

    suspend fun createInvoice(
        clientNom: String,
        clientEmail: String?,
        lignes: List<Triple<String, Double, Double>>, // description, quantite, prixUnitaire
        devise: String = "XAF",
        tvaTaux: Double = 0.0,
        dateEcheance: String,
    ): NetworkResult<Invoice>

    suspend fun emettreInvoice(id: Int): NetworkResult<Invoice>
    suspend fun emettreAvoirClient(id: Int): NetworkResult<Invoice>
    suspend fun getAvoirsClients(): NetworkResult<List<Invoice>>

    // ── Factures fournisseurs (achats) ───────────────────────────────────
    suspend fun getPurchaseInvoices(
        page: Int = 1,
        statut: String? = null,
        fournisseur: String? = null,
    ): NetworkResult<ComptaPage<PurchaseInvoice>>

    suspend fun getPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice>

    suspend fun createPurchaseInvoice(
        fournisseurNom: String,
        fournisseurEmail: String?,
        numeroFournisseur: String?,
        lignes: List<Triple<String, Double, Double>>,
        devise: String = "XAF",
        tvaTaux: Double = 0.0,
        dateFacture: String,
        dateEcheance: String,
    ): NetworkResult<PurchaseInvoice>

    suspend fun recevoirPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice>

    suspend fun validerPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice>

    suspend fun payerPurchaseInvoice(
        id: Int,
        montant: Double,
        modePaiement: PaymentMode,
        datePaiement: String,
        reference: String?,
    ): NetworkResult<SupplierPayment>
    suspend fun emettreAvoirFournisseur(id: Int): NetworkResult<PurchaseInvoice>
    suspend fun getAvoirsFournisseurs(): NetworkResult<List<PurchaseInvoice>>

    // ── Paiements ─────────────────────────────────────────────────────────
    suspend fun getPayments(factureId: Int? = null): NetworkResult<ComptaPage<Payment>>

    suspend fun createPayment(
        factureId: Int,
        montant: Double,
        modePaiement: PaymentMode,
        datePaiement: String,
        reference: String?,
    ): NetworkResult<Payment>

    suspend fun getSupplierPayments(factureFournisseurId: Int? = null): NetworkResult<ComptaPage<SupplierPayment>>

    // ── Exercices ─────────────────────────────────────────────────────────
    suspend fun getExercices(): NetworkResult<List<ExerciceDto>>
    suspend fun createExercice(dto: ExerciceCreateDto): NetworkResult<ExerciceDto>
    suspend fun cloturerExercice(id: Int): NetworkResult<ExerciceCloturerResponseDto>

    // ── États financiers ──────────────────────────────────────────────────
    suspend fun getGrandLivre(exerciceId: Int, compteId: Int): NetworkResult<GrandLivreCompteDto>
    suspend fun getBilan(exerciceId: Int): NetworkResult<BilanDto>
    suspend fun getCompteResultat(exerciceId: Int): NetworkResult<CompteResultatDto>

    // ── Plan comptable ────────────────────────────────────────────────────
    suspend fun initialiserPlan(): NetworkResult<Unit>
    suspend fun getAllComptes(): NetworkResult<List<CompteDto>>
    suspend fun getBalance(exerciceId: Int): NetworkResult<BalanceDto>

    // ── Journaux ──────────────────────────────────────────────────────────
    suspend fun getJournaux(): NetworkResult<List<JournalDto>>
    suspend fun createJournal(dto: JournalCreateDto): NetworkResult<JournalDto>

    // ── Écritures ─────────────────────────────────────────────────────────
    suspend fun getEcritures(
        page: Int = 1,
        journalId: Int? = null,
        exerciceId: Int? = null,
        statut: String? = null,
    ): NetworkResult<List<EcritureDto>>
    suspend fun createEcriture(dto: EcritureCreateDto): NetworkResult<EcritureDto>
    suspend fun validerEcriture(id: Int): NetworkResult<EcritureDto>
    suspend fun deleteEcriture(id: Int): NetworkResult<Unit>

    // ── Immobilisations ───────────────────────────────────────────────────
    suspend fun getImmobilisations(statut: String? = null, categorie: String? = null): NetworkResult<List<ImmobilisationDto>>
    suspend fun createImmobilisation(dto: ImmobilisationCreateDto): NetworkResult<ImmobilisationDto>
    suspend fun getPlanAmortissement(id: Int): NetworkResult<PlanAmortissementDto>
    suspend fun amortirImmobilisation(id: Int, exerciceId: Int? = null): NetworkResult<AmortirResponseDto>
    suspend fun cederImmobilisation(id: Int, valeurNetteCession: Double = 0.0): NetworkResult<ImmobilisationDto>

    // ── Déclaration TVA ───────────────────────────────────────────────────
    suspend fun getDeclarationTVA(dateDebut: String, dateFin: String): NetworkResult<DeclarationTVADto>

    // ── Dashboard ─────────────────────────────────────────────────────────
    suspend fun getStats(): NetworkResult<DashboardStats>
}
