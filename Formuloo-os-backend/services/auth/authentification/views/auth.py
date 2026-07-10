"""
Views d'authentification — Formuloo OS
Login, logout, refresh, me, changer-mot-de-passe
Conforme ADR-002 : authentification SSO + JWT
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.exceptions import TokenError
from drf_spectacular.utils import extend_schema

from authentification.models import AuditLog, Permission, RefreshToken as RefreshTokenModel
from authentification.serializers import (
    LoginSerializer,
    LogoutSerializer,
    MeSerializer,
    ChangerMotDePasseSerializer,
)


class LoginView(APIView):
    """
    POST /api/v1/auth/login/
    Authentification email/password → JWT
    """

    permission_classes = [AllowAny]

    @extend_schema(
        summary="Connexion utilisateur",
        description="Authentification par email et mot de passe. Retourne un access token et un refresh token.",
        tags=["Authentification"],
        request=LoginSerializer,
        responses={
            200: {"description": "Connexion réussie — tokens JWT retournés"},
            400: {"description": "Credentials invalides"},
        },
    )
    def post(self, request):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)

        user = serializer.validated_data["user"]

        # Générer les tokens JWT
        refresh = RefreshToken.for_user(user)
        access = refresh.access_token

        # Ajouter le tenant_id dans le token
        if user.tenant:
            refresh["tenant_id"] = str(user.tenant.id)
            access["tenant_id"] = str(user.tenant.id)

        # Ajouter les rôles dans le token
        roles = list(user.roles.values_list("code", flat=True))
        refresh["roles"] = roles
        access["roles"] = roles

        # Ajouter les permissions agrégées — utilisées par tous les services
        # pour le contrôle d'accès sans appel réseau supplémentaire
        permissions = list(
            Permission.objects.filter(roles__in=user.roles.all())
            .values_list("code", flat=True)
            .distinct()
        )
        refresh["permissions"] = permissions
        access["permissions"] = permissions

        # Ajouter auth_user_id — utilisé par le service RH
        # pour l'isolation des données (IsOwnerOrRH)
        refresh["auth_user_id"] = str(user.id)
        access["auth_user_id"] = str(user.id)

        # Stocker le refresh token
        RefreshTokenModel.objects.create(
            user=user,
            token=str(refresh),
            expires_at=refresh.current_time + refresh.lifetime,
        )

        # Logger la connexion
        AuditLog.log(
            tenant=user.tenant,
            user=user,
            action="LOGIN",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {
                "access": str(access),
                "refresh": str(refresh),
                "user": {
                    "id": str(user.id),
                    "email": user.email,
                    "first_name": user.first_name,
                    "last_name": user.last_name,
                    "roles": roles,
                    "permissions": permissions,
                    "tenant_id": str(user.tenant.id) if user.tenant else None,
                    "is_active": user.is_active,
                    "is_verified": user.is_verified,
                },
            },
            status=status.HTTP_200_OK,
        )


class LogoutView(APIView):
    """
    POST /api/v1/auth/logout/
    Blackliste le refresh token — déconnexion
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Déconnexion utilisateur",
        description="Blackliste le refresh token. L'access token expire naturellement.",
        tags=["Authentification"],
        request=LogoutSerializer,
        responses={
            204: {"description": "Déconnexion réussie"},
            400: {"description": "Token invalide"},
        },
    )
    def post(self, request):
        serializer = LogoutSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            # Blacklister le token SimpleJWT
            token = RefreshToken(serializer.validated_data["refresh"])
            token.blacklist()

            # Blacklister dans notre modèle
            refresh_token = RefreshTokenModel.objects.filter(
                token=serializer.validated_data["refresh"], user=request.user
            ).first()

            if refresh_token:
                refresh_token.blacklist()

            # Logger la déconnexion
            AuditLog.log(
                tenant=request.user.tenant,
                user=request.user,
                action="LOGOUT",
                resource="User",
                resource_id=request.user.id,
                payload={},
                ip_address=request.META.get("REMOTE_ADDR"),
            )

            return Response(status=status.HTTP_204_NO_CONTENT)

        except TokenError:
            return Response(
                {
                    "error": {
                        "code": "TOKEN_INVALID",
                        "message": "Token invalide ou expiré.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )


class MeView(APIView):
    """
    GET   /api/v1/auth/me/ — Profil utilisateur courant
    PATCH /api/v1/auth/me/ — Modifier le profil
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Profil utilisateur courant",
        description="Retourne le profil de l'utilisateur connecté.",
        tags=["Authentification"],
        responses={
            200: MeSerializer,
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        serializer = MeSerializer(request.user)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier le profil",
        description="Modifie le profil de l'utilisateur connecté.",
        tags=["Authentification"],
        request=MeSerializer,
        responses={
            200: MeSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def patch(self, request):
        serializer = MeSerializer(request.user, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()

        # Logger la modification
        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="UPDATE_PROFILE",
            resource="User",
            resource_id=request.user.id,
            payload=request.data,
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(serializer.data, status=status.HTTP_200_OK)


class ChangerMotDePasseView(APIView):
    """
    POST /api/v1/auth/me/changer-mot-de-passe/
    Changer le mot de passe de l'utilisateur connecté
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Changer le mot de passe",
        description="Change le mot de passe de l'utilisateur connecté.",
        tags=["Authentification"],
        request=ChangerMotDePasseSerializer,
        responses={
            204: {"description": "Mot de passe modifié"},
            400: {"description": "Ancien mot de passe incorrect"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = ChangerMotDePasseSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)

        request.user.set_password(serializer.validated_data["nouveau_mot_de_passe"])
        request.user.save()

        # Logger le changement
        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="CHANGE_PASSWORD",
            resource="User",
            resource_id=request.user.id,
            payload={},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(status=status.HTTP_204_NO_CONTENT)
