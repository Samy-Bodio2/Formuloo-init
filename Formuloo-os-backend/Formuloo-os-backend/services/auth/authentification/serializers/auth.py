"""
Serializers d'authentification — Formuloo OS
Login, logout, refresh, me
Conforme ADR-002 : authentification SSO + JWT
"""

from rest_framework import serializers
from django.contrib.auth import authenticate
from authentification.models import User


class LoginSerializer(serializers.Serializer):
    """
    Serializer pour le login email/password.
    Valide les credentials et retourne l'utilisateur.
    """

    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, style={"input_type": "password"})

    def validate(self, data):
        email = data.get("email")
        password = data.get("password")

        if not email or not password:
            raise serializers.ValidationError("Email et mot de passe obligatoires.")

        # Authentifier l'utilisateur
        user = authenticate(
            request=self.context.get("request"), username=email, password=password
        )

        if not user:
            raise serializers.ValidationError("Email ou mot de passe incorrect.")

        if not user.is_active:
            raise serializers.ValidationError("Ce compte est désactivé.")

        data["user"] = user
        return data


class LogoutSerializer(serializers.Serializer):
    """
    Serializer pour le logout.
    Blackliste le refresh token.
    """

    refresh = serializers.CharField()


class MeSerializer(serializers.ModelSerializer):
    """
    Serializer pour le profil utilisateur courant.
    GET /api/v1/auth/me/
    PATCH /api/v1/auth/me/
    """

    roles = serializers.SerializerMethodField()
    tenant_id = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = [
            "id",
            "email",
            "first_name",
            "last_name",
            "tenant_id",
            "avatar_url",
            "is_active",
            "is_verified",
            "roles",
            "created_at",
            "updated_at",
        ]
        read_only_fields = [
            "id",
            "email",
            "tenant_id",
            "is_active",
            "is_verified",
            "created_at",
            "updated_at",
        ]

    def get_roles(self, obj):
        return list(obj.roles.values_list("code", flat=True))

    def get_tenant_id(self, obj):
        return str(obj.tenant.id) if obj.tenant else None


class ChangerMotDePasseSerializer(serializers.Serializer):
    """
    Serializer pour le changement de mot de passe.
    POST /api/v1/auth/me/changer-mot-de-passe/
    """

    ancien_mot_de_passe = serializers.CharField(
        write_only=True, style={"input_type": "password"}
    )
    nouveau_mot_de_passe = serializers.CharField(
        write_only=True, style={"input_type": "password"}, min_length=8
    )

    def validate_ancien_mot_de_passe(self, value):
        user = self.context["request"].user
        if not user.check_password(value):
            raise serializers.ValidationError("Ancien mot de passe incorrect.")
        return value
