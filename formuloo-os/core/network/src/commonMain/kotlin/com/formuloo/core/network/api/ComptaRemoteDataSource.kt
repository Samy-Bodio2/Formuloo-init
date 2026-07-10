package com.formuloo.core.network.api

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.compta.FactureCreateDto
import com.formuloo.core.network.dto.compta.FactureDto
import com.formuloo.core.network.dto.compta.FactureFournisseurCreateDto
import com.formuloo.core.network.dto.compta.FactureFournisseurDto
import com.formuloo.core.network.dto.compta.PaiementCreateDto
import com.formuloo.core.network.dto.compta.PaiementDto
import com.formuloo.core.network.dto.compta.PaiementFournisseurCreateDto
import com.formuloo.core.network.dto.compta.PaiementFournisseurDto
import com.formuloo.core.network.dto.compta.BalanceDto
import com.formuloo.core.network.dto.compta.CompteDto
import com.formuloo.core.network.dto.compta.BilanDto
import com.formuloo.core.network.dto.compta.CompteResultatDto
import com.formuloo.core.network.dto.compta.EcritureCreateDto
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.ExerciceCloturerResponseDto
import com.formuloo.core.network.dto.compta.ExerciceCreateDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.core.network.dto.compta.InitialiserResponseDto
import com.formuloo.core.network.dto.compta.JournalCreateDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.core.network.dto.compta.AmortirResponseDto
import com.formuloo.core.network.dto.compta.DeclarationTVADto
import com.formuloo.core.network.dto.compta.ImmobilisationCreateDto
import com.formuloo.core.network.dto.compta.ImmobilisationDto
import com.formuloo.core.network.dto.compta.PlanAmortissementDto
import com.formuloo.core.network.dto.compta.StatsDto
import com.formuloo.core.network.dto.hr.PaginatedResponse

interface ComptaRemoteDataSource {

    // ── Factures clients ─────────────────────────────────────────────────
    suspend fun getFactures(
        page: Int = 1,
        statut: String? = null,
        dateDebut: String? = null,
        dateFin: String? = null,
    ): NetworkResult<PaginatedResponse<FactureDto>>

    suspend fun getFacture(id: Int): NetworkResult<FactureDto>

    suspend fun createFacture(dto: FactureCreateDto): NetworkResult<FactureDto>

    suspend fun emettreFacture(id: Int): NetworkResult<FactureDto>
    suspend fun emettreAvoirClient(id: Int): NetworkResult<FactureDto>

    // ── Factures fournisseurs (achats) ───────────────────────────────────
    suspend fun getAchats(
        page: Int = 1,
        statut: String? = null,
        fournisseur: String? = null,
    ): NetworkResult<PaginatedResponse<FactureFournisseurDto>>

    suspend fun getAchat(id: Int): NetworkResult<FactureFournisseurDto>

    suspend fun createAchat(dto: FactureFournisseurCreateDto): NetworkResult<FactureFournisseurDto>

    suspend fun recevoirAchat(id: Int): NetworkResult<FactureFournisseurDto>

    suspend fun validerAchat(id: Int): NetworkResult<FactureFournisseurDto>

    suspend fun payerAchat(id: Int, dto: PaiementFournisseurCreateDto): NetworkResult<PaiementFournisseurDto>
    suspend fun emettreAvoirFournisseur(id: Int): NetworkResult<FactureFournisseurDto>

    // ── Paiements ─────────────────────────────────────────────────────────
    suspend fun getPaiements(factureId: Int? = null): NetworkResult<PaginatedResponse<PaiementDto>>

    suspend fun createPaiement(dto: PaiementCreateDto): NetworkResult<PaiementDto>

    suspend fun getPaiementsFournisseurs(
        factureFournisseurId: Int? = null,
    ): NetworkResult<PaginatedResponse<PaiementFournisseurDto>>

    // ── Exercices ─────────────────────────────────────────────────────────
    suspend fun getExercices(page: Int = 1): NetworkResult<PaginatedResponse<ExerciceDto>>
    suspend fun createExercice(dto: ExerciceCreateDto): NetworkResult<ExerciceDto>
    suspend fun cloturerExercice(id: Int): NetworkResult<ExerciceCloturerResponseDto>

    // ── États financiers ──────────────────────────────────────────────────
    suspend fun getGrandLivre(exerciceId: Int, compteId: Int): NetworkResult<GrandLivreCompteDto>
    suspend fun getBilan(exerciceId: Int): NetworkResult<BilanDto>
    suspend fun getCompteResultat(exerciceId: Int): NetworkResult<CompteResultatDto>

    // ── Plan comptable ────────────────────────────────────────────────────
    suspend fun initialiserSyscohadaPlan(): NetworkResult<InitialiserResponseDto>
    suspend fun getComptes(page: Int = 1, classe: Int? = null): NetworkResult<PaginatedResponse<CompteDto>>
    suspend fun getBalance(exerciceId: Int): NetworkResult<BalanceDto>

    // ── Journaux ──────────────────────────────────────────────────────────────
    suspend fun getJournaux(page: Int = 1): NetworkResult<PaginatedResponse<JournalDto>>
    suspend fun createJournal(dto: JournalCreateDto): NetworkResult<JournalDto>

    // ── Écritures ─────────────────────────────────────────────────────────────
    suspend fun getEcritures(
        page: Int = 1,
        journalId: Int? = null,
        exerciceId: Int? = null,
        statut: String? = null,
    ): NetworkResult<PaginatedResponse<EcritureDto>>
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
    suspend fun getStats(): NetworkResult<StatsDto>
}
