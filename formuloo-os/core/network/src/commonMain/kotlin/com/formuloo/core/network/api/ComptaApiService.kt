package com.formuloo.core.network.api

import com.formuloo.core.common.FieldErrorDetail
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.ApiError
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
import com.formuloo.core.network.dto.compta.AmortirResponseDto
import com.formuloo.core.network.dto.compta.DeclarationTVADto
import com.formuloo.core.network.dto.compta.ImmobilisationCederDto
import com.formuloo.core.network.dto.compta.ImmobilisationCreateDto
import com.formuloo.core.network.dto.compta.ImmobilisationDto
import com.formuloo.core.network.dto.compta.PlanAmortissementDto
import com.formuloo.core.network.dto.compta.EcritureCreateDto
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.ExerciceCloturerResponseDto
import com.formuloo.core.network.dto.compta.ExerciceCreateDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.core.network.dto.compta.InitialiserResponseDto
import com.formuloo.core.network.dto.compta.JournalCreateDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.core.network.dto.compta.StatsDto
import com.formuloo.core.network.dto.hr.PaginatedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Client HTTP pour le service Comptabilité — contrat source : contracts/compta/v1/compta.yaml
 * BASE_URL : /api/v1 (configuré dans ApiClient.kt). Tous les endpoints sont préfixés par /compta/.
 */
class ComptaApiService(private val client: HttpClient) : ComptaRemoteDataSource {

    // ── FACTURES CLIENTS ──────────────────────────────────────────────────

    override suspend fun getFactures(
        page: Int,
        statut: String?,
        dateDebut: String?,
        dateFin: String?,
    ): NetworkResult<PaginatedResponse<FactureDto>> = safeCall {
        client.get("$BASE/factures/") {
            url {
                parameters.append("page", page.toString())
                if (statut != null) parameters.append("statut", statut)
                if (dateDebut != null) parameters.append("date_debut", dateDebut)
                if (dateFin != null) parameters.append("date_fin", dateFin)
            }
        }
    }

    override suspend fun getFacture(id: Int): NetworkResult<FactureDto> = safeCall {
        client.get("$BASE/factures/$id/")
    }

    override suspend fun createFacture(dto: FactureCreateDto): NetworkResult<FactureDto> = safeCall {
        client.post("$BASE/factures/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun emettreFacture(id: Int): NetworkResult<FactureDto> = safeCall {
        client.post("$BASE/factures/$id/emettre/")
    }

    override suspend fun emettreAvoirClient(id: Int): NetworkResult<FactureDto> = safeCall {
        client.post("$BASE/factures/$id/avoir/")
    }

    // ── FACTURES FOURNISSEURS (ACHATS) ────────────────────────────────────

    override suspend fun getAchats(
        page: Int,
        statut: String?,
        fournisseur: String?,
    ): NetworkResult<PaginatedResponse<FactureFournisseurDto>> = safeCall {
        client.get("$BASE/achats/") {
            url {
                parameters.append("page", page.toString())
                if (statut != null) parameters.append("statut", statut)
                if (fournisseur != null) parameters.append("fournisseur", fournisseur)
            }
        }
    }

    override suspend fun getAchat(id: Int): NetworkResult<FactureFournisseurDto> = safeCall {
        client.get("$BASE/achats/$id/")
    }

    override suspend fun createAchat(dto: FactureFournisseurCreateDto): NetworkResult<FactureFournisseurDto> = safeCall {
        client.post("$BASE/achats/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun recevoirAchat(id: Int): NetworkResult<FactureFournisseurDto> = safeCall {
        client.post("$BASE/achats/$id/recevoir/")
    }

    override suspend fun validerAchat(id: Int): NetworkResult<FactureFournisseurDto> = safeCall {
        client.post("$BASE/achats/$id/valider/")
    }

    override suspend fun payerAchat(
        id: Int,
        dto: PaiementFournisseurCreateDto,
    ): NetworkResult<PaiementFournisseurDto> = safeCall {
        client.post("$BASE/achats/$id/payer/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun emettreAvoirFournisseur(id: Int): NetworkResult<FactureFournisseurDto> = safeCall {
        client.post("$BASE/achats/$id/avoir/")
    }

    // ── PAIEMENTS ─────────────────────────────────────────────────────────

    override suspend fun getPaiements(factureId: Int?): NetworkResult<PaginatedResponse<PaiementDto>> = safeCall {
        client.get("$BASE/paiements/") {
            if (factureId != null) url { parameters.append("facture_id", factureId.toString()) }
        }
    }

    override suspend fun createPaiement(dto: PaiementCreateDto): NetworkResult<PaiementDto> = safeCall {
        client.post("$BASE/paiements/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun getPaiementsFournisseurs(
        factureFournisseurId: Int?,
    ): NetworkResult<PaginatedResponse<PaiementFournisseurDto>> = safeCall {
        client.get("$BASE/paiements-fournisseurs/") {
            if (factureFournisseurId != null) {
                url { parameters.append("facture_id", factureFournisseurId.toString()) }
            }
        }
    }

    // ── EXERCICES ─────────────────────────────────────────────────────────────

    override suspend fun getExercices(page: Int): NetworkResult<PaginatedResponse<ExerciceDto>> = safeCall {
        client.get("$BASE/exercices/") {
            url { parameters.append("page", page.toString()) }
        }
    }

    override suspend fun createExercice(dto: ExerciceCreateDto): NetworkResult<ExerciceDto> = safeCall {
        client.post("$BASE/exercices/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun cloturerExercice(id: Int): NetworkResult<ExerciceCloturerResponseDto> = safeCall {
        client.post("$BASE/exercices/$id/cloturer/")
    }

    // ── ÉTATS FINANCIERS ──────────────────────────────────────────────────────

    override suspend fun getGrandLivre(exerciceId: Int, compteId: Int): NetworkResult<GrandLivreCompteDto> = safeCall {
        client.get("$BASE/grand-livre/") {
            url {
                parameters.append("exercice_id", exerciceId.toString())
                parameters.append("compte_id", compteId.toString())
            }
        }
    }

    override suspend fun getBilan(exerciceId: Int): NetworkResult<BilanDto> = safeCall {
        client.get("$BASE/bilan/") {
            url { parameters.append("exercice_id", exerciceId.toString()) }
        }
    }

    override suspend fun getCompteResultat(exerciceId: Int): NetworkResult<CompteResultatDto> = safeCall {
        client.get("$BASE/compte-resultat/") {
            url { parameters.append("exercice_id", exerciceId.toString()) }
        }
    }

    // ── PLAN COMPTABLE ────────────────────────────────────────────────────

    override suspend fun initialiserSyscohadaPlan(): NetworkResult<InitialiserResponseDto> = safeCall {
        client.post("$BASE/initialiser/")
    }

    override suspend fun getComptes(page: Int, classe: Int?): NetworkResult<PaginatedResponse<CompteDto>> = safeCall {
        client.get("$BASE/comptes/") {
            url {
                parameters.append("page", page.toString())
                if (classe != null) parameters.append("classe", classe.toString())
            }
        }
    }

    override suspend fun getBalance(exerciceId: Int): NetworkResult<BalanceDto> = safeCall {
        client.get("$BASE/balance/") {
            url { parameters.append("exercice_id", exerciceId.toString()) }
        }
    }

    // ── JOURNAUX ──────────────────────────────────────────────────────────────

    override suspend fun getJournaux(page: Int): NetworkResult<PaginatedResponse<JournalDto>> = safeCall {
        client.get("$BASE/journaux/") {
            url { parameters.append("page", page.toString()) }
        }
    }

    override suspend fun createJournal(dto: JournalCreateDto): NetworkResult<JournalDto> = safeCall {
        client.post("$BASE/journaux/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    // ── ÉCRITURES ─────────────────────────────────────────────────────────────

    override suspend fun getEcritures(
        page: Int,
        journalId: Int?,
        exerciceId: Int?,
        statut: String?,
    ): NetworkResult<PaginatedResponse<EcritureDto>> = safeCall {
        client.get("$BASE/ecritures/") {
            url {
                parameters.append("page", page.toString())
                if (journalId != null) parameters.append("journal_id", journalId.toString())
                if (exerciceId != null) parameters.append("exercice_id", exerciceId.toString())
                if (statut != null) parameters.append("statut", statut)
            }
        }
    }

    override suspend fun createEcriture(dto: EcritureCreateDto): NetworkResult<EcritureDto> = safeCall {
        client.post("$BASE/ecritures/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun validerEcriture(id: Int): NetworkResult<EcritureDto> = safeCall {
        client.post("$BASE/ecritures/$id/valider/")
    }

    override suspend fun deleteEcriture(id: Int): NetworkResult<Unit> = try {
        val response = client.delete("$BASE/ecritures/$id/")
        if (response.status.isSuccess()) {
            NetworkResult.Success(Unit, response.status.value)
        } else {
            response.toErrorResult()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    // ── IMMOBILISATIONS ───────────────────────────────────────────────────────

    override suspend fun getImmobilisations(statut: String?, categorie: String?): NetworkResult<List<ImmobilisationDto>> {
        val paginated = safeCall<com.formuloo.core.network.dto.hr.PaginatedResponse<ImmobilisationDto>> {
            client.get("$BASE/immobilisations/") {
                url {
                    if (statut != null) parameters.append("statut", statut)
                    if (categorie != null) parameters.append("categorie", categorie)
                }
            }
        }
        return when (paginated) {
            is NetworkResult.Success -> NetworkResult.Success(paginated.data.results, paginated.code)
            is NetworkResult.Error -> paginated
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun createImmobilisation(dto: ImmobilisationCreateDto): NetworkResult<ImmobilisationDto> = safeCall {
        client.post("$BASE/immobilisations/") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
    }

    override suspend fun getPlanAmortissement(id: Int): NetworkResult<PlanAmortissementDto> = safeCall {
        client.get("$BASE/immobilisations/$id/plan/")
    }

    override suspend fun amortirImmobilisation(id: Int, exerciceId: Int?): NetworkResult<AmortirResponseDto> = safeCall {
        client.post("$BASE/immobilisations/$id/amortir/") {
            if (exerciceId != null) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("exercice_id" to exerciceId))
            }
        }
    }

    override suspend fun cederImmobilisation(id: Int, valeurNetteCession: Double): NetworkResult<ImmobilisationDto> = safeCall {
        client.delete("$BASE/immobilisations/$id/") {
            contentType(ContentType.Application.Json)
            setBody(ImmobilisationCederDto(valeurNetteCession = valeurNetteCession))
        }
    }

    // ── DÉCLARATION TVA ───────────────────────────────────────────────────────

    override suspend fun getDeclarationTVA(dateDebut: String, dateFin: String): NetworkResult<DeclarationTVADto> = safeCall {
        client.get("$BASE/declarations/tva/") {
            url {
                parameters.append("date_debut", dateDebut)
                parameters.append("date_fin", dateFin)
            }
        }
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────

    override suspend fun getStats(): NetworkResult<StatsDto> = safeCall {
        client.get("$BASE/stats/")
    }

    // ── HELPERS PRIVÉS ────────────────────────────────────────────────────

    private suspend inline fun <reified T> safeCall(
        crossinline block: suspend () -> HttpResponse,
    ): NetworkResult<T> = try {
        block().toResult()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error("Impossible de contacter le serveur. Vérifiez votre connexion.")
    }

    private suspend inline fun <reified T> HttpResponse.toResult(): NetworkResult<T> =
        if (status.isSuccess()) {
            NetworkResult.Success(body<T>(), status.value)
        } else {
            toErrorResult()
        }

    private suspend fun <T> HttpResponse.toErrorResult(): NetworkResult<T> {
        val code = status.value
        val apiError = try { body<ApiError>() } catch (e: Exception) { null }
        return when (code) {
            401 -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Authentification requise.",
                code = code,
            )
            404 -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Ressource introuvable.",
                code = code,
            )
            else -> NetworkResult.Error(
                message = apiError?.error?.message ?: "Erreur serveur ($code).",
                code = code,
                fieldErrors = apiError?.error?.details?.map {
                    FieldErrorDetail(field = it.field, message = it.message)
                } ?: emptyList(),
            )
        }
    }

    private companion object {
        const val BASE = "/api/v1/compta"
    }
}
