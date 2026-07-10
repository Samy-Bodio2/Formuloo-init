"""
Views Utilisateurs — Formuloo OS
CRUD des utilisateurs + gestion des rôles.
Conforme ADR-001 : isolation multi-tenant
Conforme ADR-002 : RBAC
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.pagination import PageNumberPagination
from drf_spectacular.utils import extend_schema

from authentification.models import User, Role, AuditLog
from authentification.serializers import (
    UserListSerializer,
    UserDetailSerializer,
    UserCreateSerializer,
    UserUpdateSerializer,
)


class UtilisateursListView(APIView):
    """
    GET  /api/v1/auth/utilisateurs/ — Liste des utilisateurs
    POST /api/v1/auth/utilisateurs/ — Créer un utilisateur
    Filtré automatiquement par tenant.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les utilisateurs",
        description="Retourne la liste paginée des utilisateurs du tenant courant.",
        tags=["Utilisateurs"],
        responses={
            200: UserListSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        utilisateurs = User.objects.filter(tenant=request.user.tenant).order_by(
            "last_name", "first_name"
        )

        paginator = PageNumberPagination()
        paginator.page_size = 20
        page = paginator.paginate_queryset(utilisateurs, request)
        serializer = UserListSerializer(page, many=True)
        return paginator.get_paginated_response(serializer.data)

    @extend_schema(
        summary="Créer un utilisateur",
        description="Crée un nouvel utilisateur dans le tenant courant.",
        tags=["Utilisateurs"],
        request=UserCreateSerializer,
        responses={
            201: UserDetailSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = UserCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        user = serializer.save()

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="CREATE_USER",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(UserDetailSerializer(user).data, status=status.HTTP_201_CREATED)


class UtilisateurDetailView(APIView):
    """
    GET    /api/v1/auth/utilisateurs/{id}/ — Détail
    PUT    /api/v1/auth/utilisateurs/{id}/ — Modifier
    DELETE /api/v1/auth/utilisateurs/{id}/ — Supprimer (soft delete)
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant):
        try:
            return User.objects.get(id=pk, tenant=tenant)
        except User.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un utilisateur",
        tags=["Utilisateurs"],
        responses={
            200: UserDetailSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def get(self, request, pk):
        user = self.get_object(pk, request.user.tenant)
        if not user:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = UserDetailSerializer(user)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un utilisateur",
        tags=["Utilisateurs"],
        request=UserUpdateSerializer,
        responses={
            200: UserDetailSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def put(self, request, pk):
        user = self.get_object(pk, request.user.tenant)
        if not user:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        serializer = UserUpdateSerializer(user, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="UPDATE_USER",
            resource="User",
            resource_id=user.id,
            payload=request.data,
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(UserDetailSerializer(user).data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Supprimer un utilisateur",
        tags=["Utilisateurs"],
        responses={
            204: {"description": "Utilisateur désactivé"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def delete(self, request, pk):
        user = self.get_object(pk, request.user.tenant)
        if not user:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        # Soft delete
        user.is_active = False
        user.save(update_fields=["is_active"])

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="DELETE_USER",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(status=status.HTTP_204_NO_CONTENT)


class UtilisateurActiverView(APIView):
    """
    POST /api/v1/auth/utilisateurs/{id}/activer/
    Activer un compte utilisateur.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Activer un utilisateur",
        tags=["Utilisateurs"],
        responses={
            200: {"description": "Utilisateur activé"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            user = User.objects.get(id=pk, tenant=request.user.tenant)
        except User.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        user.is_active = True
        user.save(update_fields=["is_active"])

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="ACTIVATE_USER",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {"message": "Utilisateur activé avec succès."}, status=status.HTTP_200_OK
        )


class UtilisateurDesactiverView(APIView):
    """
    POST /api/v1/auth/utilisateurs/{id}/desactiver/
    Désactiver un compte utilisateur.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Désactiver un utilisateur",
        tags=["Utilisateurs"],
        responses={
            200: {"description": "Utilisateur désactivé"},
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def post(self, request, pk):
        try:
            user = User.objects.get(id=pk, tenant=request.user.tenant)
        except User.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        user.is_active = False
        user.save(update_fields=["is_active"])

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="DEACTIVATE_USER",
            resource="User",
            resource_id=user.id,
            payload={"email": user.email},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {"message": "Utilisateur désactivé avec succès."}, status=status.HTTP_200_OK
        )


class UtilisateurRolesView(APIView):
    """
    GET    /api/v1/auth/utilisateurs/{id}/roles/
    POST   /api/v1/auth/utilisateurs/{id}/roles/
    Gestion des rôles d'un utilisateur.
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant):
        try:
            return User.objects.get(id=pk, tenant=tenant)
        except User.DoesNotExist:
            return None

    @extend_schema(
        summary="Lister les rôles d'un utilisateur",
        tags=["Utilisateurs"],
        responses={
            200: {"description": "Liste des rôles"},
            404: {"description": "Utilisateur introuvable"},
        },
    )
    def get(self, request, pk):
        user = self.get_object(pk, request.user.tenant)
        if not user:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        roles = user.roles.values("id", "name", "code")
        return Response(list(roles), status=status.HTTP_200_OK)

    @extend_schema(
        summary="Assigner un rôle à un utilisateur",
        tags=["Utilisateurs"],
        responses={
            200: {"description": "Rôle assigné"},
            404: {"description": "Utilisateur ou rôle introuvable"},
        },
    )
    def post(self, request, pk):
        user = self.get_object(pk, request.user.tenant)
        if not user:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Utilisateur introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        role_code = request.data.get("role")
        if not role_code:
            return Response(
                {
                    "error": {
                        "code": "VALIDATION_ERROR",
                        "message": "Le code du rôle est obligatoire.",
                    }
                },
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            role = Role.objects.get(code=role_code, tenant=request.user.tenant)
        except Role.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Rôle introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        user.roles.add(role)

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="ASSIGN_ROLE",
            resource="User",
            resource_id=user.id,
            payload={"role": role_code},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(
            {"message": f"Rôle {role_code} assigné avec succès."},
            status=status.HTTP_200_OK,
        )
