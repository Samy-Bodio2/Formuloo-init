"""
Views Poste — Formuloo OS
CRUD des postes de l'organigramme.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-003 : versionnement /api/v1/
"""

from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rh.models import Poste
from rh.serializers import (
    PosteCreateSerializer,
    PosteSerializer,
)


class PostesListView(APIView):
    """
    GET  /api/v1/hr/postes/ — Liste des postes
    POST /api/v1/hr/postes/ — Créer un poste
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les postes",
        description="Retourne la liste paginée des postes du tenant.",
        tags=["Postes"],
        responses={
            200: PosteSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        postes = Poste.objects.filter(tenant_id=request.user.tenant_id).select_related(
            "departement"
        )

        # Filtre département
        departement_id = request.query_params.get("departement_id")
        if departement_id:
            postes = postes.filter(departement_id=departement_id)

        # Filtre niveau
        niveau = request.query_params.get("niveau")
        if niveau:
            postes = postes.filter(niveau=niveau)

        # Filtre is_active
        is_active = request.query_params.get("is_active")
        if is_active is not None:
            postes = postes.filter(is_active=is_active.lower() == "true")

        # Recherche full-text
        search = request.query_params.get("search")
        if search:
            postes = postes.filter(titre__icontains=search)

        # Pagination
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
        start = (page - 1) * page_size
        end = start + page_size
        total = postes.count()
        page_data = postes[start:end]

        serializer = PosteSerializer(page_data, many=True)
        return Response(
            {
                "count": total,
                "next": f"/api/v1/hr/postes/?page={page + 1}" if end < total else None,
                "previous": f"/api/v1/hr/postes/?page={page - 1}" if page > 1 else None,
                "results": serializer.data,
            },
            status=status.HTTP_200_OK,
        )

    @extend_schema(
        summary="Créer un poste",
        tags=["Postes"],
        request=PosteCreateSerializer,
        responses={
            201: PosteSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = PosteCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        poste = serializer.save()

        return Response(PosteSerializer(poste).data, status=status.HTTP_201_CREATED)


class PosteDetailView(APIView):
    """
    GET    /api/v1/hr/postes/{id}/ — Détail
    PUT    /api/v1/hr/postes/{id}/ — Modifier
    DELETE /api/v1/hr/postes/{id}/ — Supprimer
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant_id):
        try:
            return Poste.objects.get(id=pk, tenant_id=tenant_id)
        except Poste.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un poste",
        tags=["Postes"],
        responses={
            200: PosteSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Poste introuvable"},
        },
    )
    def get(self, request, pk):
        poste = self.get_object(pk, request.user.tenant_id)
        if not poste:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Poste introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = PosteSerializer(poste)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un poste",
        tags=["Postes"],
        request=PosteCreateSerializer,
        responses={
            200: PosteSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Poste introuvable"},
        },
    )
    def put(self, request, pk):
        poste = self.get_object(pk, request.user.tenant_id)
        if not poste:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Poste introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = PosteCreateSerializer(
            poste, data=request.data, partial=True, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        poste = serializer.save()

        return Response(PosteSerializer(poste).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Supprimer un poste",
        tags=["Postes"],
        responses={
            204: {"description": "Poste désactivé"},
            400: {"description": "Des employés actifs occupent ce poste"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Poste introuvable"},
        },
    )
    def delete(self, request, pk):
        poste = self.get_object(pk, request.user.tenant_id)
        if not poste:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Poste introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Vérifier qu'aucun employé actif n'occupe ce poste
        if poste.nb_employes > 0:
            return Response(
                {
                    "error": {
                        "code": "CONFLICT",
                        "message": f"Impossible de supprimer — "
                        f"{poste.nb_employes} employé(s) "
                        f"actif(s) occupent ce poste.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Soft delete
        poste.is_active = False
        poste.save(update_fields=["is_active", "updated_at"])

        return Response(status=status.HTTP_204_NO_CONTENT)
