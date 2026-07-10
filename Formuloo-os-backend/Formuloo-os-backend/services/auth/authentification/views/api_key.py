"""
Views Clés API — Formuloo OS
Gestion des clés API pour les intégrations tierces.
Conforme ADR-002 : sécurité + multi-tenant
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from drf_spectacular.utils import extend_schema

from authentification.models import APIKey, AuditLog
from authentification.serializers import (
    APIKeySerializer,
    APIKeyCreateSerializer,
)


class APIKeysListView(APIView):
    """
    GET  /api/v1/auth/api-keys/ — Liste des clés API
    POST /api/v1/auth/api-keys/ — Créer une clé API
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les clés API",
        description="Retourne la liste des clés API du tenant courant. La clé brute n'est jamais retournée.",
        tags=["API Keys"],
        responses={
            200: {"description": "Liste des clés API"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        api_keys = APIKey.objects.filter(tenant=request.user.tenant).order_by(
            "-created_at"
        )

        serializer = APIKeySerializer(api_keys, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Créer une clé API",
        description="Génère une nouvelle clé API. La clé brute est retournée UNE SEULE FOIS.",
        tags=["API Keys"],
        responses={
            201: {"description": "Clé API créée"},
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = APIKeyCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)

        # Générer la clé brute et son hash
        raw_key, key_hash = APIKey.generate_key()

        api_key = APIKey.objects.create(
            tenant=request.user.tenant,
            owner=request.user,
            name=request.data.get("name"),
            key_hash=key_hash,
            scopes=request.data.get("scopes", []),
            rate_limit=request.data.get("rate_limit", 100),
            expires_at=request.data.get("expires_at", None),
        )

        # Logger la création
        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="CREATE_APIKEY",
            resource="APIKey",
            resource_id=api_key.id,
            payload={"name": api_key.name},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {
                "id": str(api_key.id),
                "name": api_key.name,
                "key": raw_key,
                "scopes": api_key.scopes,
                "rate_limit": api_key.rate_limit,
                "created_at": api_key.created_at,
                "message": "Conservez cette clé — elle ne sera plus affichée.",
            },
            status=status.HTTP_201_CREATED,
        )


class APIKeyDetailView(APIView):
    """
    GET    /api/v1/auth/api-keys/{id}/ — Détail
    DELETE /api/v1/auth/api-keys/{id}/ — Révoquer
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant):
        try:
            return APIKey.objects.get(id=pk, tenant=tenant)
        except APIKey.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'une clé API",
        tags=["API Keys"],
        responses={
            200: {"description": "Clé API trouvée"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Clé API introuvable"},
        },
    )
    def get(self, request, pk):
        api_key = self.get_object(pk, request.user.tenant)
        if not api_key:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Clé API introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = APIKeySerializer(api_key)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Révoquer une clé API",
        description="Désactive une clé API — elle ne pourra plus être utilisée.",
        tags=["API Keys"],
        responses={
            204: {"description": "Clé API révoquée"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Clé API introuvable"},
        },
    )
    def delete(self, request, pk):
        api_key = self.get_object(pk, request.user.tenant)
        if not api_key:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Clé API introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        api_key.is_active = False
        api_key.save(update_fields=["is_active"])

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="REVOKE_APIKEY",
            resource="APIKey",
            resource_id=api_key.id,
            payload={"name": api_key.name},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(status=status.HTTP_204_NO_CONTENT)
