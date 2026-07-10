"""
Avoirs (notes de crédit) — clients et fournisseurs.
POST /factures/{id}/avoir/          → avoir sur facture client
POST /achats/{id}/avoir/            → avoir sur facture fournisseur
"""

from datetime import date
from decimal import Decimal

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import NotFound

from comptabilite.models import (
    Facture, LigneFacture, Ecriture, LigneEcriture,
    FactureFournisseur, LigneFactureFournisseur,
    Journal, Exercice, Compte,
)
from comptabilite.serializers import FactureSerializer
from comptabilite.serializers.facture_fournisseur import FactureFournisseurSerializer
from comptabilite.permissions import CanWriteFactures


def _creer_ecriture_avoir_vente(avoir, tenant_id):
    """
    Écriture avoir vente (inverse de l'écriture de vente) :
        701 Ventes (débit) = montant HT
        4431 TVA collectée (débit) = TVA
        411 Client (crédit) = montant TTC
    """
    try:
        journal = Journal.objects.get(tenant_id=tenant_id, type="VENTES")
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT",
            date_debut__lte=avoir.date_emission,
            date_fin__gte=avoir.date_emission,
        ).first()
        if not exercice:
            return None

        compte_client = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="411").first()
        compte_ventes = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="701").first()
        if not compte_client or not compte_ventes:
            return None

        montant_ht = avoir.montant_ht
        montant_ttc = avoir.montant_ttc
        tva = avoir.tva

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=avoir.date_emission,
            libelle=f"Avoir {avoir.numero} — {avoir.client_nom}",
            created_by=getattr(avoir, "_created_by", None),
        )

        if tva > 0:
            compte_tva = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4431").first()
            LigneEcriture.objects.create(
                ecriture=ecriture, compte=compte_ventes,
                libelle="Annulation ventes HT", debit=montant_ht, credit=Decimal("0"),
            )
            if compte_tva:
                LigneEcriture.objects.create(
                    ecriture=ecriture, compte=compte_tva,
                    libelle="Annulation TVA collectée", debit=tva, credit=Decimal("0"),
                )
        else:
            LigneEcriture.objects.create(
                ecriture=ecriture, compte=compte_ventes,
                libelle="Annulation ventes", debit=montant_ttc, credit=Decimal("0"),
            )

        LigneEcriture.objects.create(
            ecriture=ecriture, compte=compte_client,
            libelle=avoir.client_nom, debit=Decimal("0"), credit=montant_ttc,
        )
        return ecriture
    except Exception:
        return None


class AvoirClientView(APIView):
    """POST /factures/{id}/avoir/ — génère un avoir sur une facture client émise."""

    permission_classes = [IsAuthenticated, CanWriteFactures]

    def post(self, request, pk):
        tenant_id = request.user.tenant_id
        try:
            facture_origine = Facture.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=tenant_id
            )
        except Facture.DoesNotExist:
            raise NotFound()

        if facture_origine.statut not in (Facture.Statut.EMISE, Facture.Statut.PARTIELLEMENT_PAYEE):
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Un avoir ne peut être émis que sur une facture émise ou partiellement payée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if facture_origine.type_document != Facture.TypeDocument.FACTURE:
            return Response(
                {"error": {"code": "DEJA_AVOIR", "message": "Impossible de créer un avoir sur un avoir."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        montant_avoir = request.data.get("montant_avoir")  # None = avoir total

        avoir = Facture(
            tenant_id=tenant_id,
            type_document=Facture.TypeDocument.AVOIR,
            facture_origine=facture_origine,
            client_nom=facture_origine.client_nom,
            client_email=facture_origine.client_email,
            devise=facture_origine.devise,
            tva_taux=facture_origine.tva_taux,
            date_emission=date.today(),
            date_echeance=date.today(),
            statut=Facture.Statut.EMISE,
        )
        avoir.generer_numero()
        avoir._created_by = getattr(request.user, "auth_user_id", None)
        avoir.created_by = avoir._created_by
        avoir.save()

        # Copier les lignes (ou partiel si montant_avoir spécifié)
        for ligne in facture_origine.lignes.all():
            LigneFacture.objects.create(
                facture=avoir,
                description=f"[AVOIR] {ligne.description}",
                quantite=ligne.quantite,
                prix_unitaire=ligne.prix_unitaire,
            )

        ecriture = _creer_ecriture_avoir_vente(avoir, tenant_id)
        if ecriture:
            avoir.ecriture = ecriture
            avoir.save(update_fields=["ecriture"])

        return Response(
            FactureSerializer(avoir).data, status=status.HTTP_201_CREATED
        )


class AvoirFournisseurView(APIView):
    """POST /achats/{id}/avoir/ — génère un avoir sur une facture fournisseur validée."""

    permission_classes = [IsAuthenticated, CanWriteFactures]

    def post(self, request, pk):
        tenant_id = request.user.tenant_id
        try:
            facture_origine = FactureFournisseur.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=tenant_id
            )
        except FactureFournisseur.DoesNotExist:
            raise NotFound()

        if facture_origine.statut not in (
            FactureFournisseur.Statut.VALIDEE,
            FactureFournisseur.Statut.PARTIELLEMENT_PAYEE,
        ):
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Un avoir fournisseur ne peut être créé que sur une facture validée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        avoir = FactureFournisseur(
            tenant_id=tenant_id,
            type_document=FactureFournisseur.TypeDocument.AVOIR,
            facture_origine=facture_origine,
            fournisseur_nom=facture_origine.fournisseur_nom,
            fournisseur_email=facture_origine.fournisseur_email,
            devise=facture_origine.devise,
            tva_taux=facture_origine.tva_taux,
            date_facture=date.today(),
            date_echeance=date.today(),
            statut=FactureFournisseur.Statut.VALIDEE,
        )
        avoir.generer_numero_interne()
        avoir.created_by = getattr(request.user, "auth_user_id", None)
        avoir.save()

        for ligne in facture_origine.lignes.all():
            LigneFactureFournisseur.objects.create(
                facture=avoir,
                description=f"[AVOIR] {ligne.description}",
                compte_charge=ligne.compte_charge,
                quantite=ligne.quantite,
                prix_unitaire=ligne.prix_unitaire,
            )

        return Response(
            FactureFournisseurSerializer(avoir).data, status=status.HTTP_201_CREATED
        )
