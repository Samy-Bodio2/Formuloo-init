"""
Views Département — Formuloo OS
CRUD des départements + organigramme hiérarchique.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Permissions :
    GET    → IsManagerOrRH
    POST   → IsRHManager
    PUT    → IsRHManager
    DELETE → IsRHManager

Audit :
    POST   → CREATE_DEPARTEMENT
    PUT    → UPDATE_DEPARTEMENT
    DELETE → ARCHIVE_DEPARTEMENT
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Departement
from rh.permissions import IsManagerOrRH, IsRHManager
from rh.serializers import (
    DepartementCreateSerializer,
    DepartementSerializer,
    DepartementTreeSerializer,
)
from rh.services.audit import audit_departement


class DepartementsListView(APIView):
    """
    GET  /api/v1/hr/departements/ — Liste des départements
    POST /api/v1/hr/departements/ — Créer un département
    """

    def get_permissions(self):
        """
        Permissions différentes selon la méthode HTTP :
        GET  → IsManagerOrRH (lecture)
        POST → IsRHManager   (écriture)
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsManagerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    @extend_schema(
        summary="Lister les départements",
        description="Retourne la liste paginée " "des départements du tenant.",
        tags=["Departements"],
        responses={
            200: DepartementSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        departements = Departement.objects.filter(
            tenant_id=request.user.tenant_id,
        ).select_related("responsable", "parent")

        # Filtre is_active
        is_active = request.query_params.get("is_active")
        if is_active is not None:
            departements = departements.filter(is_active=is_active.lower() == "true")

        # Filtre parent_id
        parent_id = request.query_params.get("parent_id")
        if parent_id:
            departements = departements.filter(parent_id=parent_id)

        # Recherche full-text
        search = request.query_params.get("search")
        if search:
            from django.db.models import Q

            departements = departements.filter(
                Q(nom__icontains=search) | Q(code__icontains=search)
            )

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = departements.count()
        page_data = departements[start:end]

        serializer = DepartementSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/departements/" f"?page={page + 1}"
                    if end < total
                    else None
                ),
                "previous": (
                    f"/api/v1/hr/departements/" f"?page={page - 1}"
                    if page > 1
                    else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Créer un département",
        tags=["Departements"],
        request=DepartementCreateSerializer,
        responses={
            201: DepartementSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def post(self, request):
        serializer = DepartementCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        departement = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_departement("CREATE_DEPARTEMENT", departement, request)

        return Response(
            DepartementSerializer(departement).data, status=status.HTTP_201_CREATED
        )


class DepartementTreeView(APIView):
    """
    GET /api/v1/hr/departements/tree/
    Organigramme hiérarchique des départements.
    """

    permission_classes = [IsAuthenticated, IsManagerOrRH]

    @extend_schema(
        summary="Organigramme hiérarchique",
        description="Retourne l'arbre complet " "des départements.",
        tags=["Departements"],
        responses={
            200: DepartementTreeSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
        },
    )
    def get(self, request):
        # Uniquement les départements racines
        departements_racines = Departement.objects.filter(
            tenant_id=request.user.tenant_id, parent=None, is_active=True
        )
        serializer = DepartementTreeSerializer(
            departements_racines, many=True, context={"request": request}
        )
        return Response(serializer.data, status=status.HTTP_200_OK)


class DepartementDetailView(APIView):
    """
    GET    /api/v1/hr/departements/{id}/ — Détail
    PUT    /api/v1/hr/departements/{id}/ — Modifier
    DELETE /api/v1/hr/departements/{id}/ — Archiver
    """

    def get_permissions(self):
        """
        Permissions différentes selon la méthode HTTP :
        GET    → IsManagerOrRH
        PUT    → IsRHManager
        DELETE → IsRHManager
        """
        if self.request.method == "GET":
            return [IsAuthenticated(), IsManagerOrRH()]
        return [IsAuthenticated(), IsRHManager()]

    def get_object(self, pk, tenant_id):
        try:
            return Departement.objects.get(id=pk, tenant_id=tenant_id)
        except Departement.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un département",
        tags=["Departements"],
        responses={
            200: DepartementSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Département introuvable"},
        },
    )
    def get(self, request, pk):
        departement = self.get_object(pk, request.user.tenant_id)
        if not departement:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Département introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = DepartementSerializer(departement)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un département",
        tags=["Departements"],
        request=DepartementCreateSerializer,
        responses={
            200: DepartementSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Département introuvable"},
        },
    )
    def put(self, request, pk):
        departement = self.get_object(pk, request.user.tenant_id)
        if not departement:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Département introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = DepartementCreateSerializer(
            departement, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        departement = serializer.save()

        # ── Audit ─────────────────────────────────────────
        audit_departement("UPDATE_DEPARTEMENT", departement, request)

        return Response(
            DepartementSerializer(departement).data, status=status.HTTP_200_OK
        )

    @extend_schema(
        summary="Archiver un département",
        tags=["Departements"],
        responses={
            204: {"description": "Département archivé"},
            400: {"description": "Département contient " "des employés actifs"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Accès refusé"},
            404: {"description": "Département introuvable"},
        },
    )
    def delete(self, request, pk):
        departement = self.get_object(pk, request.user.tenant_id)
        if not departement:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Département introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier employés actifs
        if departement.nb_employes > 0:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": f"Impossible de supprimer — "
                        f"{departement.nb_employes} "
                        f"employé(s) actif(s) "
                        f"dans ce département.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Vérifier sous-départements actifs
        if departement.sous_departements.filter(is_active=True).exists():
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": "Impossible de supprimer — "
                        "des sous-départements "
                        "actifs existent.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # ── Audit avant archivage ──────────────────────────
        audit_departement("ARCHIVE_DEPARTEMENT", departement, request)

        # Soft delete
        departement.is_active = False
        departement.save(update_fields=["is_active", "updated_at"])

        return Response(status=status.HTTP_204_NO_CONTENT)
