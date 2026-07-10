"""
Serializers User — Formuloo OS
CRUD des utilisateurs.
Conforme ADR-002 : multi-tenant + RBAC
"""

from rest_framework import serializers
from authentification.models import User, Role, Organisation


class UserListSerializer(serializers.ModelSerializer):
    """
    Serializer pour la liste des utilisateurs.
    GET /api/v1/auth/utilisateurs/
    """

    roles = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            "id",
            "email",
            "first_name",
            "last_name",
            "avatar_url",
            "is_active",
            "is_verified",
            "roles",
            "created_at",
        ]

    def get_roles(self, obj):
        return list(obj.roles.values_list("code", flat=True))


class UserDetailSerializer(serializers.ModelSerializer):
    """
    Serializer pour le détail d'un utilisateur.
    GET /api/v1/auth/utilisateurs/{id}/
    """

    roles = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            "id",
            "email",
            "first_name",
            "last_name",
            "avatar_url",
            "is_active",
            "is_verified",
            "roles",
            "created_at",
            "updated_at",
        ]

    def get_roles(self, obj):
        return list(obj.roles.values_list("code", flat=True))


class UserCreateSerializer(serializers.ModelSerializer):
    """
    Serializer pour la création d'un utilisateur.
    POST /api/v1/auth/utilisateurs/

    - Si l'utilisateur connecté est ADMIN_PME :
      tenant extrait automatiquement du JWT
    - Si l'utilisateur connecté est SUPER_ADMIN :
      tenant_id doit être fourni dans le body
    """

    password = serializers.CharField(
        write_only=True, min_length=8, style={"input_type": "password"}
    )
    role = serializers.CharField(write_only=True, required=False)
    tenant_id = serializers.UUIDField(write_only=True, required=False)

    class Meta:
        model = User
        fields = [
            "email",
            "first_name",
            "last_name",
            "avatar_url",
            "password",
            "role",
            "tenant_id",
        ]

    def get_tenant(self):
        """
        Résout le tenant à partir du contexte :
        - ADMIN_PME → tenant depuis son JWT
        - SUPER_ADMIN → tenant_id depuis le body
        """
        request = self.context["request"]
        tenant = request.user.tenant

        if not tenant:
            tenant_id = request.data.get("tenant_id")
            if tenant_id:
                try:
                    tenant = Organisation.objects.get(id=tenant_id)
                except Organisation.DoesNotExist:
                    raise serializers.ValidationError("Organisation introuvable.")
        return tenant

    def validate_email(self, value):
        """
        Vérifie que l'email est unique dans le tenant.
        """
        tenant = self.get_tenant()
        if tenant and User.objects.filter(tenant=tenant, email=value).exists():
            raise serializers.ValidationError(
                "Cet email existe déjà dans cette organisation."
            )
        return value

    def create(self, validated_data):
        role_code = validated_data.pop("role", None)
        validated_data.pop("tenant_id", None)
        password = validated_data.pop("password")
        tenant = self.get_tenant()

        user = User.objects.create_user(
            tenant=tenant, password=password, **validated_data
        )

        # Assigner le rôle si fourni
        if role_code and tenant:
            try:
                role = Role.objects.get(code=role_code, tenant=tenant)
                user.roles.add(role)
            except Role.DoesNotExist:
                pass

        return user


class UserUpdateSerializer(serializers.ModelSerializer):
    """
    Serializer pour la modification d'un utilisateur.
    PUT /api/v1/auth/utilisateurs/{id}/
    """

    class Meta:
        model = User
        fields = ["first_name", "last_name", "avatar_url", "is_active"]
