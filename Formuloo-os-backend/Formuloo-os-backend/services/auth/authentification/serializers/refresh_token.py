"""
Serializers RefreshToken — Formuloo OS
Sessions actives (refresh tokens).
Conforme ADR-002 : authentification SSO + sécurité
"""

from rest_framework import serializers
from authentification.models import RefreshToken


class RefreshTokenSerializer(serializers.ModelSerializer):
    """
    Serializer pour la liste des sessions actives.
    Le token brut n'est jamais retourné.
    """

    is_valid = serializers.SerializerMethodField()

    class Meta:
        model = RefreshToken
        fields = [
            "id",
            "is_blacklisted",
            "is_valid",
            "created_at",
            "expires_at",
            "blacklisted_at",
        ]
        read_only_fields = fields

    def get_is_valid(self, obj):
        return obj.is_valid
