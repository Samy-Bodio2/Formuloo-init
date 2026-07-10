"""
Serializers Role — Formuloo OS
CRUD des rôles RBAC.
Conforme ADR-002 : contrôle d'accès basé sur les rôles
"""

from rest_framework import serializers
from authentification.models import Role, Permission


class PermissionSerializer(serializers.ModelSerializer):
    """
    Serializer pour les permissions.
    GET /api/v1/auth/permissions/
    """

    class Meta:
        model = Permission
        fields = ["id", "module", "action", "resource", "code"]
        read_only_fields = ["id", "code"]


class RoleListSerializer(serializers.ModelSerializer):
    """
    Serializer pour la liste des rôles.
    GET /api/v1/auth/roles/
    """

    permissions = serializers.SerializerMethodField()

    class Meta:
        model = Role
        fields = ["id", "name", "code", "is_system", "permissions", "created_at"]

    def get_permissions(self, obj):
        return list(obj.permissions.values_list("code", flat=True))


class RoleDetailSerializer(serializers.ModelSerializer):
    """
    Serializer pour le détail d'un rôle.
    GET /api/v1/auth/roles/{id}/
    """

    permissions = PermissionSerializer(many=True, read_only=True)

    class Meta:
        model = Role
        fields = [
            "id",
            "name",
            "code",
            "is_system",
            "permissions",
            "created_at",
            "updated_at",
        ]


class RoleCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour la création d'un rôle.
    POST /api/v1/auth/roles/
    """

    permissions = serializers.ListField(child=serializers.CharField(), required=False)

    class Meta:
        model = Role
        fields = ["name", "code", "permissions"]

    def validate_code(self, value):
        """
        Vérifie que le code est unique dans le tenant.
        """
        tenant = self.context["request"].user.tenant
        if Role.objects.filter(tenant=tenant, code=value).exists():
            raise serializers.ValidationError(
                "Ce code de rôle existe déjà dans votre organisation."
            )
        return value

    def create(self, validated_data):
        permission_codes = validated_data.pop("permissions", [])
        tenant = self.context["request"].user.tenant

        role = Role.objects.create(tenant=tenant, **validated_data)

        # Assigner les permissions
        if permission_codes:
            permissions = Permission.objects.filter(code__in=permission_codes)
            role.permissions.set(permissions)

        return role
