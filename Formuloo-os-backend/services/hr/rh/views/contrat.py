"""
Views Contrat — Formuloo OS
CRUD des contrats de travail.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET    /contrats/        → IsManagerOrRH
    POST   /contrats/        → IsRHManager
    GET    /contrats/{id}/   → IsOwnerOrRH
    PUT    /contrats/{id}/   → IsRHManager
    DELETE /contrats/{id}/   → IsRHManager

Audit :
    POST   → CREATE_CONTRAT
    PUT    → UPDATE_CONTRAT
    DELETE → RESILIER_CONTRAT
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Contrat
from rh.permissions import (
    IsManagerOrRH,
    IsOwnerOrRH,
    IsRHManager,
)
from rh.serializers import (
    ContratCreateSerializer,
    ContratSerializer,
)
from rh.services.audit import audit_contrat


class ContratsListView(APIView):
    """
    GET  /api/v1/hr/contrats/ — Liste des contrats
    POST /api/v1/hr/contrats/ — Créer un contrat
    """

    def get_permissions(self):
        """
        GET  → IsManagerOrRH
        POST → IsRHManager
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsManagerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    @extend_schema(
        summary="Lister les contrats",
        description="Retourne la liste paginée " "des contrats du tenant.",
        tags=["Contrats"],
        responses={
            200: ContratSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        contrats = Contrat.objects.filter(
            tenant_id=request.user.tenant_id
        ).select_related("employee")

        # Filtre employé
        employe_id = request.query_params.get("employe_id")
        if employe_id:
            contrats = contrats.filter(employee_id=employe_id)

        # Filtre type
        type_contrat = request.query_params.get("type_contrat")
        if type_contrat:
            contrats = contrats.filter(type=type_contrat)

        # Filtre statut
        statut = request.query_params.get("statut")
        if statut:
            contrats = contrats.filter(statut=statut)

        # Filtre is_active
        is_active = request.query_params.get("is_active")
        if is_active is not None:
            contrats = contrats.filter(is_active=is_active.lower() == "true")

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = contrats.count()
        page_data = contrats[start:end]

        serializer = ContratSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/contrats/" f"?page={page + 1}" if end < total else None
                ),
                "previous": (
                    f"/api/v1/hr/contrats/" f"?page={page - 1}" if page > 1 else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Créer un contrat",
        tags=["Contrats"],
        request=ContratCreateSerializer,
        responses={
            201: ContratSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = ContratCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        contrat = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_contrat("CREATE_CONTRAT", contrat, request)

        return Response(ContratSerializer(contrat).data, status=status.HTTP_201_CREATED)


class ContratDetailView(APIView):
    """
    GET    /api/v1/hr/contrats/{id}/ — Détail
    PUT    /api/v1/hr/contrats/{id}/ — Modifier
    DELETE /api/v1/hr/contrats/{id}/ — Résilier
    """

    def get_permissions(self):
        """
        GET    → IsOwnerOrRH
        PUT    → IsRHManager
        DELETE → IsRHManager
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsOwnerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    def get_object(self, pk, tenant_id):
        try:
            return Contrat.objects.get(id=pk, tenant_id=tenant_id)
        except Contrat.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un contrat",
        tags=["Contrats"],
        responses={
            200: ContratSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Contrat introuvable"},
        },
    )
    def get(self, request, pk):
        contrat = self.get_object(pk, request.user.tenant_id)
        if not contrat:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Contrat introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier ownership
        self.check_object_permissions(request, contrat)

        serializer = ContratSerializer(contrat)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un contrat",
        tags=["Contrats"],
        request=ContratCreateSerializer,
        responses={
            200: ContratSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Contrat introuvable"},
        },
    )
    def put(self, request, pk):
        contrat = self.get_object(pk, request.user.tenant_id)
        if not contrat:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Contrat introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = ContratCreateSerializer(
            contrat, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        contrat = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_contrat("UPDATE_CONTRAT", contrat, request)

        return Response(ContratSerializer(contrat).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Résilier un contrat",
        tags=["Contrats"],
        responses={
            204: {"description": "Contrat résilié"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Contrat introuvable"},
        },
    )
    def delete(self, request, pk):
        contrat = self.get_object(pk, request.user.tenant_id)
        if not contrat:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Contrat introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # ── Audit avant résiliation ────────────────────────
        audit_contrat("RESILIER_CONTRAT", contrat, request)

        # Soft delete — résilier le contrat
        contrat.statut = Contrat.StatutContrat.RESILIE
        contrat.is_active = False
        contrat.save(update_fields=["statut", "is_active", "updated_at"])

        return Response(status=status.HTTP_204_NO_CONTENT)
