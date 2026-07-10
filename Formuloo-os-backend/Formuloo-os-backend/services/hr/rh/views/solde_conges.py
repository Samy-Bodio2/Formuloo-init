"""
Views SoldeConges — Formuloo OS
Suivi des soldes de congés par employé.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET /soldes-conges/       → IsManagerOrRH
    GET /soldes-conges/{id}/  → IsOwnerOrRH
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import SoldeConges
from rh.permissions import IsManagerOrRH, IsOwnerOrRH
from rh.serializers import SoldeCongesSerializer


class SoldesCongesListView(APIView):
    """
    GET /api/v1/hr/soldes-conges/ — Liste des soldes
    """

    permission_classes = [IsAuthenticated, IsManagerOrRH]

    @extend_schema(
        summary="Lister les soldes de congés",
        description="Retourne les soldes de congés " "du tenant.",
        tags=["SoldesConges"],
        responses={
            200: SoldeCongesSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        soldes = SoldeConges.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("employee")

        # Filtre employé
        employe_id = request.query_params.get("employe_id")
        if employe_id:
            soldes = soldes.filter(employee_id=employe_id)

        # Filtre année
        annee = request.query_params.get("annee")
        if annee:
            soldes = soldes.filter(annee=annee)

        # Filtre type_conge
        type_conge = request.query_params.get("type_conge")
        if type_conge:
            soldes = soldes.filter(type_conge=type_conge)

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = soldes.count()
        page_data = soldes[start:end]

        serializer = SoldeCongesSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/soldes-conges/" f"?page={page + 1}"
                    if end < total
                    else None
                ),
                "previous": (
                    f"/api/v1/hr/soldes-conges/" f"?page={page - 1}"
                    if page > 1
                    else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )


class SoldeCongesDetailView(APIView):
    """
    GET /api/v1/hr/soldes-conges/{id}/ — Détail
    """

    permission_classes = [IsAuthenticated, IsOwnerOrRH]

    @extend_schema(
        summary="Détail d'un solde de congés",
        tags=["SoldesConges"],
        responses={
            200: SoldeCongesSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Solde introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            solde = SoldeConges.objects.get(id=pk, tenant_id=request.user.tenant_id)
        except SoldeConges.DoesNotExist:
            return Response(
                {
                    "error": {
                        "code": "NOT_FOUND",
                        "message": "Solde de congés introuvable.",
                    }
                },
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, solde)

        serializer = SoldeCongesSerializer(solde)
        return Response(serializer.data, status=status.HTTP_200_OK)
