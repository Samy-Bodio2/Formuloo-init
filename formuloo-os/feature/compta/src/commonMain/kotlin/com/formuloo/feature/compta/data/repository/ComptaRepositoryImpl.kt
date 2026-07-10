package com.formuloo.feature.compta.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.api.ComptaRemoteDataSource
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
import com.formuloo.core.network.dto.compta.FactureCreateDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.core.network.dto.compta.JournalCreateDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.core.network.dto.compta.FactureFournisseurCreateDto
import com.formuloo.core.network.dto.compta.LigneFactureCreateDto
import com.formuloo.core.network.dto.compta.LigneFactureFournisseurCreateDto
import com.formuloo.core.network.dto.compta.PaiementCreateDto
import com.formuloo.core.network.dto.compta.PaiementFournisseurCreateDto
import com.formuloo.feature.compta.data.mapper.toComptaPage
import com.formuloo.feature.compta.data.mapper.toDomain
import com.formuloo.feature.compta.domain.model.ComptaPage
import com.formuloo.feature.compta.domain.model.DashboardStats
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.Payment
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import com.formuloo.feature.compta.domain.model.SupplierPayment

class ComptaRepositoryImpl(
    private val remote: ComptaRemoteDataSource,
) : ComptaRepository {

    override suspend fun getInvoices(
        page: Int,
        statut: String?,
        dateDebut: String?,
        dateFin: String?,
    ): NetworkResult<ComptaPage<Invoice>> = when (val result = remote.getFactures(page, statut, dateDebut, dateFin)) {
        is NetworkResult.Success -> NetworkResult.Success(result.data.toComptaPage { it.toDomain() }, result.code)
        is NetworkResult.Error -> result
        is NetworkResult.Loading -> NetworkResult.Loading
    }

    override suspend fun getInvoice(id: Int): NetworkResult<Invoice> = remote.getFacture(id).map { it.toDomain() }

    override suspend fun createInvoice(
        clientNom: String,
        clientEmail: String?,
        lignes: List<Triple<String, Double, Double>>,
        devise: String,
        tvaTaux: Double,
        dateEcheance: String,
    ): NetworkResult<Invoice> = remote.createFacture(
        FactureCreateDto(
            clientNom = clientNom,
            clientEmail = clientEmail,
            lignes = lignes.map { (description, quantite, prixUnitaire) ->
                LigneFactureCreateDto(description = description, quantite = quantite, prixUnitaire = prixUnitaire)
            },
            devise = devise,
            tvaTaux = tvaTaux,
            dateEcheance = dateEcheance,
        ),
    ).map { it.toDomain() }

    override suspend fun emettreInvoice(id: Int): NetworkResult<Invoice> = remote.emettreFacture(id).map { it.toDomain() }

    override suspend fun emettreAvoirClient(id: Int): NetworkResult<Invoice> =
        remote.emettreAvoirClient(id).map { it.toDomain() }

    override suspend fun getAvoirsClients(): NetworkResult<List<Invoice>> {
        val all = mutableListOf<Invoice>()
        var page = 1
        while (true) {
            when (val r = remote.getFactures(page)) {
                is NetworkResult.Success -> {
                    all.addAll(r.data.results.filter { it.typeDocument == "AVOIR" }.map { it.toDomain() })
                    if (r.data.next == null) break
                    page++
                }
                is NetworkResult.Error -> return r
                is NetworkResult.Loading -> break
            }
        }
        return NetworkResult.Success(all, 200)
    }

    override suspend fun getPurchaseInvoices(
        page: Int,
        statut: String?,
        fournisseur: String?,
    ): NetworkResult<ComptaPage<PurchaseInvoice>> = when (val result = remote.getAchats(page, statut, fournisseur)) {
        is NetworkResult.Success -> NetworkResult.Success(result.data.toComptaPage { it.toDomain() }, result.code)
        is NetworkResult.Error -> result
        is NetworkResult.Loading -> NetworkResult.Loading
    }

    override suspend fun getPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice> =
        remote.getAchat(id).map { it.toDomain() }

    override suspend fun createPurchaseInvoice(
        fournisseurNom: String,
        fournisseurEmail: String?,
        numeroFournisseur: String?,
        lignes: List<Triple<String, Double, Double>>,
        devise: String,
        tvaTaux: Double,
        dateFacture: String,
        dateEcheance: String,
    ): NetworkResult<PurchaseInvoice> = remote.createAchat(
        FactureFournisseurCreateDto(
            fournisseurNom = fournisseurNom,
            fournisseurEmail = fournisseurEmail,
            numeroFournisseur = numeroFournisseur,
            lignes = lignes.map { (description, quantite, prixUnitaire) ->
                LigneFactureFournisseurCreateDto(description = description, quantite = quantite, prixUnitaire = prixUnitaire)
            },
            devise = devise,
            tvaTaux = tvaTaux,
            dateFacture = dateFacture,
            dateEcheance = dateEcheance,
        ),
    ).map { it.toDomain() }

    override suspend fun recevoirPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice> =
        remote.recevoirAchat(id).map { it.toDomain() }

    override suspend fun validerPurchaseInvoice(id: Int): NetworkResult<PurchaseInvoice> =
        remote.validerAchat(id).map { it.toDomain() }

    override suspend fun payerPurchaseInvoice(
        id: Int,
        montant: Double,
        modePaiement: PaymentMode,
        datePaiement: String,
        reference: String?,
    ): NetworkResult<SupplierPayment> = remote.payerAchat(
        id,
        PaiementFournisseurCreateDto(
            montant = montant,
            modePaiement = modePaiement.name,
            datePaiement = datePaiement,
            reference = reference,
        ),
    ).map { it.toDomain() }

    override suspend fun emettreAvoirFournisseur(id: Int): NetworkResult<PurchaseInvoice> =
        remote.emettreAvoirFournisseur(id).map { it.toDomain() }

    override suspend fun getAvoirsFournisseurs(): NetworkResult<List<PurchaseInvoice>> {
        val all = mutableListOf<PurchaseInvoice>()
        var page = 1
        while (true) {
            when (val r = remote.getAchats(page)) {
                is NetworkResult.Success -> {
                    all.addAll(r.data.results.filter { it.typeDocument == "AVOIR" }.map { it.toDomain() })
                    if (r.data.next == null) break
                    page++
                }
                is NetworkResult.Error -> return r
                is NetworkResult.Loading -> break
            }
        }
        return NetworkResult.Success(all, 200)
    }

    override suspend fun getPayments(factureId: Int?): NetworkResult<ComptaPage<Payment>> =
        when (val result = remote.getPaiements(factureId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toComptaPage { it.toDomain() }, result.code)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun createPayment(
        factureId: Int,
        montant: Double,
        modePaiement: PaymentMode,
        datePaiement: String,
        reference: String?,
    ): NetworkResult<Payment> = remote.createPaiement(
        PaiementCreateDto(
            factureId = factureId,
            montant = montant,
            modePaiement = modePaiement.name,
            datePaiement = datePaiement,
            reference = reference,
        ),
    ).map { it.toDomain() }

    override suspend fun getSupplierPayments(factureFournisseurId: Int?): NetworkResult<ComptaPage<SupplierPayment>> =
        when (val result = remote.getPaiementsFournisseurs(factureFournisseurId)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toComptaPage { it.toDomain() }, result.code)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getExercices(): NetworkResult<List<ExerciceDto>> {
        val all = mutableListOf<ExerciceDto>()
        var page = 1
        while (true) {
            when (val result = remote.getExercices(page)) {
                is NetworkResult.Success -> {
                    all.addAll(result.data.results)
                    if (result.data.next == null) break
                    page++
                }
                is NetworkResult.Error -> return result
                is NetworkResult.Loading -> break
            }
        }
        return NetworkResult.Success(all, 200)
    }

    override suspend fun createExercice(dto: ExerciceCreateDto): NetworkResult<ExerciceDto> =
        remote.createExercice(dto)

    override suspend fun cloturerExercice(id: Int): NetworkResult<ExerciceCloturerResponseDto> =
        remote.cloturerExercice(id)

    override suspend fun getGrandLivre(exerciceId: Int, compteId: Int): NetworkResult<GrandLivreCompteDto> =
        remote.getGrandLivre(exerciceId, compteId)

    override suspend fun getBilan(exerciceId: Int): NetworkResult<BilanDto> =
        remote.getBilan(exerciceId)

    override suspend fun getCompteResultat(exerciceId: Int): NetworkResult<CompteResultatDto> =
        remote.getCompteResultat(exerciceId)

    override suspend fun initialiserPlan(): NetworkResult<Unit> =
        when (val r = remote.initialiserSyscohadaPlan()) {
            is NetworkResult.Success -> NetworkResult.Success(Unit, r.code)
            is NetworkResult.Error -> r
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun getAllComptes(): NetworkResult<List<CompteDto>> {
        val all = mutableListOf<CompteDto>()
        var page = 1
        while (true) {
            when (val result = remote.getComptes(page)) {
                is NetworkResult.Success -> {
                    all.addAll(result.data.results)
                    if (result.data.next == null) break
                    page++
                }
                is NetworkResult.Error -> return result
                is NetworkResult.Loading -> break
            }
        }
        return NetworkResult.Success(all, 200)
    }

    override suspend fun getBalance(exerciceId: Int): NetworkResult<BalanceDto> =
        remote.getBalance(exerciceId)

    override suspend fun getJournaux(): NetworkResult<List<JournalDto>> {
        val all = mutableListOf<JournalDto>()
        var page = 1
        while (true) {
            when (val result = remote.getJournaux(page)) {
                is NetworkResult.Success -> {
                    all.addAll(result.data.results)
                    if (result.data.next == null) break
                    page++
                }
                is NetworkResult.Error -> return result
                is NetworkResult.Loading -> break
            }
        }
        return NetworkResult.Success(all, 200)
    }

    override suspend fun createJournal(dto: JournalCreateDto): NetworkResult<JournalDto> =
        remote.createJournal(dto)

    override suspend fun getEcritures(
        page: Int,
        journalId: Int?,
        exerciceId: Int?,
        statut: String?,
    ): NetworkResult<List<EcritureDto>> = when (val r = remote.getEcritures(page, journalId, exerciceId, statut)) {
        is NetworkResult.Success -> NetworkResult.Success(r.data.results, r.code)
        is NetworkResult.Error -> r
        is NetworkResult.Loading -> NetworkResult.Loading
    }

    override suspend fun createEcriture(dto: EcritureCreateDto): NetworkResult<EcritureDto> =
        remote.createEcriture(dto)

    override suspend fun validerEcriture(id: Int): NetworkResult<EcritureDto> =
        remote.validerEcriture(id)

    override suspend fun deleteEcriture(id: Int): NetworkResult<Unit> =
        remote.deleteEcriture(id)

    override suspend fun getImmobilisations(statut: String?, categorie: String?): NetworkResult<List<ImmobilisationDto>> =
        remote.getImmobilisations(statut, categorie)

    override suspend fun createImmobilisation(dto: ImmobilisationCreateDto): NetworkResult<ImmobilisationDto> =
        remote.createImmobilisation(dto)

    override suspend fun getPlanAmortissement(id: Int): NetworkResult<PlanAmortissementDto> =
        remote.getPlanAmortissement(id)

    override suspend fun amortirImmobilisation(id: Int, exerciceId: Int?): NetworkResult<AmortirResponseDto> =
        remote.amortirImmobilisation(id, exerciceId)

    override suspend fun cederImmobilisation(id: Int, valeurNetteCession: Double): NetworkResult<ImmobilisationDto> =
        remote.cederImmobilisation(id, valeurNetteCession)

    override suspend fun getDeclarationTVA(dateDebut: String, dateFin: String): NetworkResult<DeclarationTVADto> =
        remote.getDeclarationTVA(dateDebut, dateFin)

    override suspend fun getStats(): NetworkResult<DashboardStats> = remote.getStats().map { it.toDomain() }

    private inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is NetworkResult.Success -> NetworkResult.Success(transform(data), code)
        is NetworkResult.Error -> this
        is NetworkResult.Loading -> NetworkResult.Loading
    }
}
