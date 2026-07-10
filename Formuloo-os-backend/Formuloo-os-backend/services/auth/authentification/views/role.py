"""
Views Rôles et Permissions — Formuloo OS
CRUD des rôles RBAC + gestion des permissions.
Conforme ADR-002 : contrôle d'accès basé sur les rôles
"""

from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from drf_spectacular.utils import extend_schema

from authentification.models import Role, Permission, AuditLog
from authentification.serializers import (
    RoleListSerializer,
    RoleDetailSerializer,
    RoleCreateSerializer,
    PermissionSerializer,
)


class RolesListView(APIView):
    """
    GET  /api/v1/auth/roles/ — Liste des rôles
    POST /api/v1/auth/roles/ — Créer un rôle
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les rôles",
        description="Retourne la liste des rôles du tenant courant.",
        tags=["Rôles"],
        responses={
            200: RoleListSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        roles = Role.objects.filter(tenant=request.user.tenant).prefetch_related(
            "permissions"
        )

        serializer = RoleListSerializer(roles, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Créer un rôle",
        description="Crée un nouveau rôle dans le tenant courant.",
        tags=["Rôles"],
        request=RoleCreateSerializer,
        responses={
            201: RoleDetailSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def post(self, request):
        serializer = RoleCreateSerializer(
            data=request.data, context={"request": request}
        )
        serializer.is_valid(raise_exception=True)
        role = serializer.save()

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="CREATE_ROLE",
            resource="Role",
            resource_id=role.id,
            payload={"code": role.code},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(RoleDetailSerializer(role).data, status=status.HTTP_201_CREATED)


class RoleDetailView(APIView):
    """
    GET    /api/v1/auth/roles/{id}/ — Détail
    PUT    /api/v1/auth/roles/{id}/ — Modifier
    DELETE /api/v1/auth/roles/{id}/ — Supprimer
    """

    permission_classes = [IsAuthenticated]

    def get_object(self, pk, tenant):
        try:
            return Role.objects.get(id=pk, tenant=tenant)
        except Role.DoesNotExist:
            return None

    @extend_schema(
        summary="Détail d'un rôle",
        tags=["Rôles"],
        responses={
            200: RoleDetailSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Rôle introuvable"},
        },
    )
    def get(self, request, pk):
        role = self.get_object(pk, request.user.tenant)
        if not role:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Rôle introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = RoleDetailSerializer(role)
        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Modifier un rôle",
        tags=["Rôles"],
        responses={
            200: RoleDetailSerializer,
            400: {"description": "Données invalides"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Rôle système non modifiable"},
            404: {"description": "Rôle introuvable"},
        },
    )
    def put(self, request, pk):
        role = self.get_object(pk, request.user.tenant)
        if not role:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Rôle introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if role.is_system:
            return Response(
                {
                    "error": {
                        "code": "FORBIDDEN",
                        "message": "Les rôles système ne peuvent pas être modifiés.",
                    }
                },
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = RoleDetailSerializer(role, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="UPDATE_ROLE",
            resource="Role",
            resource_id=role.id,
            payload=request.data,
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        return Response(serializer.data, status=status.HTTP_200_OK)

    @extend_schema(
        summary="Supprimer un rôle",
        tags=["Rôles"],
        responses={
            204: {"description": "Rôle supprimé"},
            401: {"description": "Token JWT absent ou invalide"},
            403: {"description": "Impossible de supprimer un rôle système"},
            404: {"description": "Rôle introuvable"},
        },
    )
    def delete(self, request, pk):
        role = self.get_object(pk, request.user.tenant)
        if not role:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Rôle introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )

        if role.is_system:
            return Response(
                {
                    "error": {
                        "code": "FORBIDDEN",
                        "message": "Les rôles système ne peuvent pas être supprimés.",
                    }
                },
                status=status.HTTP_403_FORBIDDEN,
            )

        AuditLog.log(
            tenant=request.user.tenant,
            user=request.user,
            action="DELETE_ROLE",
            resource="Role",
            resource_id=role.id,
            payload={"code": role.code},
            ip_address=request.META.get("REMOTE_ADDR"),
        )

        role.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class PermissionsListView(APIView):
    """
    GET /api/v1/auth/permissions/
    Liste toutes les permissions disponibles.
    Lecture seule — immuable.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Lister les permissions",
        description="Retourne toutes les permissions disponibles. Lecture seule.",
        tags=["Permissions"],
        responses={
            200: PermissionSerializer(many=True),
            401: {"description": "Token JWT absent ou invalide"},
        },
    )
    def get(self, request):
        permissions = Permission.objects.all().order_by("module", "action", "resource")
        serializer = PermissionSerializer(permissions, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)


class PermissionDetailView(APIView):
    """
    GET /api/v1/auth/permissions/{id}/
    Détail d'une permission.
    Lecture seule — immuable.
    """

    permission_classes = [IsAuthenticated]

    @extend_schema(
        summary="Détail d'une permission",
        tags=["Permissions"],
        responses={
            200: PermissionSerializer,
            401: {"description": "Token JWT absent ou invalide"},
            404: {"description": "Permission introuvable"},
        },
    )
    def get(self, request, pk):
        try:
            permission = Permission.objects.get(id=pk)
        except Permission.DoesNotExist:
            return Response(
                {"error": {"code": "NOT_FOUND", "message": "Permission introuvable."}},
                status=status.HTTP_404_NOT_FOUND,
            )
        serializer = PermissionSerializer(permission)
        return Response(serializer.data, status=status.HTTP_200_OK)
