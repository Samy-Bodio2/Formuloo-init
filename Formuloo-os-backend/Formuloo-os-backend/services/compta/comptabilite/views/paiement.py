from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Paiement, Facture, Ecriture, LigneEcriture, Journal, Exercice, Compte
from comptabilite.serializers import PaiementSerializer, PaiementCreateSerializer
from comptabilite.permissions import CanReadPaiements, CanWritePaiements


def _creer_ecriture_tresorerie(paiement, facture, tenant_id):
    """
    Génère l'écriture de trésorerie au journal BANQUE ou CAISSE.
    521 Banque (débit) = montant
    411 Client (crédit) = montant
    """
    try:
        type_journal = "BANQUE" if paiement.mode_paiement == "VIREMENT" else "CAISSE"
        journal = Journal.objects.get(tenant_id=tenant_id, type=type_journal)
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT",
            date_debut__lte=paiement.date_paiement,
            date_fin__gte=paiement.date_paiement,
        ).first()
        if not exercice:
            return None

        compte_tresorerie = Compte.objects.filter(
            tenant_id=tenant_id,
            numero__startswith="521" if type_journal == "BANQUE" else "571",
        ).first()
        compte_client = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="411").first()
        if not compte_tresorerie or not compte_client:
            return None

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=paiement.date_paiement,
            libelle=f"Paiement {facture.numero} — {facture.client_nom}",
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_tresorerie,
            libelle="Encaissement",
            debit=paiement.montant,
            credit=0,
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_client,
            libelle=facture.client_nom,
            debit=0,
            credit=paiement.montant,
        )
        return ecriture
    except Exception:
        return None


class PaiementsListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadPaiements()]
        return [IsAuthenticated(), CanWritePaiements()]

    def get(self, request):
        qs = Paiement.objects.filter(tenant_id=request.user.tenant_id)
        facture_id = request.query_params.get("facture_id")
        if facture_id:
            qs = qs.filter(facture_id=facture_id)
        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            PaiementSerializer(page, many=True).data
        )

    def post(self, request):
        ser = PaiementCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data
        tenant_id = request.user.tenant_id

        try:
            facture = Facture.objects.get(pk=data["facture_id"], tenant_id=tenant_id)
        except Facture.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Facture introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if facture.statut not in [Facture.Statut.EMISE, Facture.Statut.PAYEE]:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures émises peuvent être payées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        paiement = Paiement.objects.create(
            tenant_id=tenant_id,
            facture=facture,
            montant=data["montant"],
            devise=facture.devise,
            mode_paiement=data["mode_paiement"],
            date_paiement=data["date_paiement"],
            reference=data.get("reference", ""),
        )

        ecriture = _creer_ecriture_tresorerie(paiement, facture, tenant_id)
        if ecriture:
            paiement.ecriture = ecriture
            paiement.save()

        # Marquer la facture comme payée si le montant total est couvert
        total_paye = sum(p.montant for p in facture.paiements.all())
        if total_paye >= facture.montant_ttc:
            facture.statut = Facture.Statut.PAYEE
            facture.save()

        return Response(PaiementSerializer(paiement).data, status=status.HTTP_201_CREATED)


class PaiementDetailView(APIView):

    permission_classes = [IsAuthenticated, CanReadPaiements]

    def get(self, request, pk):
        try:
            paiement = Paiement.objects.get(pk=pk, tenant_id=request.user.tenant_id)
        except Paiement.DoesNotExist:
            raise NotFound()
        return Response(PaiementSerializer(paiement).data)
