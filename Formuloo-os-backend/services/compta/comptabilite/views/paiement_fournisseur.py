"""
Paiements fournisseurs.
POST /achats/{id}/payer/                    → enregistre un paiement
GET  /paiements-fournisseurs/               → liste paginée
GET  /paiements-fournisseurs/{id}/          → détail

Écriture OHADA automatique :
    401 Fournisseurs (débit) = montant
    521 Banque / 571 Caisse / 532 Mobile Money (crédit) = montant
"""

from decimal import Decimal
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import (
    PaiementFournisseur, FactureFournisseur,
    Ecriture, LigneEcriture, Journal, Exercice, Compte,
)
from comptabilite.serializers.paiement_fournisseur import (
    PaiementFournisseurSerializer, PaiementFournisseurCreateSerializer,
)
from comptabilite.permissions import CanReadPaiements, CanWritePaiements


# Mapping mode_paiement → compte trésorerie SYSCOHADA
COMPTE_TRESORERIE_MAP = {
    "VIREMENT":     "521",
    "CHEQUE":       "521",
    "ESPECES":      "571",
    "MOBILE_MONEY": "532",
}

# Mapping mode_paiement → type de journal
JOURNAL_TYPE_MAP = {
    "VIREMENT":     "BANQUE",
    "CHEQUE":       "BANQUE",
    "ESPECES":      "CAISSE",
    "MOBILE_MONEY": "BANQUE",
}


def _creer_ecriture_paiement_fournisseur(paiement, facture, tenant_id):
    """
    401 Fournisseurs (débit) = montant payé
    521/571/532 Trésorerie (crédit) = montant payé
    """
    try:
        type_journal = JOURNAL_TYPE_MAP.get(paiement.mode_paiement, "BANQUE")
        journal = Journal.objects.filter(tenant_id=tenant_id, type=type_journal).first()
        if not journal:
            return None

        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT",
            date_debut__lte=paiement.date_paiement,
            date_fin__gte=paiement.date_paiement,
        ).first()
        if not exercice:
            return None

        prefix_tresorerie = COMPTE_TRESORERIE_MAP.get(paiement.mode_paiement, "521")
        compte_tresorerie = Compte.objects.filter(
            tenant_id=tenant_id, numero__startswith=prefix_tresorerie
        ).first()
        compte_fournisseur = Compte.objects.filter(
            tenant_id=tenant_id, numero__startswith="401"
        ).first()
        if not compte_tresorerie or not compte_fournisseur:
            return None

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=paiement.date_paiement,
            libelle=f"Paiement {facture.numero_interne} — {facture.fournisseur_nom}",
            created_by=getattr(paiement, "_created_by", None),
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_fournisseur,
            libelle=facture.fournisseur_nom,
            debit=paiement.montant,
            credit=Decimal("0"),
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_tresorerie,
            libelle=f"Règlement {paiement.reference or paiement.mode_paiement}",
            debit=Decimal("0"),
            credit=paiement.montant,
        )
        return ecriture
    except Exception:
        return None


class PayerFactureFournisseurView(APIView):
    """POST /achats/{id}/payer/ — enregistre un paiement partiel ou total."""

    permission_classes = [IsAuthenticated, CanWritePaiements]

    def post(self, request, pk):
        tenant_id = request.user.tenant_id
        try:
            facture = FactureFournisseur.objects.get(pk=pk, tenant_id=tenant_id)
        except FactureFournisseur.DoesNotExist:
            raise NotFound()

        if facture.statut not in (
            FactureFournisseur.Statut.VALIDEE,
            FactureFournisseur.Statut.PARTIELLEMENT_PAYEE,
        ):
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures validées peuvent être payées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        ser = PaiementFournisseurCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data

        montant = data["montant"]
        solde_restant = facture.solde_restant_du
        if montant > solde_restant:
            return Response(
                {
                    "error": {
                        "code": "MONTANT_EXCESSIF",
                        "message": f"Le montant ({montant}) dépasse le solde restant dû ({solde_restant}).",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        paiement = PaiementFournisseur(
            tenant_id=tenant_id,
            facture_fournisseur=facture,
            montant=montant,
            devise=facture.devise,
            mode_paiement=data["mode_paiement"],
            date_paiement=data["date_paiement"],
            reference=data.get("reference", ""),
            created_by=getattr(request.user, "auth_user_id", None),
        )
        paiement._created_by = paiement.created_by
        paiement.save()

        ecriture = _creer_ecriture_paiement_fournisseur(paiement, facture, tenant_id)
        if ecriture:
            paiement.ecriture = ecriture
            paiement.save(update_fields=["ecriture"])

        facture.mettre_a_jour_statut_paiement()

        return Response(
            PaiementFournisseurSerializer(paiement).data,
            status=status.HTTP_201_CREATED,
        )


class PaiementsFournisseursListView(APIView):

    permission_classes = [IsAuthenticated, CanReadPaiements]

    def get(self, request):
        qs = PaiementFournisseur.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("facture_fournisseur")

        facture_id = request.query_params.get("facture_id")
        if facture_id:
            qs = qs.filter(facture_fournisseur_id=facture_id)

        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            PaiementFournisseurSerializer(page, many=True).data
        )


class PaiementFournisseurDetailView(APIView):

    permission_classes = [IsAuthenticated, CanReadPaiements]

    def get(self, request, pk):
        try:
            paiement = PaiementFournisseur.objects.get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except PaiementFournisseur.DoesNotExist:
            raise NotFound()
        return Response(PaiementFournisseurSerializer(paiement).data)
