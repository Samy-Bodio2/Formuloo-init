"""
Views RefreshToken — Formuloo OS
Gestion des sessions actives (refresh tokens).
Conforme ADR-002 : authentification SSO + sécurité
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.exceptions import TokenError
from drf_spectacular.utils import extend_schema

from authentification.models import RefreshToken as RefreshTokenModel
from authentification.serializers import RefreshTokenSerializer


class RefreshTokensListView(APIView):
    """
    GET /api/v1/auth/refresh-tokens/
    Liste des sessions actives de l'utilisateur connecté.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les sessions actives",
        description="Retourne la liste des sessions actives (refresh tokens non blacklistés) de l'utilisateur connecté.",
        tags=["Auth"],
        responses={
            200: {"description": "Liste des sessions actives"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        refresh_tokens = RefreshTokenModel.objects.filter(
            user=request.user, is_blacklisted=False
        ).order_by("-created_at")

        serializer = RefreshTokenSerializer(refresh_tokens, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)


class RefreshTokenDetailView(APIView):
    """
    DELETE /api/v1/auth/refresh-tokens/{id}/
    Révoquer une session active.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Révoquer une session",
        description="Blackliste un refresh token — révoque une session active.",
        tags=["Auth"],
        responses={
            204: {"description": "Session révoquée"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Session introuvable"},
        },
    )
    def delete(self, request, pk):
        try:
            refresh_token = RefreshTokenModel.objects.get(
                id=pk, user=request.user, is_blacklisted=False
            )
        except RefreshTokenModel.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Session introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Blacklister via SimpleJWT
        try:
            token = RefreshToken(refresh_token.token)
            token.blacklist()
        except TokenError:
            pass

        # Blacklister dans notre modèle
        refresh_token.blacklist()

        return Response(status=status.HTTP_204_NO_CONTENT)
