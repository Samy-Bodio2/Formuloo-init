"""
Serializers APIKey — Formuloo OS
Clés API pour les intégrations tierces.
Conforme ADR-002 : sécurité + multi-tenant
"""

from rest_framework import serializers
from authentification.models import APIKey


class APIKeySerializer(serializers.ModelSerializer):
    """
    Serializer pour la liste et le détail des clés API.
    La clé brute n'est jamais retournée.
    """

    owner = serializers.SerializerMethodField()

    class Meta:
        model = APIKey
        fields = [
            "id",
            "name",
            "owner",
            "scopes",
            "rate_limit",
            "is_active",
            "last_used",
            "expires_at",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "owner", "created_at", "updated_at"]

    def get_owner(self, obj):
        return {
            "id": str(obj.owner.id),
            "email": obj.owner.email,
            "nom": obj.owner.get_full_name(),
        }


class APIKeyCreateSerializer(serializers.Serializer):
    """
    Serializer pour la création d'une clé API.
    POST /api/v1/auth/api-keys/
    """

    name = serializers.CharField(max_length=100)
    scopes = serializers.ListField(
        child=serializers.CharField(), required=False, default=list
    )
    rate_limit = serializers.IntegerField(required=False, default=100, min_value=1)
    expires_at = serializers.DateTimeField(
        required=False, allow_null=True, default=None
    )

    def validate_name(self, value):
        """
        Vérifie que le nom est unique dans le tenant.
        """
        tenant = self.context["request"].user.tenant
        if APIKey.objects.filter(tenant=tenant, name=value, is_active=True).exists():
            raise serializers.ValidationError(
                "Une clé API active avec ce nom existe déjà."
            )
        return value
