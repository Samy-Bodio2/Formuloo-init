"""
Views Factures Fournisseurs — cycle achats OHADA.
Workflow : BROUILLON → RECUE → VALIDEE (écriture auto) → PAYEE | ANNULEE
"""

from datetime import date
from decimal import Decimal

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import (
    FactureFournisseur, LigneFactureFournisseur,
    Ecriture, LigneEcriture, Journal, Exercice, Compte,
)
from comptabilite.serializers import (
    FactureFournisseurSerializer, FactureFournisseurCreateSerializer,
    FactureFournisseurUpdateSerializer,
)
from comptabilite.permissions import ComptaPermission

CanReadAchats = ComptaPermission("compta.read.factures")
CanWriteAchats = ComptaPermission("compta.write.factures")
CanDeleteAchats = ComptaPermission("compta.delete.factures")


def _creer_ecriture_achat(facture, tenant_id):
    """
    Écriture d'achat OHADA :
        6xx Achats/Charges (débit) = montant HT
        4452 TVA déductible (débit) = TVA (si > 0)
        401 Fournisseurs (crédit) = montant TTC
    """
    try:
        journal = Journal.objects.get(tenant_id=tenant_id, type="ACHATS")
        exercice = Exercice.objects.filter(
            tenant_id=tenant_id, statut="OUVERT",
            date_debut__lte=facture.date_facture,
            date_fin__gte=facture.date_facture,
        ).first()
        if not exercice:
            return None

        compte_fournisseur = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="401").first()
        if not compte_fournisseur:
            return None

        montant_ht = facture.montant_ht
        montant_ttc = facture.montant_ttc
        tva = facture.tva

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=facture.date_reception or facture.date_facture,
            libelle=f"Achat {facture.numero_fournisseur or facture.numero_interne} — {facture.fournisseur_nom}",
            created_by=getattr(facture, "_created_by", None),
        )

        # Lignes débit : comptes de charges par ligne
        has_specific_comptes = False
        for ligne in facture.lignes.all():
            if ligne.compte_charge:
                LigneEcriture.objects.create(
                    ecriture=ecriture,
                    compte=ligne.compte_charge,
                    libelle=ligne.description,
                    debit=ligne.montant_total,
                    credit=Decimal("0"),
                )
                has_specific_comptes = True

        # Si aucun compte de charge spécifié, utiliser un compte 6 générique
        if not has_specific_comptes:
            compte_achats = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="601").first()
            if not compte_achats:
                ecriture.delete()
                return None
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=compte_achats,
                libelle=f"Achats {facture.fournisseur_nom}",
                debit=montant_ht,
                credit=Decimal("0"),
            )

        # Ligne débit TVA déductible
        if tva > 0:
            compte_tva_deductible = Compte.objects.filter(tenant_id=tenant_id, numero__startswith="4452").first()
            if compte_tva_deductible:
                LigneEcriture.objects.create(
                    ecriture=ecriture,
                    compte=compte_tva_deductible,
                    libelle="TVA déductible",
                    debit=tva,
                    credit=Decimal("0"),
                )

        # Ligne crédit : 401 Fournisseurs (TTC)
        LigneEcriture.objects.create(
            ecriture=ecriture,
            compte=compte_fournisseur,
            libelle=facture.fournisseur_nom,
            debit=Decimal("0"),
            credit=montant_ttc,
        )

        return ecriture
    except Exception:
        return None


class FacturesFournisseursListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadAchats()]
        return [IsAuthenticated(), CanWriteAchats()]

    def get(self, request):
        qs = FactureFournisseur.objects.filter(
            tenant_id=request.user.tenant_id
        ).prefetch_related("lignes")

        statut = request.query_params.get("statut")
        fournisseur = request.query_params.get("fournisseur")
        date_debut = request.query_params.get("date_debut")
        date_fin = request.query_params.get("date_fin")

        if statut:
            qs = qs.filter(statut=statut)
        if fournisseur:
            qs = qs.filter(fournisseur_nom__icontains=fournisseur)
        if date_debut:
            qs = qs.filter(date_facture__gte=date_debut)
        if date_fin:
            qs = qs.filter(date_facture__lte=date_fin)

        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            FactureFournisseurSerializer(page, many=True).data
        )

    def post(self, request):
        ser = FactureFournisseurCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data
        tenant_id = request.user.tenant_id

        facture = FactureFournisseur(
            tenant_id=tenant_id,
            fournisseur_nom=data["fournisseur_nom"],
            fournisseur_email=data.get("fournisseur_email", ""),
            numero_fournisseur=data.get("numero_fournisseur", ""),
            devise=data.get("devise", "XAF"),
            tva_taux=data.get("tva_taux", 0),
            date_facture=data["date_facture"],
            date_echeance=data["date_echeance"],
        )
        facture.generer_numero_interne()
        facture._created_by = getattr(request.user, "auth_user_id", None)
        facture.created_by = facture._created_by
        facture.save()

        for ligne_data in data["lignes"]:
            compte_charge = None
            if ligne_data.get("compte_charge_id"):
                try:
                    compte_charge = Compte.objects.get(
                        pk=ligne_data["compte_charge_id"], tenant_id=tenant_id
                    )
                except Compte.DoesNotExist:
                    pass
            LigneFactureFournisseur.objects.create(
                facture=facture,
                description=ligne_data["description"],
                compte_charge=compte_charge,
                quantite=ligne_data["quantite"],
                prix_unitaire=ligne_data["prix_unitaire"],
            )

        return Response(
            FactureFournisseurSerializer(facture).data, status=status.HTTP_201_CREATED
        )


class FactureFournisseurDetailView(APIView):

    def _get(self, pk, tenant_id):
        try:
            return FactureFournisseur.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=tenant_id
            )
        except FactureFournisseur.DoesNotExist:
            raise NotFound()

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadAchats()]
        if self.request.method == "DELETE":
            return [IsAuthenticated(), CanDeleteAchats()]
        return [IsAuthenticated(), CanWriteAchats()]

    def get(self, request, pk):
        return Response(
            FactureFournisseurSerializer(self._get(pk, request.user.tenant_id)).data
        )

    def put(self, request, pk):
        facture = self._get(pk, request.user.tenant_id)
        if facture.statut not in [FactureFournisseur.Statut.BROUILLON, FactureFournisseur.Statut.RECUE]:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures brouillon ou reçues peuvent être modifiées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ser = FactureFournisseurUpdateSerializer(facture, data=request.data, partial=True)
        ser.is_valid(raise_exception=True)
        ser.save()
        return Response(FactureFournisseurSerializer(facture).data)

    def delete(self, request, pk):
        facture = self._get(pk, request.user.tenant_id)
        if facture.statut in [FactureFournisseur.Statut.VALIDEE, FactureFournisseur.Statut.PAYEE]:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Une facture validée ou payée ne peut pas être annulée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        facture.statut = FactureFournisseur.Statut.ANNULEE
        facture.save()
        return Response(status=status.HTTP_204_NO_CONTENT)


class FactureFournisseurRecevoirView(APIView):
    """POST /achats/{id}/recevoir/ — BROUILLON → RECUE (date de réception enregistrée)"""

    permission_classes = [IsAuthenticated, CanWriteAchats]

    def post(self, request, pk):
        try:
            facture = FactureFournisseur.objects.get(pk=pk, tenant_id=request.user.tenant_id)
        except FactureFournisseur.DoesNotExist:
            raise NotFound()

        if facture.statut != FactureFournisseur.Statut.BROUILLON:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures brouillon peuvent être marquées comme reçues."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        facture.statut = FactureFournisseur.Statut.RECUE
        facture.date_reception = date.today()
        facture.save()
        return Response(FactureFournisseurSerializer(facture).data)


class FactureFournisseurValiderView(APIView):
    """POST /achats/{id}/valider/ — RECUE → VALIDEE + écriture comptable auto"""

    permission_classes = [IsAuthenticated, CanWriteAchats]

    def post(self, request, pk):
        try:
            facture = FactureFournisseur.objects.prefetch_related("lignes").get(
                pk=pk, tenant_id=request.user.tenant_id
            )
        except FactureFournisseur.DoesNotExist:
            raise NotFound()

        if facture.statut != FactureFournisseur.Statut.RECUE:
            return Response(
                {"error": {"code": "STATUT_INVALIDE", "message": "Seules les factures reçues peuvent être validées."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        facture._created_by = getattr(request.user, "auth_user_id", None)
        ecriture = _creer_ecriture_achat(facture, request.user.tenant_id)
        facture.statut = FactureFournisseur.Statut.VALIDEE
        if ecriture:
            facture.ecriture = ecriture
        facture.save()

        return Response(FactureFournisseurSerializer(facture).data)
