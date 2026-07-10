"""
Dashboard statistiques comptables.
GET /stats/   → indicateurs clés pour le tenant courant
"""

from datetime import date, timedelta
from decimal import Decimal
from django.db.models import Sum, Count, Q
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated

from comptabilite.models import (
    Facture, FactureFournisseur, Ecriture, LigneEcriture, Compte, Exercice,
)
from comptabilite.permissions import CanReadEtats


class StatsView(APIView):
    """
    GET /stats/

    Retourne les indicateurs clés du mois courant et de l'exercice ouvert :
      - Chiffre d'affaires (factures émises)
      - Charges du mois
      - Factures clients impayées + en retard
      - Factures fournisseurs en attente de paiement
      - Résultat prévisionnel exercice
      - Solde de trésorerie (comptes 5xx)
    """

    permission_classes = [IsAuthenticated, CanReadEtats]

    def get(self, request):
        tenant_id = request.user.tenant_id
        aujourd_hui = date.today()
        debut_mois = aujourd_hui.replace(day=1)

        # ── Exercice ouvert ────────────────────────────────────
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT"
        ).first()

        # ── Chiffre d'affaires du mois (factures émises) ───────
        ca_mois = Facture.objects.filter(
            tenant_id=tenant_id,
            type_document=Facture.TypeDocument.FACTURE,
            statut__in=[Facture.Statut.EMISE, Facture.Statut.PARTIELLEMENT_PAYEE, Facture.Statut.PAYEE],
            date_emission__gte=debut_mois,
            date_emission__lte=aujourd_hui,
        ).aggregate(total=Sum("lignes__prix_unitaire"))

        # Calcul CA via les lignes de factures
        factures_mois = Facture.objects.filter(
            tenant_id=tenant_id,
            type_document=Facture.TypeDocument.FACTURE,
            statut__in=[Facture.Statut.EMISE, Facture.Statut.PARTIELLEMENT_PAYEE, Facture.Statut.PAYEE],
            date_emission__gte=debut_mois,
        ).prefetch_related("lignes")
        ca_mois_total = sum(f.montant_ttc for f in factures_mois)

        # ── Factures clients impayées ──────────────────────────
        factures_impayees = Facture.objects.filter(
            tenant_id=tenant_id,
            type_document=Facture.TypeDocument.FACTURE,
            statut__in=[Facture.Statut.EMISE, Facture.Statut.PARTIELLEMENT_PAYEE],
        ).prefetch_related("lignes", "paiements")
        nb_impayees = factures_impayees.count()
        montant_impaye = sum(f.solde_restant_du for f in factures_impayees)
        nb_en_retard = sum(
            1 for f in factures_impayees if f.date_echeance < aujourd_hui
        )
        montant_en_retard = sum(
            f.solde_restant_du for f in factures_impayees
            if f.date_echeance < aujourd_hui
        )

        # ── Factures fournisseurs en attente ───────────────────
        achats_en_attente = FactureFournisseur.objects.filter(
            tenant_id=tenant_id,
            statut__in=[
                FactureFournisseur.Statut.VALIDEE,
                FactureFournisseur.Statut.PARTIELLEMENT_PAYEE,
            ],
        ).prefetch_related("lignes", "paiements_fournisseur")
        nb_achats_attente = achats_en_attente.count()
        montant_achats_attente = sum(f.solde_restant_du for f in achats_en_attente)
        nb_achats_en_retard = sum(
            1 for f in achats_en_attente if f.date_echeance < aujourd_hui
        )

        # ── Charges du mois (écritures classe 6) ──────────────
        charges_mois = Decimal("0")
        if exercice:
            comptes_charges = Compte.objects.filter(tenant_id=tenant_id, classe=6)
            for cc in comptes_charges:
                agg = LigneEcriture.objects.filter(
                    compte=cc,
                    ecriture__exercice=exercice,
                    ecriture__tenant_id=tenant_id,
                    ecriture__statut=Ecriture.Statut.VALIDEE,
                    ecriture__date_ecriture__gte=debut_mois,
                ).aggregate(total=Sum("debit"))
                charges_mois += agg["total"] or Decimal("0")

        # ── Résultat prévisionnel de l'exercice ────────────────
        resultat_previsionnel = Decimal("0")
        if exercice:
            produits_exercice = Decimal("0")
            charges_exercice = Decimal("0")
            for cp in Compte.objects.filter(tenant_id=tenant_id, classe=7):
                agg = LigneEcriture.objects.filter(
                    compte=cp, ecriture__exercice=exercice,
                    ecriture__tenant_id=tenant_id,
                    ecriture__statut=Ecriture.Statut.VALIDEE,
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                produits_exercice += (agg["tc"] or Decimal("0")) - (agg["td"] or Decimal("0"))
            for cc in Compte.objects.filter(tenant_id=tenant_id, classe__in=[6, 8]):
                agg = LigneEcriture.objects.filter(
                    compte=cc, ecriture__exercice=exercice,
                    ecriture__tenant_id=tenant_id,
                    ecriture__statut=Ecriture.Statut.VALIDEE,
                ).aggregate(td=Sum("debit"), tc=Sum("credit"))
                charges_exercice += (agg["td"] or Decimal("0")) - (agg["tc"] or Decimal("0"))
            resultat_previsionnel = produits_exercice - charges_exercice

        # ── Solde trésorerie (comptes 5xx validés) ─────────────
        solde_tresorerie = Decimal("0")
        comptes_tresorerie = Compte.objects.filter(
            tenant_id=tenant_id, classe=5
        ).exclude(type_compte="PASSIF")
        for ct in comptes_tresorerie:
            agg = LigneEcriture.objects.filter(
                compte=ct,
                ecriture__tenant_id=tenant_id,
                ecriture__statut=Ecriture.Statut.VALIDEE,
            ).aggregate(td=Sum("debit"), tc=Sum("credit"))
            td = agg["td"] or Decimal("0")
            tc = agg["tc"] or Decimal("0")
            solde_tresorerie += td - tc

        return Response({
            "date": str(aujourd_hui),
            "devise": "XAF",
            "exercice_courant": {
                "id": exercice.id if exercice else None,
                "annee": exercice.annee if exercice else None,
            },
            "ventes": {
                "ca_mois": str(ca_mois_total),
                "nb_factures_mois": factures_mois.count(),
                "impayees": {
                    "nb": nb_impayees,
                    "montant": str(montant_impaye),
                    "en_retard": {"nb": nb_en_retard, "montant": str(montant_en_retard)},
                },
            },
            "achats": {
                "en_attente_paiement": {
                    "nb": nb_achats_attente,
                    "montant": str(montant_achats_attente),
                    "en_retard": nb_achats_en_retard,
                },
            },
            "charges_mois": str(charges_mois),
            "resultat_previsionnel_exercice": str(resultat_previsionnel),
            "solde_tresorerie": str(solde_tresorerie),
        })
