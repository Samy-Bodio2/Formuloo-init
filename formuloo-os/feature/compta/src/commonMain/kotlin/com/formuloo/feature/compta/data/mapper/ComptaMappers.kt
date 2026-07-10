package com.formuloo.feature.compta.data.mapper

import com.formuloo.core.network.dto.compta.FactureDto
import com.formuloo.core.network.dto.compta.FactureFournisseurDto
import com.formuloo.core.network.dto.compta.LigneFactureDto
import com.formuloo.core.network.dto.compta.LigneFactureFournisseurDto
import com.formuloo.core.network.dto.compta.PaiementDto
import com.formuloo.core.network.dto.compta.PaiementFournisseurDto
import com.formuloo.core.network.dto.compta.StatsDto
import com.formuloo.core.network.dto.hr.PaginatedResponse
import com.formuloo.feature.compta.domain.model.ComptaPage
import com.formuloo.feature.compta.domain.model.DashboardStats
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.InvoiceLine
import com.formuloo.feature.compta.domain.model.InvoiceStatus
import com.formuloo.feature.compta.domain.model.Payment
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import com.formuloo.feature.compta.domain.model.PurchaseInvoiceLine
import com.formuloo.feature.compta.domain.model.PurchaseInvoiceStatus
import com.formuloo.feature.compta.domain.model.SupplierPayment

fun LigneFactureDto.toDomain() = InvoiceLine(
    id = id,
    description = description,
    quantite = quantite,
    prixUnitaire = prixUnitaire,
    montantTotal = montantTotal,
)

fun FactureDto.toDomain() = Invoice(
    id = id,
    typeDocument = typeDocument,
    factureOrigineId = factureOrigine,
    numero = numero,
    clientNom = clientNom,
    clientEmail = clientEmail,
    lignes = lignes.map { it.toDomain() },
    montantHt = montantHt,
    tvaTaux = tvaTaux,
    tva = tva,
    montantTtc = montantTtc,
    devise = devise,
    statut = runCatching { InvoiceStatus.valueOf(statut.uppercase()) }.getOrDefault(InvoiceStatus.BROUILLON),
    dateEmission = dateEmission,
    dateEcheance = dateEcheance,
    createdAt = createdAt,
)

fun LigneFactureFournisseurDto.toDomain() = PurchaseInvoiceLine(
    id = id,
    description = description,
    quantite = quantite,
    prixUnitaire = prixUnitaire,
    montantTotal = montantTotal,
)

fun FactureFournisseurDto.toDomain() = PurchaseInvoice(
    id = id,
    typeDocument = typeDocument,
    factureOrigineId = factureOrigine,
    numeroInterne = numeroInterne,
    numeroFournisseur = numeroFournisseur,
    fournisseurNom = fournisseurNom,
    fournisseurEmail = fournisseurEmail,
    lignes = lignes.map { it.toDomain() },
    montantHt = montantHt,
    tvaTaux = tvaTaux,
    tva = tva,
    montantTtc = montantTtc,
    devise = devise,
    statut = runCatching { PurchaseInvoiceStatus.valueOf(statut.uppercase()) }.getOrDefault(PurchaseInvoiceStatus.BROUILLON),
    dateReception = dateReception,
    dateFacture = dateFacture,
    dateEcheance = dateEcheance,
    createdAt = createdAt,
)

private fun parseModePaiement(value: String) =
    runCatching { PaymentMode.valueOf(value.uppercase()) }.getOrDefault(PaymentMode.VIREMENT)

fun PaiementDto.toDomain() = Payment(
    id = id,
    factureId = factureId,
    montant = montant,
    devise = devise,
    modePaiement = parseModePaiement(modePaiement),
    datePaiement = datePaiement,
    reference = reference,
    createdAt = createdAt,
)

fun PaiementFournisseurDto.toDomain() = SupplierPayment(
    id = id,
    factureFournisseurId = factureFournisseur,
    factureNumero = factureNumero,
    fournisseurNom = fournisseurNom,
    montant = montant,
    devise = devise,
    modePaiement = parseModePaiement(modePaiement),
    datePaiement = datePaiement,
    reference = reference,
    createdAt = createdAt,
)

fun <D, T> PaginatedResponse<D>.toComptaPage(mapper: (D) -> T): ComptaPage<T> = ComptaPage(
    items = results.map(mapper),
    totalCount = count,
    hasNext = next != null,
)

private fun parseMontant(value: String): Double = value.toDoubleOrNull() ?: 0.0

fun StatsDto.toDomain() = DashboardStats(
    date = date,
    devise = devise,
    exerciceId = exerciceCourant.id,
    exerciceAnnee = exerciceCourant.annee,
    caMois = parseMontant(ventes.caMois),
    nbFacturesMois = ventes.nbFacturesMois,
    nbImpayees = ventes.impayees.nb,
    montantImpaye = parseMontant(ventes.impayees.montant),
    nbImpayeesEnRetard = ventes.impayees.enRetard.nb,
    montantImpayeEnRetard = parseMontant(ventes.impayees.enRetard.montant),
    nbAchatsEnAttente = achats.enAttentePaiement.nb,
    montantAchatsEnAttente = parseMontant(achats.enAttentePaiement.montant),
    nbAchatsEnRetard = achats.enAttentePaiement.enRetard,
    chargesMois = parseMontant(chargesMois),
    resultatPrevisionnelExercice = parseMontant(resultatPrevisionnelExercice),
    soldeTresorerie = parseMontant(soldeTresorerie),
)
