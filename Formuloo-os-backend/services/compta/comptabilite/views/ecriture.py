from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from rest_framework.exceptions import NotFound

from comptabilite.models import Ecriture, LigneEcriture, Journal, Exercice, Compte
from comptabilite.serializers import EcritureSerializer, EcritureCreateSerializer
from comptabilite.permissions import (
    CanReadEcritures, CanWriteEcritures,
    CanDeleteEcritures, CanValidateEcritures,
)


class EcrituresListView(APIView):

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadEcritures()]
        return [IsAuthenticated(), CanWriteEcritures()]

    def get(self, request):
        qs = Ecriture.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("journal", "exercice").prefetch_related("lignes__compte")

        journal_id = request.query_params.get("journal_id")
        exercice_id = request.query_params.get("exercice_id")
        date_debut = request.query_params.get("date_debut")
        date_fin = request.query_params.get("date_fin")
        statut = request.query_params.get("statut")

        if journal_id:
            qs = qs.filter(journal_id=journal_id)
        if exercice_id:
            qs = qs.filter(exercice_id=exercice_id)
        if date_debut:
            qs = qs.filter(date_ecriture__gte=date_debut)
        if date_fin:
            qs = qs.filter(date_ecriture__lte=date_fin)
        if statut:
            qs = qs.filter(statut=statut)

        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(qs, request)
        return paginator.get_paginated_response(
            EcritureSerializer(page, many=True).data
        )

    def post(self, request):
        ser = EcritureCreateSerializer(data=request.data)
        ser.is_valid(raise_exception=True)
        data = ser.validated_data
        tenant_id = request.user.tenant_id

        try:
            journal = Journal.objects.get(pk=data["journal_id"], tenant_id=tenant_id)
        except Journal.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Journal introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            exercice = Exercice.objects.get(pk=data["exercice_id"], tenant_id=tenant_id)
        except Exercice.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Exercice introuvable."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not exercice.is_ouvert:
            return Response(
                {"error": {"code": "EXERCICE_CLOTURE", "message": "L'exercice est clôturé."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Vérifier que tous les comptes existent
        compte_ids = [l["compte_id"] for l in data["lignes"]]
        comptes = {c.id: c for c in Compte.objects.filter(pk__in=compte_ids, tenant_id=tenant_id)}
        manquants = [cid for cid in compte_ids if cid not in comptes]
        if manquants:
            return Response(
                {"error": {"code": "COMPTE_NOT_FOUND", "message": f"Comptes introuvables : {manquants}"}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        ecriture = Ecriture.objects.create(
            tenant_id=tenant_id,
            journal=journal,
            exercice=exercice,
            date_ecriture=data["date_ecriture"],
            libelle=data["libelle"],
            reference_piece=data.get("reference_piece", ""),
        )
        for ligne_data in data["lignes"]:
            LigneEcriture.objects.create(
                ecriture=ecriture,
                compte=comptes[ligne_data["compte_id"]],
                libelle=ligne_data.get("libelle", ""),
                debit=ligne_data["debit"],
                credit=ligne_data["credit"],
            )

        ecriture.refresh_from_db()
        return Response(
            EcritureSerializer(ecriture).data, status=status.HTTP_201_CREATED
        )


class EcritureDetailView(APIView):

    def _get(self, pk, tenant_id):
        try:
            return Ecriture.objects.prefetch_related(
                "lignes__compte"
            ).select_related("journal").get(pk=pk, tenant_id=tenant_id)
        except Ecriture.DoesNotExist:
            raise NotFound()

    def get_permissions(self):
        if self.request.method == "GET":
            return [IsAuthenticated(), CanReadEcritures()]
        return [IsAuthenticated(), CanDeleteEcritures()]

    def get(self, request, pk):
        return Response(EcritureSerializer(self._get(pk, request.user.tenant_id)).data)

    def delete(self, request, pk):
        ecriture = self._get(pk, request.user.tenant_id)
        if ecriture.statut == Ecriture.Statut.VALIDEE:
            return Response(
                {"error": {"code": "ECRITURE_VALIDEE", "message": "Une écriture validée ne peut pas être supprimée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )
        ecriture.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class EcritureValiderView(APIView):
    """POST /ecritures/{id}/valider/ — irréversible."""

    permission_classes = [IsAuthenticated, CanValidateEcritures]

    def post(self, request, pk):
        try:
            ecriture = Ecriture.objects.prefetch_related(
                "lignes__compte"
            ).get(pk=pk, tenant_id=request.user.tenant_id)
        except Ecriture.DoesNotExist:
            raise NotFound()

        if ecriture.statut == Ecriture.Statut.VALIDEE:
            return Response(
                {"error": {"code": "DEJA_VALIDEE", "message": "Cette écriture est déjà validée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if not ecriture.est_equilibree:
            return Response(
                {"error": {"code": "DESEQUILIBREE", "message": "L'écriture est déséquilibrée."}},
                status=status.HTTP_400_BAD_REQUEST,
            )

        ecriture.statut = Ecriture.Statut.VALIDEE
        ecriture.save()
        return Response(EcritureSerializer(ecriture).data)
