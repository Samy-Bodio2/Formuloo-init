from datetime import date
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Facture, LigneFacture, Ecriture, LigneEcriture, Journal, Exercice, Compte
from comptabilite.serializers import FactureSerializer, FactureCreateSerializer, FactureUpdateSerializer
from comptabilite.permissions import CanReadFactures, CanWriteFactures, CanDeleteFactures
from comptabilite.services.email import envoyer_facture_client


def _creer_ecriture_vente(facture, tenant_id):
    """
    Génère automatiquement l'écriture de vente au journal VTE.
    411 Client (débit) = montant TTC
    701 Ventes (crédit) = montant HT
    4431 TVA collectée (crédit) = TVA (si tva_taux > 0)

    Si les comptes ou le journal n'existent pas → pas d'écriture auto.
    """
    try:
        journal = Journal.objects.get(tenant_id=tenant_id, type="VENTES")
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT",
            date_debut__lte=facture.date_emission,
            date_fin__gte=facture.date_emission,
        ).first()
        if not exercice:
            return None

        compte_client = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="411").first()
        compte_ventes = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="701").first()
        if not compte_client or not compte_ventes:
            return None

        montant_ht = facture.montant_ht
        montant_ttc = facture.montant_ttc
        tva = facture.tva

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=facture.date_emission,
            libelle=f"Facture {facture.numero} — {facture.client_nom}",
        )
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_client,
            libelle=facture.client_nom,
            debit=montant_ttc,
            credit=0,
        )
        if tva > 0:
            compte_tva = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4431").first()
            if compte_tva:
                LigneEcriture.objects.create(
                    ecriture=ecriture,
                    compte=compte_ventes,
                    libelle="Ventes HT",
                    debit=0,
                    credit=montant_ht,
                )
                LigneEcriture.objects.create(
                    ecriture=ecriture,
                    compte=compte_tva,
                    libelle="TVA collectée",
                    debit=0,
                    credit=tva,
                )
            else:
                LigneEcriture.objects.create(
                    ecriture=ecriture,
                    compte=compte_ventes,
                    libelle="Ventes",
                    debit=0,
                    credit=montant_ttc,
                )
        else:
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=compte_ventes,
                libelle="Ventes",
                debit=0,
                credit=montant_ttc,
            )
        return ecriture
    except Exception:
        return None


class FacturesListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadFactures()]
        return [IsAuthenticated(), CanWriteFactures()]

    def get(self, request):
        qs = Facture.objects.filter(
            tenant_id=request.user.tenant_id
        ).prefetch_related("lignes")

        statut = request.query_params.get("statut")
        date_debut = request.query_params.get("date_debut")
        date_fin = request.query_params.get("date_fin")

        if statut:
            qs = qs.filter(statut=statut)
        if date_debut:
            qs = qs.filter(date_echeance__gte=date_debut)
        if date_fin:
            qs = qs.filter(date_echeance__lte=date_fin)

        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            FactureSerializer(page, many=True).data
        )

    def post(self, request):
        ser = FactureCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data
        tenant_id = request.user.tenant_id

        facture = Facture(
            tenant_id=tenant_id,
            client_nom=data["client_nom"],
            client_email=data.get("client_email", ""),
            devise=data.get("devise", "XAF"),
            tva_taux=data.get("tva_taux", 0),
            date_echeance=data["date_echeance"],
        )
        facture.generer_numero()
        facture.save()

        for ligne_data in data["lignes"]:
            LigneFacture.objects.create(
                facture=facture,
                description=ligne_data["description"],
                quantite=ligne_data["quantite"],
                prix_unitaire=ligne_data["prix_unitaire"],
            )

        return Response(
            FactureSerializer(facture).data, status=status.HTTP_201_CREATED
        )


class FactureDetailView(APIView):

    def _get(self, pk, tenant_id):
        try:
            return Facture.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=tenant_id
            )
        except Facture.DoesNotExist:
            raise NotFound()

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadFactures()]
        if self.request.method == "DELETE":
            return [IsAuthenticated(), CanDeleteFactures()]
        return [IsAuthenticated(), CanWriteFactures()]

    def get(self, request, pk):
        return Response(FactureSerializer(self._get(pk, request.user.tenant_id)).data)

    def put(self, request, pk):
        facture = self._get(pk, request.user.tenant_id)
        if facture.statut != Facture.Statut.BROUILLON:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures brouillon peuvent être modifiées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ser = FactureUpdateSerializer(facture, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return Response(FactureSerializer(facture).data)

    def delete(self, request, pk):
        facture = self._get(pk, request.user.tenant_id)
        if facture.statut == Facture.Statut.PAYEE:
            return Response(
                {"error": {"code": "FACTURE_PAYEE", "message": "Une facture payée ne peut pas être annulée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        facture.statut = Facture.Statut.ANNULEE
        facture.save()
        return Response(status=status.HTTP_204_NO_CONTENT)


class FactureEmettreView(APIView):
    """POST /factures/{id}/emettre/ — BROUILLON → EMISE + écriture auto."""

    permission_classes = [IsAuthenticated, CanWriteFactures]

    def post(self, request, pk):
        try:
            facture = Facture.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except Facture.DoesNotExist:
            raise NotFound()

        if facture.statut != Facture.Statut.BROUILLON:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures brouillon peuvent être émises."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        facture.statut = Facture.Statut.EMISE
        facture.date_emission = date.today()
        ecriture = _creer_ecriture_vente(facture, request.user.tenant_id)
        if ecriture:
            facture.ecriture = ecriture
        facture.save()

        # Notification email au client (non bloquante si SMTP non configuré)
        envoyer_facture_client(facture)

        return Response(FactureSerializer(facture).data)
