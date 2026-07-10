"""
Views Présence — Formuloo OS
Gestion des pointages et présences.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/

Soft Delete :
→ DELETE archive la présence (is_active=False)
→ On ne supprime JAMAIS une présence
→ Historique conservé pour audit et litiges
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Presence
from rh.serializers import (
    PresenceCreateSerializer,
    PresenceSerializer,
)


class PresencesListView(APIView):
    """
    GET  /api/v1/hr/presences/ — Liste des présences
    POST /api/v1/hr/presences/ — Enregistrer une présence
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les présences",
        description="Retourne la liste paginée " "des pointages actifs du tenant.",
        tags=["Presences"],
        responses={
            200: PresenceSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        # Filtrer uniquement les présences actives
        # is_active=False → présences archivées
        # non visibles dans la liste normale
        presences = Presence.objects.filter(
            tenant_id=request.user.tenant_id, is_active=True  # ← soft delete
        ).select_related("employee")

        # Filtre employé
        employe_id = request.query_params.get("employe_id")
        if employe_id:
            presences = presences.filter(employee_id=employe_id)

        # Filtre date exacte
        date = request.query_params.get("date")
        if date:
            presences = presences.filter(date=date)

        # Filtre statut
        statut = request.query_params.get("statut")
        if statut:
            presences = presences.filter(statut=statut)

        # Filtre période
        date_debut = request.query_params.get("date_debut")
        if date_debut:
            presences = presences.filter(date__gte=date_debut)

        date_fin = request.query_params.get("date_fin")
        if date_fin:
            presences = presences.filter(date__lte=date_fin)

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = presences.count()
        page_data = presences[start:end]

        serializer = PresenceSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": (
                    f"/api/v1/hr/presences/" f"?page={page + 1}"
                    if end < total
                    else None
                ),
                "previous": (
                    f"/api/v1/hr/presences/" f"?page={page - 1}" if page > 1 else None
                ),
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Enregistrer une présence",
        tags=["Presences"],
        request=PresenceCreateSerializer,
        responses={
            201: PresenceSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = PresenceCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        presence = serializer.save()

        return Response(
            PresenceSerializer(presence).data, status=status.HTTP_201_CREATED
        )


class PresenceDetailView(APIView):
    """
    GET    /api/v1/hr/presences/{id}/ — Détail
    PUT    /api/v1/hr/presences/{id}/ — Modifier
    DELETE /api/v1/hr/presences/{id}/ — Archiver (soft delete)
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant_id):
        """
        Récupère une présence active par son UUID.
        Retourne None si introuvable ou archivée.
        """
        try:
            return Presence.objects.get(
                id=pk, tenant_id=tenant_id, is_active=True  # ← soft delete
            )
        except Presence.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'une présence",
        tags=["Presences"],
        responses={
            200: PresenceSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Présence introuvable"},
        },
    )
    def get(self, request, pk):
        presence = self.get_object(pk, request.user.tenant_id)
        if not presence:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Présence introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = PresenceSerializer(presence)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier une présence",
        tags=["Presences"],
        request=PresenceCreateSerializer,
        responses={
            200: PresenceSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Présence introuvable"},
        },
    )
    def put(self, request, pk):
        presence = self.get_object(pk, request.user.tenant_id)
        if not presence:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Présence introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = PresenceCreateSerializer(
            presence, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        presence = serializer.save()

        return Response(PresenceSerializer(presence).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Archiver une présence",
        description="Archive la présence (soft delete). "
        "L'historique est conservé "
        "pour audit et litiges.",
        tags=["Presences"],
        responses={
            204: {"description": "Présence archivée"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Présence introuvable"},
        },
    )
    def delete(self, request, pk):
        """
        Archive la présence au lieu de la supprimer.

        Soft delete → is_active=False
        → Historique conservé ✅
        → Audit trail possible ✅
        → Restauration possible ✅
        → Litiges couverts ✅
        """
        presence = self.get_object(pk, request.user.tenant_id)
        if not presence:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Présence introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Soft delete — archiver au lieu de supprimer
        # Utilise la méthode archiver() du modèle
        # qui met is_active=False
        presence.archiver()

        return Response(status=status.HTTP_204_NO_CONTENT)
